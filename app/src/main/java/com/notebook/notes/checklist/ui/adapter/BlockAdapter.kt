package com.notebook.notes.checklist.ui.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemBlockCheckboxBinding
import com.notebook.notes.checklist.databinding.ItemBlockDrawingBinding
import com.notebook.notes.checklist.databinding.ItemBlockFileBinding
import com.notebook.notes.checklist.databinding.ItemBlockImageBinding
import com.notebook.notes.checklist.databinding.ItemBlockLinkBinding
import com.notebook.notes.checklist.databinding.ItemBlockTextBinding
import com.notebook.notes.checklist.databinding.ItemBlockVoiceBinding
import com.notebook.notes.checklist.model.AttachmentMetadata
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.NoteBlockExtra
import com.notebook.notes.checklist.attachment.FileTypeIconResolver
import com.notebook.notes.checklist.model.VoiceNoteMetadata
import com.notebook.notes.checklist.model.formatAttachmentMeta
import com.notebook.notes.checklist.ui.audio.VoiceWaveformHelper
import com.notebook.notes.checklist.ui.note.NoteItemOptionsConfig
import com.notebook.notes.checklist.ui.note.NoteItemOptionsListener
import com.notebook.notes.checklist.ui.note.NoteItemOptionsPopup
import com.notebook.notes.checklist.ui.list.EmptyListItemDeleteHelper
import com.notebook.notes.checklist.ui.richtext.RichTextEditorController
import com.notebook.notes.checklist.ui.richtext.RichTextEditorHost
import com.notebook.notes.checklist.utils.JsonUtils
import java.io.File

class BlockAdapter(
    private val richTextHost: RichTextEditorHost,
    private val onBlockChanged: (Long, String, Boolean?) -> Unit,
    private val onBlockRemoved: (Long) -> Unit
) : ListAdapter<NoteBlock, RecyclerView.ViewHolder>(BlockDiffCallback()) {

    private val textControllers = mutableMapOf<Long, RichTextEditorController>()
    private var pendingFocusBlockId: Long? = null

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    fun flushAllTextBlocks() {
        textControllers.values.forEach { it.flushContent() }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position).type) {
        BlockType.TEXT -> VIEW_TEXT
        BlockType.CHECKBOX -> VIEW_CHECKBOX
        BlockType.IMAGE -> VIEW_IMAGE
        BlockType.FILE -> VIEW_FILE
        BlockType.LINK -> VIEW_LINK
        BlockType.DRAWING -> VIEW_DRAWING
        BlockType.VOICE -> VIEW_VOICE
        else -> VIEW_TEXT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_CHECKBOX -> CheckboxViewHolder(
                ItemBlockCheckboxBinding.inflate(inflater, parent, false)
            )
            VIEW_IMAGE -> ImageViewHolder(
                ItemBlockImageBinding.inflate(inflater, parent, false)
            )
            VIEW_FILE -> FileViewHolder(
                ItemBlockFileBinding.inflate(inflater, parent, false)
            )
            VIEW_LINK -> LinkViewHolder(
                ItemBlockLinkBinding.inflate(inflater, parent, false)
            )
            VIEW_DRAWING -> DrawingViewHolder(
                ItemBlockDrawingBinding.inflate(inflater, parent, false)
            )
            VIEW_VOICE -> VoiceViewHolder(
                ItemBlockVoiceBinding.inflate(inflater, parent, false)
            )
            else -> TextViewHolder(
                ItemBlockTextBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val block = getItem(position)
        when (holder) {
            is TextViewHolder -> holder.bind(block)
            is CheckboxViewHolder -> holder.bind(block)
            is ImageViewHolder -> holder.bind(block)
            is FileViewHolder -> holder.bind(block)
            is LinkViewHolder -> holder.bind(block)
            is DrawingViewHolder -> holder.bind(block)
            is VoiceViewHolder -> holder.bind(block)
        }
    }

    fun setPendingFocusBlockId(blockId: Long) {
        pendingFocusBlockId = blockId
    }

    fun focusTextBlock(
        recyclerView: RecyclerView,
        blockId: Long,
        onFocused: (() -> Unit)? = null
    ) {
        pendingFocusBlockId = blockId
        val index = currentList.indexOfFirst { it.id == blockId }
        if (index < 0) {
            onFocused?.invoke()
            return
        }
        (recyclerView.layoutManager as? LinearLayoutManager)
            ?.scrollToPositionWithOffset(index, 0)
            ?: recyclerView.scrollToPosition(index)
        recyclerView.post {
            tryFocusTextBlock(recyclerView, blockId, 0, onFocused)
        }
    }

    private fun tryFocusTextBlock(
        recyclerView: RecyclerView,
        blockId: Long,
        attempt: Int,
        onFocused: (() -> Unit)? = null
    ) {
        val index = currentList.indexOfFirst { it.id == blockId }
        if (index < 0) {
            pendingFocusBlockId = null
            onFocused?.invoke()
            return
        }
        val holder = recyclerView.findViewHolderForAdapterPosition(index) as? TextViewHolder
        if (holder != null) {
            holder.requestEditorFocus(onFocused)
        } else if (attempt < 15) {
            recyclerView.post { tryFocusTextBlock(recyclerView, blockId, attempt + 1, onFocused) }
        } else {
            pendingFocusBlockId = null
            onFocused?.invoke()
        }
    }

    inner class TextViewHolder(
        private val binding: ItemBlockTextBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundBlockId: Long? = null

        fun bind(block: NoteBlock) {
            val editText = binding.richTextEditor
            editText.hint = editText.context.getString(
                if (block.extraData == NoteBlockExtra.NOTES_HERE) {
                    R.string.notes_here
                } else {
                    R.string.add_text
                }
            )
            val existing = textControllers[block.id]
            if (existing == null || boundBlockId != block.id) {
                boundBlockId = block.id
                RichTextEditorController(editText).also { ctrl ->
                    textControllers[block.id] = ctrl
                    editText.controller = ctrl
                    ctrl.loadDocument(JsonUtils.jsonToRichText(block.content))
                    editText.onFocusChangedListener = { _, focused ->
                        if (focused) {
                            richTextHost.onRichTextEditorFocused(ctrl)
                        } else {
                            richTextHost.onRichTextEditorBlurred()
                        }
                    }
                    ctrl.onContentChanged = { document ->
                        onBlockChanged(block.id, JsonUtils.richTextToJson(document), null)
                    }
                }
            }
            // Default "Notes Here" placeholder must stay — backspace on empty must not remove it.
            if (block.extraData == NoteBlockExtra.NOTES_HERE) {
                EmptyListItemDeleteHelper.detach(editText)
            } else {
                EmptyListItemDeleteHelper.attach(
                    editText = editText,
                    isEmpty = { editText.text.isNullOrBlank() },
                    onDeleteWhenEmpty = {
                        textControllers.remove(block.id)
                        onBlockRemoved(block.id)
                    }
                )
            }
            if (block.id == pendingFocusBlockId) {
                scheduleEditorFocus(editText)
            }
        }

        private fun scheduleEditorFocus(
            editText: com.notebook.notes.checklist.ui.richtext.RichTextEditText,
            onFocused: (() -> Unit)? = null
        ) {
            fun applyFocus() {
                editText.isCursorVisible = true
                editText.isFocusableInTouchMode = true
                editText.isFocusable = true
                if (!editText.requestFocus()) {
                    editText.requestFocusFromTouch()
                }
                val cursor = editText.text?.length ?: 0
                editText.setSelection(cursor)
                editText.controller?.let { richTextHost.onRichTextEditorFocused(it) }
                val imm = editText.context.getSystemService(InputMethodManager::class.java)
                imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                editText.postDelayed({
                    if (!editText.hasFocus()) {
                        editText.requestFocus()
                    }
                    imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                }, 100L)
                pendingFocusBlockId = null
                onFocused?.invoke()
            }

            if (editText.isLaidOut) {
                editText.post { applyFocus() }
            } else {
                editText.viewTreeObserver.addOnGlobalLayoutListener(
                    object : ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (!editText.isLaidOut) return
                            editText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            editText.post { applyFocus() }
                        }
                    }
                )
            }
        }

        fun requestEditorFocus(onFocused: (() -> Unit)? = null) {
            scheduleEditorFocus(binding.richTextEditor, onFocused)
        }
    }

    inner class CheckboxViewHolder(
        private val binding: ItemBlockCheckboxBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            binding.cbItem.setOnCheckedChangeListener(null)
            binding.cbItem.isChecked = block.checked
            binding.etText.setText(block.content)
            binding.cbItem.setOnCheckedChangeListener { _, isChecked ->
                onBlockChanged(block.id, binding.etText.text.toString(), isChecked)
            }
            binding.etText.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    onBlockChanged(block.id, binding.etText.text.toString(), binding.cbItem.isChecked)
                }
            }
            binding.btnRemove.setOnClickListener { onBlockRemoved(block.id) }
        }
    }

    inner class ImageViewHolder(
        private val binding: ItemBlockImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            binding.ivImage.load(File(block.content)) {
                crossfade(true)
            }
            binding.icMoreVert.setOnClickListener { onBlockRemoved(block.id) }
        }
    }

    inner class FileViewHolder(
        private val binding: ItemBlockFileBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            val metadata = AttachmentMetadata.fromBlock(block)
            val fileName = metadata?.fileName ?: File(block.content).name
            val fileSize = metadata?.fileSize ?: File(block.content).length()
            val createdAt = metadata?.createdAt ?: File(block.content).lastModified()
            val mimeType = metadata?.mimeType.orEmpty()

            binding.ivFileIcon.setImageResource(FileTypeIconResolver.iconFor(fileName, mimeType))
            binding.tvFileName.text = metadata?.displayLabel() ?: fileName
            binding.tvFileMeta.text = formatAttachmentMeta(createdAt, fileSize)
            binding.ivMore.setOnClickListener { anchor ->
                NoteItemOptionsPopup.show(
                    anchor = anchor,
                    config = NoteItemOptionsConfig(
                        showView = false,
                        showShare = false,
                        showDelete = true
                    ),
                    listener = NoteItemOptionsListener(
                        onDelete = { onBlockRemoved(block.id) }
                    )
                )
            }
        }
    }

    inner class LinkViewHolder(
        private val binding: ItemBlockLinkBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            binding.tvLink.text = block.content
            binding.tvLink.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(block.content))
                    binding.root.context.startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            binding.ivMore.setOnClickListener { anchor ->
                NoteItemOptionsPopup.show(
                    anchor = anchor,
                    config = NoteItemOptionsConfig(
                        showView = false,
                        showShare = true,
                        showDelete = true
                    ),
                    listener = NoteItemOptionsListener(
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, block.content)
                            }
                            anchor.context.startActivity(
                                Intent.createChooser(
                                    shareIntent,
                                    anchor.context.getString(R.string.share)
                                )
                            )
                        },
                        onDelete = { onBlockRemoved(block.id) }
                    )
                )
            }
        }
    }

    inner class DrawingViewHolder(
        private val binding: ItemBlockDrawingBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            binding.ivDrawing.load(File(block.content)) {
                crossfade(true)
            }
            binding.btnRemove.setOnClickListener { onBlockRemoved(block.id) }
        }
    }

    inner class VoiceViewHolder(
        private val binding: ItemBlockVoiceBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            val metadata = VoiceNoteMetadata.fromJson(block.extraData)
            val waveform = metadata.waveform.ifEmpty {
                VoiceWaveformHelper.fallbackWaveform(block.content).toList()
            }
            binding.waveformView.setWaveform(waveform)
            binding.btnPlayPause.isEnabled = false
            binding.ivMore.setOnClickListener { anchor ->
                NoteItemOptionsPopup.show(
                    anchor = anchor,
                    config = NoteItemOptionsConfig(
                        showView = false,
                        showShare = false,
                        showDelete = true
                    ),
                    listener = NoteItemOptionsListener(
                        onDelete = { onBlockRemoved(block.id) }
                    )
                )
            }
        }
    }

    private class BlockDiffCallback : DiffUtil.ItemCallback<NoteBlock>() {
        override fun areItemsTheSame(oldItem: NoteBlock, newItem: NoteBlock) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NoteBlock, newItem: NoteBlock) =
            oldItem == newItem
    }

    companion object {
        private const val VIEW_TEXT = 0
        private const val VIEW_CHECKBOX = 1
        private const val VIEW_IMAGE = 2
        private const val VIEW_FILE = 3
        private const val VIEW_LINK = 4
        private const val VIEW_DRAWING = 5
        private const val VIEW_VOICE = 6
    }
}
