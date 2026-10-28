package com.notebook.notes.checklist.ui.adapter

import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.KeyEvent
import android.widget.SeekBar
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.attachment.AttachmentIntentHelper
import com.notebook.notes.checklist.attachment.AttachmentViewTypeResolver
import com.notebook.notes.checklist.attachment.FileTypeIconResolver
import com.notebook.notes.checklist.databinding.ItemAttachmentAudioBinding
import com.notebook.notes.checklist.databinding.ItemAttachmentDocumentBinding
import com.notebook.notes.checklist.databinding.ItemAttachmentGenericBinding
import com.notebook.notes.checklist.databinding.ItemAttachmentImageBinding
import com.notebook.notes.checklist.databinding.ItemAttachmentVideoBinding
import com.notebook.notes.checklist.databinding.ItemBlockDrawingBinding
import com.notebook.notes.checklist.databinding.ItemBlockImageBinding
import com.notebook.notes.checklist.databinding.ItemBlockLinkBinding
import com.notebook.notes.checklist.databinding.ItemBlockDividerBinding
import com.notebook.notes.checklist.databinding.ItemBlockTextBinding
import com.notebook.notes.checklist.databinding.ItemBlockVoiceBinding
import com.notebook.notes.checklist.databinding.ItemBulletListBinding
import com.notebook.notes.checklist.databinding.ItemNoteMediaRowBinding
import com.notebook.notes.checklist.databinding.ItemNumberListBinding
import com.notebook.notes.checklist.databinding.ItemAddTodoFooterBinding
import com.notebook.notes.checklist.databinding.ItemTodoBinding
import com.notebook.notes.checklist.model.AttachmentMetadata
import com.notebook.notes.checklist.model.AttachmentCategory
import com.notebook.notes.checklist.model.BlockType
import com.notebook.notes.checklist.model.NoteBlock
import com.notebook.notes.checklist.model.NoteBlockExtra
import com.notebook.notes.checklist.model.NoteContentItem
import com.notebook.notes.checklist.model.BulletListHelper
import com.notebook.notes.checklist.model.MediaBlockHelper
import com.notebook.notes.checklist.model.NumberListHelper
import com.notebook.notes.checklist.model.TodoItem
import com.notebook.notes.checklist.model.VoiceNoteMetadata
import com.notebook.notes.checklist.model.formatAttachmentMeta
import com.notebook.notes.checklist.model.formatFileSize
import com.notebook.notes.checklist.model.formatVoiceDuration
import com.notebook.notes.checklist.ui.audio.VoiceWaveformHelper
import com.notebook.notes.checklist.ui.note.NoteItemOptionsConfig
import com.notebook.notes.checklist.ui.note.NoteItemOptionsListener
import com.notebook.notes.checklist.ui.note.NoteItemOptionsPopup
import com.notebook.notes.checklist.ui.list.EmptyListItemDeleteHelper
import com.notebook.notes.checklist.ui.todo.TodoItemBinder
import com.notebook.notes.checklist.ui.todo.TodoItemTouchHelperCallback
import androidx.recyclerview.widget.ItemTouchHelper
import com.notebook.notes.checklist.ui.richtext.RichTextEditText
import com.notebook.notes.checklist.ui.richtext.RichTextEditorController
import com.notebook.notes.checklist.ui.richtext.RichTextEditorHost
import com.notebook.notes.checklist.ui.richtext.RichTextFormats
import com.notebook.notes.checklist.util.SearchHighlightHelper
import com.notebook.notes.checklist.util.SearchHighlightPalette
import com.notebook.notes.checklist.utils.JsonUtils
import java.io.File

class NoteContentAdapter(
    private val richTextHost: RichTextEditorHost,
    private val defaultTypingFontSize: Int? = null,
    private val defaultTypingFontFamily: String? = null,
    private var defaultLineSpacingExtra: Int? = null,
    private val onBlockChanged: (Long, String, Boolean?) -> Unit,
    private val onBlockRemoved: (Long) -> Unit,
    private val onTodoToggled: (Long) -> Unit,
    private val onTodoTextChanged: (Long, String) -> Unit,
    private val onTodoDeleted: (Long) -> Unit,
    private val onAddNextTodoAfter: (Long) -> Unit,
    private val onDeleteEmptyTodo: (Long) -> Unit,
    private val onTodoReordered: (List<NoteContentItem>) -> Unit,
    private val onAddTodoFooterClick: () -> Unit,
    private val onNumberListTextChanged: (Long, String, Int) -> Unit,
    private val onAddNextNumberList: (Long) -> Unit,
    private val onExitNumberListOnEmpty: (Long) -> Unit,
    val numberListOrders: () -> Map<Long, Int>,
    private val onBulletListTextChanged: (Long, String, Int) -> Unit,
    private val onAddNextBulletList: (Long) -> Unit,
    private val onExitBulletListOnEmpty: (Long) -> Unit,
    val bulletListPositions: () -> Map<Long, Int>,
    private val onVoicePlayPause: (Long, String, Long) -> Unit,
    private val onVoiceSeek: (Long, Long) -> Unit,
    private val onVoiceRename: (Long, String?) -> Unit,
    private val onDrawingEdit: (Long, String) -> Unit,
    private val onAttachmentImageOpen: (Long, String, String, String) -> Unit,
    private val onAttachmentVideoOpen: (Long, String, String, String) -> Unit,
    private val onAttachmentAudioPlayPause: (Long, String, Long) -> Unit,
    private val onAttachmentAudioSeek: (Long, String, Long, Long) -> Unit,
    private val onAttachmentOpen: (String, String) -> Unit,
    private val onAttachmentRename: (Long, String?) -> Unit,
    private val voicePlaybackState: () -> VoicePlaybackState,
    private val attachmentPlaybackState: () -> VoicePlaybackState
) : ListAdapter<NoteContentItem, RecyclerView.ViewHolder>(ContentDiffCallback()) {

    private val textControllers = mutableMapOf<Long, RichTextEditorController>()
    private val todoControllers = mutableMapOf<Long, RichTextEditorController>()
    private val numberListControllers = mutableMapOf<Long, RichTextEditorController>()
    private val bulletListControllers = mutableMapOf<Long, RichTextEditorController>()
    private val todoItemBinder = TodoItemBinder(object : TodoItemBinder.Callbacks {
        override fun onItemToggled(itemId: Long) = onTodoToggled(itemId)
        override fun onItemDeleted(itemId: Long) {
            todoControllers.remove(itemId)
            onTodoDeleted(itemId)
        }
        override fun onAddNextAfter(itemId: Long) = onAddNextTodoAfter(itemId)
        override fun onDeleteIfEmpty(itemId: Long) = onDeleteEmptyTodo(itemId)
        override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
            itemTouchHelper?.startDrag(viewHolder)
        }
    })
    private var itemTouchHelper: ItemTouchHelper? = null
    private var dragInProgress = false
    private var pendingFocusBlockId: Long? = null
    private var pendingFocusTodoId: Long? = null
    private var pendingFocusNumberListId: Long? = null
    private var pendingFocusBulletListId: Long? = null
    private var searchQuery: String = ""
    private var searchHighlightPalette: SearchHighlightPalette? = null
    private var isReadingMode: Boolean = false
    /** Stable id of the body editor the user is on, so its caret survives scroll recycling. */
    private var retainedFocusStableId: Long? = null
    /** Caret offset captured when the retained editor blurs (e.g. recycled by a scroll). */
    private var retainedFocusSelection: Int = -1
    private var adaptiveTextColor: Int? = null
    private var adaptiveHintColor: Int? = null
    private var adaptiveMutedColor: Int? = null

    init {
        setHasStableIds(true)
    }

    fun setReadingMode(enabled: Boolean) {
        if (isReadingMode == enabled) return
        isReadingMode = enabled
        todoItemBinder.isReadingMode = enabled
        if (enabled) clearRetainedFocus()
        notifyDataSetChanged()
    }

    /**
     * Updates create-note text, todo, and chrome icons with derived ink colors.
     * Stored rich-text content is unchanged; default spans use [NoteColorUtils] override at draw time.
     */
    fun setAdaptiveTextColors(textColor: Int, hintColor: Int, mutedColor: Int = hintColor) {
        if (
            adaptiveTextColor == textColor &&
            adaptiveHintColor == hintColor &&
            adaptiveMutedColor == mutedColor
        ) return
        adaptiveTextColor = textColor
        adaptiveHintColor = hintColor
        adaptiveMutedColor = mutedColor
        todoItemBinder.adaptiveInkColor = textColor
        todoItemBinder.adaptiveMutedColor = mutedColor
        refreshNoteTextItemPaints()
    }

    private fun refreshNoteTextItemPaints() {
        val textColor = adaptiveTextColor ?: return
        val hintColor = adaptiveHintColor ?: textColor
        textControllers.values.forEach { it.applyAdaptivePaintColors(textColor, hintColor) }
        todoControllers.values.forEach { it.applyAdaptivePaintColors(textColor, hintColor) }
        numberListControllers.values.forEach { it.applyAdaptivePaintColors(textColor, hintColor) }
        bulletListControllers.values.forEach { it.applyAdaptivePaintColors(textColor, hintColor) }
        notifyItemRangeChanged(0, itemCount)
    }

    private fun applyAdaptiveColorsIfNeeded(editText: android.widget.EditText) {
        val textColor = adaptiveTextColor ?: return
        val hintColor = adaptiveHintColor ?: textColor
        editText.setTextColor(textColor)
        editText.setHintTextColor(hintColor)
    }

    /**
     * For new-note sessions only: blank editors start typing with Settings defaults.
     * Also applies typeface to the EditText so empty hint placeholders match the family.
     * Existing notes pass null and keep stored formatting untouched.
     */
    private fun applyDefaultTypingFontSizeIfNeeded(
        controller: RichTextEditorController,
        editText: android.widget.EditText
    ) {
        if (controller.getDocument().plainText().isNotBlank()) return
        var style = controller.typingStyle
        defaultTypingFontSize?.let { size ->
            style = style.copy(fontSize = size)
            editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, size.toFloat())
        }
        defaultTypingFontFamily?.let { family ->
            style = style.copy(fontFamily = family)
            editText.typeface = RichTextFormats.resolveTypeface(
                family = family,
                bold = style.bold,
                italic = style.italic,
                context = editText.context
            )
        }
        controller.typingStyle = style
    }

    /**
     * For new-note sessions / persisted note spacing: apply Settings list-item height
     * as lineSpacingExtra. Null leaves the editor layout spacing unchanged.
     */
    private fun applyDefaultLineSpacingIfNeeded(editText: android.widget.EditText) {
        val extraPx = defaultLineSpacingExtra ?: return
        editText.setLineSpacing(extraPx.toFloat(), 1.0f)
    }

    /**
     * Scales bullet/number prefixes (and checklist chrome) to match the list item's
     * rich-text font size so large Heading/Title styles do not look misaligned.
     * Only updates when the size actually changes — setTextSize/layoutParams always
     * trigger a layout pass and cause zig-zag flicker during rapid typing.
     */
    private fun resolveListItemFontSizeSp(controller: RichTextEditorController): Int {
        val fromTyping = controller.typingStyle.fontSize.takeIf { it > 0 } ?: 0
        val current = controller.getCurrentFontSize()
        return maxOf(fromTyping, current, 12).coerceIn(12, 48)
    }

    private fun syncListPrefixTextSize(
        prefix: android.widget.TextView,
        controller: RichTextEditorController
    ) {
        val sizeSp = resolveListItemFontSizeSp(controller).toFloat()
        val targetPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sizeSp,
            prefix.resources.displayMetrics
        )
        if (kotlin.math.abs(prefix.textSize - targetPx) < 0.5f) return
        prefix.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    }

    private fun syncChecklistChromeSize(
        binding: ItemTodoBinding,
        controller: RichTextEditorController
    ) {
        val sizeSp = resolveListItemFontSizeSp(controller)
        val density = binding.root.resources.displayMetrics.density
        val checkSizePx = (sizeSp * density)
            .toInt()
            .coerceIn((24 * density).toInt(), (36 * density).toInt())
        val checkLp = binding.ivTodoCheck.layoutParams
        if (checkLp.width != checkSizePx || checkLp.height != checkSizePx) {
            checkLp.width = checkSizePx
            checkLp.height = checkSizePx
            binding.ivTodoCheck.layoutParams = checkLp
        }
    }

    /** Updates spacing for newly bound and already-bound editors (e.g. when opening a saved note). */
    fun setLineSpacingExtra(extraPx: Int?) {
        if (defaultLineSpacingExtra == extraPx) return
        defaultLineSpacingExtra = extraPx
        val apply: (RichTextEditorController) -> Unit = { controller ->
            applyDefaultLineSpacingIfNeeded(controller.editorView)
        }
        textControllers.values.forEach(apply)
        todoControllers.values.forEach(apply)
        numberListControllers.values.forEach(apply)
        bulletListControllers.values.forEach(apply)
    }

    private fun applyTodoChromeColors(binding: ItemTodoBinding, isCompleted: Boolean) {
        val muted = adaptiveMutedColor ?: adaptiveHintColor ?: return
        val ink = adaptiveTextColor ?: return
        binding.ivDragHandle.setColorFilter(muted)
        binding.ivDelete.setColorFilter(muted)
        if (isCompleted) {
            binding.ivTodoCheck.clearColorFilter()
        } else {
            binding.ivTodoCheck.setColorFilter(ink)
        }
    }

    fun setSearchQuery(query: String, palette: SearchHighlightPalette?) {
        val normalized = query.trim()
        if (searchQuery == normalized && searchHighlightPalette == palette) return
        searchQuery = normalized
        searchHighlightPalette = palette
        applySearchHighlightToControllers()
    }

    fun findFirstSearchMatchPosition(): Int = findSearchMatchPosition(matchOrdinal = 0)

    /**
     * Finds the adapter position for the [matchOrdinal]-th content match (0-based across blocks).
     * When [preferBlockId] is set, prefers a match in that block at/after [preferStartOffset].
     */
    fun findSearchMatchPosition(
        matchOrdinal: Int = 0,
        preferBlockId: Long? = null,
        preferStartOffset: Int = 0
    ): Int {
        if (searchQuery.isEmpty()) return -1
        if (preferBlockId != null) {
            val preferredIndex = currentList.indexOfFirst { item ->
                when (item) {
                    is NoteContentItem.BlockItem -> item.block.id == preferBlockId
                    is NoteContentItem.TodoEntry -> item.item.id == preferBlockId
                    else -> false
                }
            }
            if (preferredIndex >= 0) {
                val controller = controllerForPosition(preferredIndex)
                val fromCursor = controller?.searchMatchIndexFrom(preferStartOffset) ?: -1
                if (fromCursor >= 0) return preferredIndex
                if (controller?.hasSearchMatch() == true) return preferredIndex
                if (plainTextHasMatchAt(preferredIndex)) return preferredIndex
            }
        }
        var remaining = matchOrdinal.coerceAtLeast(0)
        currentList.forEachIndexed { index, item ->
            val matchCount = searchMatchCountAt(index, item)
            if (matchCount <= 0) return@forEachIndexed
            if (remaining < matchCount) return index
            remaining -= matchCount
        }
        return -1
    }

    fun countSearchMatches(): Int {
        if (searchQuery.isEmpty()) return 0
        return currentList.indices.sumOf { searchMatchCountAt(it, currentList[it]) }
    }

    /**
     * Scrolls the matching block into view and optionally focuses/selects the match in the editor.
     * Returns true when a content match was found.
     */
    fun navigateToSearchMatch(
        recyclerView: RecyclerView,
        moveFocus: Boolean,
        matchOrdinal: Int = 0,
        preferBlockId: Long? = null,
        preferStartOffset: Int = 0
    ): Boolean {
        if (searchQuery.isEmpty()) return false
        val position = findSearchMatchPosition(matchOrdinal, preferBlockId, preferStartOffset)
        if (position < 0) return false
        (recyclerView.layoutManager as? LinearLayoutManager)
            ?.scrollToPositionWithOffset(position, 0)
            ?: recyclerView.scrollToPosition(position)
        if (!moveFocus) return true
        val selectionStart = resolveMatchStartInBlock(
            position = position,
            matchOrdinal = matchOrdinal,
            preferBlockId = preferBlockId,
            preferStartOffset = preferStartOffset
        )
        recyclerView.post {
            tryFocusSearchMatch(recyclerView, position, selectionStart, 0)
        }
        return true
    }

    private fun searchMatchCountAt(index: Int, item: NoteContentItem): Int {
        val controller = controllerForPosition(index)
        if (controller != null) {
            val indices = controller.allSearchMatchIndices()
            if (indices.isNotEmpty()) return indices.size
        }
        return if (plainTextHasMatchAt(index)) {
            val plain = plainTextAt(index) ?: return 0
            SearchHighlightHelper.allMatchIndices(plain, searchQuery).size
        } else {
            0
        }
    }

    private fun plainTextHasMatchAt(index: Int): Boolean {
        val plain = plainTextAt(index) ?: return false
        return SearchHighlightHelper.firstMatchIndex(plain, searchQuery) >= 0
    }

    private fun plainTextAt(index: Int): String? {
        return when (val item = currentList.getOrNull(index)) {
            is NoteContentItem.BlockItem -> when (item.block.type) {
                BlockType.TEXT, BlockType.NUMBER_LIST, BlockType.BULLET_LIST ->
                    JsonUtils.blockContentPlainText(item.block.content)
                else -> null
            }
            is NoteContentItem.TodoEntry ->
                JsonUtils.jsonToRichText(item.item.text).plainText()
            else -> null
        }
    }

    private fun controllerForPosition(position: Int): RichTextEditorController? {
        return when (val item = currentList.getOrNull(position)) {
            is NoteContentItem.BlockItem -> when (item.block.type) {
                BlockType.TEXT -> textControllers[item.block.id]
                BlockType.NUMBER_LIST -> numberListControllers[item.block.id]
                BlockType.BULLET_LIST -> bulletListControllers[item.block.id]
                else -> null
            }
            is NoteContentItem.TodoEntry -> todoControllers[item.item.id]
            else -> null
        }
    }

    private fun resolveMatchStartInBlock(
        position: Int,
        matchOrdinal: Int,
        preferBlockId: Long?,
        preferStartOffset: Int
    ): Int {
        val controller = controllerForPosition(position)
        val item = currentList.getOrNull(position)
        val blockId = when (item) {
            is NoteContentItem.BlockItem -> item.block.id
            is NoteContentItem.TodoEntry -> item.item.id
            else -> null
        }
        if (controller != null && preferBlockId != null && blockId == preferBlockId) {
            val fromCursor = controller.searchMatchIndexFrom(preferStartOffset)
            if (fromCursor >= 0) return fromCursor
        }
        val indices = controller?.allSearchMatchIndices().orEmpty().ifEmpty {
            val plain = plainTextAt(position).orEmpty()
            SearchHighlightHelper.allMatchIndices(plain, searchQuery)
        }
        if (indices.isEmpty()) return 0
        // Map global ordinal onto this block's local match when possible.
        var preceding = 0
        for (i in 0 until position) {
            preceding += searchMatchCountAt(i, currentList[i])
        }
        val localIndex = (matchOrdinal - preceding).coerceIn(0, indices.lastIndex)
        return indices[localIndex]
    }

    private fun tryFocusSearchMatch(
        recyclerView: RecyclerView,
        position: Int,
        matchStart: Int,
        attempt: Int
    ) {
        val controller = controllerForPosition(position)
        if (controller != null) {
            controller.focusSearchMatch(matchStart = matchStart, showKeyboard = false)
            return
        }
        val holder = recyclerView.findViewHolderForAdapterPosition(position)
        if (holder != null) {
            // Holder bound but controller not yet registered — retry shortly.
            if (attempt < 15) {
                recyclerView.post {
                    tryFocusSearchMatch(recyclerView, position, matchStart, attempt + 1)
                }
            }
            return
        }
        if (attempt < 15) {
            recyclerView.post {
                tryFocusSearchMatch(recyclerView, position, matchStart, attempt + 1)
            }
        }
    }

    private fun applySearchHighlightToControllers() {
        val apply = { controller: RichTextEditorController ->
            controller.setSearchHighlight(searchQuery, searchHighlightPalette)
        }
        textControllers.values.forEach(apply)
        todoControllers.values.forEach(apply)
        numberListControllers.values.forEach(apply)
        bulletListControllers.values.forEach(apply)
    }

    fun attachTodoDragHelper(recyclerView: RecyclerView) {
        val callback = TodoItemTouchHelperCallback(
            getItem = { position -> currentList.getOrNull(position) },
            onMove = { from, to -> moveTodoItem(from, to) },
            onDragFinished = {
                if (dragInProgress) {
                    dragInProgress = false
                    onTodoReordered(currentList.filterNot { it is NoteContentItem.AddTodoFooter })
                }
            }
        )
        itemTouchHelper = ItemTouchHelper(callback).also { it.attachToRecyclerView(recyclerView) }
    }

    private fun moveTodoItem(fromPosition: Int, toPosition: Int): Boolean {
        if (fromPosition == toPosition) return false
        val fromItem = currentList.getOrNull(fromPosition) as? NoteContentItem.TodoEntry ?: return false
        val toItem = currentList.getOrNull(toPosition) as? NoteContentItem.TodoEntry ?: return false
        if (fromItem.item.listId != toItem.item.listId) return false
        val updated = currentList.toMutableList()
        val moved = updated.removeAt(fromPosition)
        updated.add(toPosition, moved)
        dragInProgress = true
        submitList(
            NoteContentItem.withAddTodoFooter(
                items = updated.filterNot { it is NoteContentItem.AddTodoFooter },
                showFooter = true
            )
        )
        return true
    }

    override fun getItemId(position: Int): Long = getItem(position).stableId

    fun flushAllTextBlocks() {
        textControllers.values.forEach { it.flushContent() }
        todoControllers.values.forEach { it.flushContent() }
        numberListControllers.values.forEach { it.flushContent() }
        bulletListControllers.values.forEach { it.flushContent() }
    }

    fun getLivePlainTextForBlock(blockId: Long): String? =
        textControllers[blockId]?.getDocument()?.plainText()
            ?: numberListControllers[blockId]?.getDocument()?.plainText()
            ?: bulletListControllers[blockId]?.getDocument()?.plainText()

    fun refreshVoicePlayback() {
        currentList.forEachIndexed { index, item ->
            if (item is NoteContentItem.BlockItem) {
                when (item.block.type) {
                    BlockType.VOICE -> notifyItemChanged(index, PAYLOAD_PLAYBACK)
                    BlockType.ATTACHMENT, BlockType.FILE -> {
                        if (AttachmentMetadata.fromBlock(item.block)?.attachmentCategory() ==
                            com.notebook.notes.checklist.model.AttachmentCategory.AUDIO
                        ) {
                            notifyItemChanged(index, PAYLOAD_PLAYBACK)
                        }
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = when (val item = getItem(position)) {
        is NoteContentItem.AddTodoFooter -> VIEW_ADD_TODO_FOOTER
        is NoteContentItem.TodoEntry -> VIEW_TODO
        is NoteContentItem.MediaRow -> VIEW_MEDIA_ROW
        is NoteContentItem.BlockItem -> when (item.block.type) {
            BlockType.TEXT -> VIEW_TEXT
            BlockType.IMAGE -> VIEW_IMAGE
            BlockType.LINK -> VIEW_LINK
            BlockType.DRAWING -> VIEW_DRAWING
            BlockType.VOICE -> VIEW_VOICE
            BlockType.NUMBER_LIST -> VIEW_NUMBER_LIST
            BlockType.BULLET_LIST -> VIEW_BULLET_LIST
            BlockType.DIVIDER -> VIEW_DIVIDER
            BlockType.ATTACHMENT, BlockType.FILE -> AttachmentViewTypeResolver.viewTypeForBlock(item.block)
            else -> VIEW_TEXT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_ADD_TODO_FOOTER -> AddTodoFooterViewHolder(
                ItemAddTodoFooterBinding.inflate(inflater, parent, false)
            )
            VIEW_TODO -> TodoViewHolder(ItemTodoBinding.inflate(inflater, parent, false))
            VIEW_MEDIA_ROW -> MediaRowViewHolder(ItemNoteMediaRowBinding.inflate(inflater, parent, false))
            VIEW_IMAGE -> ImageViewHolder(ItemBlockImageBinding.inflate(inflater, parent, false))
            VIEW_LINK -> LinkViewHolder(ItemBlockLinkBinding.inflate(inflater, parent, false))
            VIEW_DRAWING -> DrawingViewHolder(ItemBlockDrawingBinding.inflate(inflater, parent, false))
            VIEW_VOICE -> VoiceViewHolder(ItemBlockVoiceBinding.inflate(inflater, parent, false))
            VIEW_NUMBER_LIST -> NumberListViewHolder(ItemNumberListBinding.inflate(inflater, parent, false))
            VIEW_BULLET_LIST -> BulletListViewHolder(ItemBulletListBinding.inflate(inflater, parent, false))
            VIEW_DIVIDER -> DividerViewHolder(ItemBlockDividerBinding.inflate(inflater, parent, false))
            AttachmentViewTypeResolver.VIEW_ATTACHMENT_IMAGE ->
                AttachmentImageViewHolder(ItemAttachmentImageBinding.inflate(inflater, parent, false))
            AttachmentViewTypeResolver.VIEW_ATTACHMENT_VIDEO ->
                AttachmentVideoViewHolder(ItemAttachmentVideoBinding.inflate(inflater, parent, false))
            AttachmentViewTypeResolver.VIEW_ATTACHMENT_AUDIO ->
                AttachmentAudioViewHolder(ItemAttachmentAudioBinding.inflate(inflater, parent, false))
            AttachmentViewTypeResolver.VIEW_ATTACHMENT_DOCUMENT ->
                AttachmentDocumentViewHolder(ItemAttachmentDocumentBinding.inflate(inflater, parent, false))
            AttachmentViewTypeResolver.VIEW_ATTACHMENT_GENERIC ->
                AttachmentGenericViewHolder(ItemAttachmentGenericBinding.inflate(inflater, parent, false))
            else -> TextViewHolder(ItemBlockTextBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NoteContentItem.BlockItem -> when (holder) {
                is TextViewHolder -> holder.bind(item.block)
                is ImageViewHolder -> holder.bind(item.block)
                is LinkViewHolder -> holder.bind(item.block)
                is DrawingViewHolder -> holder.bind(item.block)
                is VoiceViewHolder -> holder.bind(item.block)
                is NumberListViewHolder -> holder.bind(item.block)
                is BulletListViewHolder -> holder.bind(item.block)
                is DividerViewHolder -> holder.bind(item.block)
                is AttachmentImageViewHolder -> holder.bind(item.block)
                is AttachmentVideoViewHolder -> holder.bind(item.block)
                is AttachmentAudioViewHolder -> holder.bind(item.block)
                is AttachmentDocumentViewHolder -> holder.bind(item.block)
                is AttachmentGenericViewHolder -> holder.bind(item.block)
                else -> Unit
            }
            is NoteContentItem.TodoEntry -> (holder as TodoViewHolder).bind(item.item)
            is NoteContentItem.AddTodoFooter -> (holder as AddTodoFooterViewHolder).bind()
            is NoteContentItem.MediaRow -> (holder as MediaRowViewHolder).bind(item)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.contains(PAYLOAD_PLAYBACK)) {
            val item = getItem(position) as? NoteContentItem.BlockItem ?: return
            when (holder) {
                is VoiceViewHolder -> holder.bindPlayback(item.block)
                is AttachmentAudioViewHolder -> holder.bindPlayback(item.block)
                else -> onBindViewHolder(holder, position)
            }
            return
        }
        super.onBindViewHolder(holder, position, payloads)
    }

    inner class AddTodoFooterViewHolder(
        private val binding: ItemAddTodoFooterBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.root.isEnabled = !isReadingMode
            binding.root.alpha = if (isReadingMode) 0.5f else 1.0f
            binding.root.setOnClickListener { onAddTodoFooterClick() }
            val color = adaptiveTextColor ?: adaptiveHintColor
            if (color != null) {
                binding.tvPlus.setTextColor(color)
                binding.tvLabel.setTextColor(color)
            }
        }
    }

    fun getPendingFocusBlockId(): Long? = pendingFocusBlockId

    fun setPendingFocusBlockId(blockId: Long) {
        pendingFocusBlockId = blockId
        pendingFocusTodoId = null
        pendingFocusNumberListId = null
        pendingFocusBulletListId = null
    }

    fun setPendingFocusNumberListId(blockId: Long) {
        pendingFocusNumberListId = blockId
        pendingFocusBlockId = null
        pendingFocusTodoId = null
        pendingFocusBulletListId = null
    }

    fun setPendingFocusBulletListId(blockId: Long) {
        pendingFocusBulletListId = blockId
        pendingFocusBlockId = null
        pendingFocusTodoId = null
        pendingFocusNumberListId = null
    }

    fun focusBulletListBlock(recyclerView: RecyclerView, blockId: Long) {
        pendingFocusBulletListId = blockId
        pendingFocusBlockId = null
        pendingFocusTodoId = null
        pendingFocusNumberListId = null
        ensureVisibleAndFocus(recyclerView, blockId) { holder ->
            (holder as? BulletListViewHolder)?.requestFocus()
        }
    }

    fun focusNumberListBlock(recyclerView: RecyclerView, blockId: Long) {
        pendingFocusNumberListId = blockId
        pendingFocusBlockId = null
        pendingFocusTodoId = null
        pendingFocusBulletListId = null
        ensureVisibleAndFocus(recyclerView, blockId) { holder ->
            (holder as? NumberListViewHolder)?.requestFocus()
        }
    }

    fun focusTodoItem(recyclerView: RecyclerView, itemId: Long) {
        pendingFocusTodoId = itemId
        todoItemBinder.pendingFocusItemId = itemId
        pendingFocusBlockId = null
        ensureVisibleAndFocus(
            recyclerView = recyclerView,
            stableId = -1_000_000_000L - itemId
        ) { holder ->
            (holder as? TodoViewHolder)?.requestFocusAtEnd()
        }
    }

    fun focusTextBlock(
        recyclerView: RecyclerView,
        blockId: Long,
        onFocused: (() -> Unit)? = null
    ) {
        pendingFocusBlockId = blockId
        pendingFocusTodoId = null
        ensureVisibleAndFocus(recyclerView, blockId) { holder ->
            (holder as? TextViewHolder)?.requestEditorFocus(onFocused)
        }
    }

    /**
     * Quietly restores the caret to the editor the user was on, but only if that
     * row is currently visible (bound). No scrolling, no keyboard pop — meant to be
     * called after a scroll settles so the edited item doesn't lose its caret to
     * RecyclerView recycling. Off-screen rows are left untouched.
     */
    fun restoreRetainedFocusIfVisible(recyclerView: RecyclerView) {
        if (isReadingMode) return
        val stableId = retainedFocusStableId ?: return
        val holder = recyclerView.findViewHolderForItemId(stableId) ?: return
        val editText = editorFromHolder(holder) ?: return
        if (editText.hasFocus()) return
        // Suppress the keyboard/panel side-effects so a scroll never re-pops the IME.
        editText.suppressTypingKeyboardRequest = true
        if (!editText.requestFocus()) editText.requestFocusFromTouch()
        val length = editText.text?.length ?: 0
        val selection = retainedFocusSelection.takeIf { it in 0..length } ?: length
        editText.setSelection(selection)
        editText.post { editText.suppressTypingKeyboardRequest = false }
    }

    /** Drops the retained editor (e.g. when the title takes focus or reading mode starts). */
    fun clearRetainedFocus() {
        retainedFocusStableId = null
        retainedFocusSelection = -1
    }

    private fun editorFromHolder(holder: RecyclerView.ViewHolder): RichTextEditText? =
        when (holder) {
            is TextViewHolder -> holder.editor
            is TodoViewHolder -> holder.editor
            is NumberListViewHolder -> holder.editor
            is BulletListViewHolder -> holder.editor
            else -> null
        }

    private fun ensureVisibleAndFocus(
        recyclerView: RecyclerView,
        stableId: Long,
        onHolder: (RecyclerView.ViewHolder?) -> Unit
    ) {
        val index = currentList.indexOfFirst { it.stableId == stableId }
        if (index < 0) return
        val lm = recyclerView.layoutManager as? LinearLayoutManager
        val first = lm?.findFirstVisibleItemPosition() ?: -1
        val last = lm?.findLastVisibleItemPosition() ?: -1
        // Only jump-scroll when the target row is off-screen — never pin to top
        // while the user is already looking at a tall todo.
        if (lm == null || index < first || index > last) {
            lm?.scrollToPosition(index) ?: recyclerView.scrollToPosition(index)
        }
        recyclerView.post { tryFocusHolder(recyclerView, stableId, 0, onHolder) }
    }

    private fun tryFocusHolder(
        recyclerView: RecyclerView,
        stableId: Long,
        attempt: Int,
        onHolder: (RecyclerView.ViewHolder?) -> Unit
    ) {
        val index = currentList.indexOfFirst { it.stableId == stableId }
        if (index < 0) {
            pendingFocusBlockId = null
            pendingFocusTodoId = null
            return
        }
        val holder = recyclerView.findViewHolderForAdapterPosition(index)
        if (holder != null) {
            onHolder(holder)
            pendingFocusBlockId = null
            pendingFocusTodoId = null
        } else if (attempt < 15) {
            recyclerView.post { tryFocusHolder(recyclerView, stableId, attempt + 1, onHolder) }
        } else {
            pendingFocusBlockId = null
            pendingFocusTodoId = null
        }
    }

    inner class TextViewHolder(
        private val binding: ItemBlockTextBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val editor: RichTextEditText get() = binding.richTextEditor
        private var boundBlockId: Long? = null

        fun bind(block: NoteBlock) {
            val editText = binding.richTextEditor
            editText.isFocusable = !isReadingMode
            editText.isFocusableInTouchMode = !isReadingMode
            editText.isCursorVisible = !isReadingMode
            
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
                    ctrl.blockId = block.id
                    editText.controller = ctrl
                    ctrl.loadDocument(JsonUtils.jsonToRichText(block.content))
                    applyDefaultTypingFontSizeIfNeeded(ctrl, editText)
                    applyDefaultLineSpacingIfNeeded(editText)
                    ctrl.setSearchHighlight(searchQuery, searchHighlightPalette)
                    editText.onFocusChangedListener = { _, focused ->
                        if (focused) {
                            retainedFocusStableId = block.id
                            richTextHost.onRichTextEditorFocused(ctrl)
                        } else {
                            if (retainedFocusStableId == block.id) {
                                retainedFocusSelection = editText.selectionEnd
                            }
                            richTextHost.onRichTextEditorBlurred()
                        }
                    }
                    ctrl.onContentChanged = { document ->
                        onBlockChanged(block.id, JsonUtils.richTextToJson(document), null)
                    }
                }
            }
            applyAdaptiveColorsIfNeeded(editText)
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
                editText.requestFocus()
                editText.setSelection(editText.text?.length ?: 0)
                editText.controller?.let { richTextHost.onRichTextEditorFocused(it) }
                val imm = editText.context.getSystemService(InputMethodManager::class.java)
                imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                pendingFocusBlockId = null
                onFocused?.invoke()
            }
            if (editText.isLaidOut) editText.post { applyFocus() }
            else editText.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (!editText.isLaidOut) return
                    editText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    editText.post { applyFocus() }
                }
            })
        }

        fun requestEditorFocus(onFocused: (() -> Unit)? = null) {
            scheduleEditorFocus(binding.richTextEditor, onFocused)
        }
    }

    inner class DividerViewHolder(
        binding: ItemBlockDividerBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) = Unit
    }

    inner class TodoViewHolder(
        private val binding: ItemTodoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val editor: RichTextEditText get() = binding.etTodoText
        private var boundItemId: Long? = null

        fun bind(item: TodoItem) {
            setupRichTextController(item)
            todoItemBinder.bindActions(binding, item, this)
            applyAdaptiveColorsIfNeeded(binding.etTodoText)
            applyTodoChromeColors(binding, item.isCompleted)
            binding.etTodoText.controller?.let { syncChecklistChromeSize(binding, it) }
        }

        private fun setupRichTextController(item: TodoItem) {
            val editText = binding.etTodoText
            val existing = todoControllers[item.id]
            if (existing == null || boundItemId != item.id) {
                boundItemId = item.id
                RichTextEditorController(editText).also { ctrl ->
                    todoControllers[item.id] = ctrl
                    ctrl.blockId = item.id
                    editText.controller = ctrl
                    ctrl.loadDocument(JsonUtils.jsonToRichText(item.text))
                    applyDefaultTypingFontSizeIfNeeded(ctrl, editText)
                    applyDefaultLineSpacingIfNeeded(editText)
                    ctrl.setSearchHighlight(searchQuery, searchHighlightPalette)
                    val todoStableId = -1_000_000_000L - item.id
                    editText.onFocusChangedListener = { _, focused ->
                        if (focused) {
                            retainedFocusStableId = todoStableId
                            richTextHost.onRichTextEditorFocused(ctrl)
                        } else {
                            if (retainedFocusStableId == todoStableId) {
                                retainedFocusSelection = editText.selectionEnd
                            }
                            richTextHost.onRichTextEditorBlurred()
                        }
                    }
                    ctrl.onContentChanged = { document ->
                        onTodoTextChanged(item.id, JsonUtils.richTextToJson(document))
                    }
                    ctrl.onStyleMetricsChanged = {
                        // Font-size / heading changes only — never during normal typing.
                        syncChecklistChromeSize(binding, ctrl)
                    }
                    syncChecklistChromeSize(binding, ctrl)
                }
            }
            applyAdaptiveColorsIfNeeded(editText)
            todoItemBinder.applyCompletedStyle(binding, item.isCompleted)
            if (item.id == pendingFocusTodoId) {
                scheduleEditorFocus(editText)
            }
        }

        private fun scheduleEditorFocus(
            editText: RichTextEditText,
            placeCaretAtEnd: Boolean = true,
            onFocused: (() -> Unit)? = null
        ) {
            fun applyFocus() {
                editText.requestFocus()
                if (placeCaretAtEnd) {
                    editText.setSelection(editText.text?.length ?: 0)
                }
                editText.controller?.let { richTextHost.onRichTextEditorFocused(it) }
                val imm = editText.context.getSystemService(InputMethodManager::class.java)
                imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
                pendingFocusTodoId = null
                onFocused?.invoke()
            }
            if (editText.isLaidOut) editText.post { applyFocus() }
            else editText.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (!editText.isLaidOut) return
                    editText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    editText.post { applyFocus() }
                }
            })
        }

        fun requestFocus() {
            scheduleEditorFocus(binding.etTodoText, placeCaretAtEnd = true)
        }

        /** Programmatic focus for a newly added item — caret at end. */
        fun requestFocusAtEnd() {
            scheduleEditorFocus(binding.etTodoText, placeCaretAtEnd = true)
        }
    }

    inner class NumberListViewHolder(
        private val binding: ItemNumberListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val editor: RichTextEditText get() = binding.etText
        private var boundBlockId: Long? = null

        fun bind(block: NoteBlock) {
            val order = numberListOrders()[block.id] ?: 1
            binding.tvNumber.text = NumberListHelper.formatPrefix(order)
            
            binding.etText.isFocusable = !isReadingMode
            binding.etText.isFocusableInTouchMode = !isReadingMode
            binding.etText.isCursorVisible = !isReadingMode
            
            setupRichTextController(block)
            attachListItemActions(
                editText = binding.etText,
                isEmpty = { binding.etText.controller?.getDocument()?.plainText().isNullOrBlank() },
                onDeleteWhenEmpty = {
                    numberListControllers.remove(block.id)
                    onExitNumberListOnEmpty(block.id)
                },
                onAddNext = { onAddNextNumberList(block.id) },
                // Next on an empty row should insert another empty item, not remove this one.
                // Backspace on empty still exits/deletes via EmptyListItemDeleteHelper.
                addNextEvenIfEmpty = true
            )
            applyAdaptiveColorsIfNeeded(binding.etText)
            adaptiveTextColor?.let { binding.tvNumber.setTextColor(it) }
            binding.etText.controller?.let { syncListPrefixTextSize(binding.tvNumber, it) }
            if (block.id == pendingFocusNumberListId) requestFocus()
        }

        private fun setupRichTextController(block: NoteBlock) {
            val editText = binding.etText
            val existing = numberListControllers[block.id]
            if (existing == null || boundBlockId != block.id) {
                boundBlockId = block.id
                RichTextEditorController(editText).also { ctrl ->
                    numberListControllers[block.id] = ctrl
                    ctrl.blockId = block.id
                    editText.controller = ctrl
                    ctrl.loadDocument(JsonUtils.jsonToRichText(block.content))
                    applyDefaultTypingFontSizeIfNeeded(ctrl, editText)
                    applyDefaultLineSpacingIfNeeded(editText)
                    ctrl.setSearchHighlight(searchQuery, searchHighlightPalette)
                    editText.onFocusChangedListener = { _, focused ->
                        if (focused) {
                            retainedFocusStableId = block.id
                            richTextHost.onRichTextEditorFocused(ctrl)
                        } else {
                            if (retainedFocusStableId == block.id) {
                                retainedFocusSelection = editText.selectionEnd
                            }
                            richTextHost.onRichTextEditorBlurred()
                        }
                    }
                    ctrl.onContentChanged = { document ->
                        val order = numberListOrders()[block.id] ?: 1
                        onNumberListTextChanged(block.id, JsonUtils.richTextToJson(document), order)
                    }
                    ctrl.onStyleMetricsChanged = {
                        syncListPrefixTextSize(binding.tvNumber, ctrl)
                    }
                    syncListPrefixTextSize(binding.tvNumber, ctrl)
                }
            }
        }

        fun requestFocus() {
            scheduleRichTextFocus(binding.etText) { pendingFocusNumberListId = null }
        }
    }

    inner class BulletListViewHolder(
        private val binding: ItemBulletListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val editor: RichTextEditText get() = binding.etText
        private var boundBlockId: Long? = null

        fun bind(block: NoteBlock) {
            binding.tvBullet.text = BulletListHelper.BULLET_PREFIX
            
            binding.etText.isFocusable = !isReadingMode
            binding.etText.isFocusableInTouchMode = !isReadingMode
            binding.etText.isCursorVisible = !isReadingMode
            
            setupRichTextController(block)
            attachListItemActions(
                editText = binding.etText,
                isEmpty = { binding.etText.controller?.getDocument()?.plainText().isNullOrBlank() },
                onDeleteWhenEmpty = {
                    bulletListControllers.remove(block.id)
                    onExitBulletListOnEmpty(block.id)
                },
                onAddNext = { onAddNextBulletList(block.id) }
            )
            applyAdaptiveColorsIfNeeded(binding.etText)
            adaptiveTextColor?.let { binding.tvBullet.setTextColor(it) }
            binding.etText.controller?.let { syncListPrefixTextSize(binding.tvBullet, it) }
            if (block.id == pendingFocusBulletListId) requestFocus()
        }

        private fun setupRichTextController(block: NoteBlock) {
            val editText = binding.etText
            val existing = bulletListControllers[block.id]
            if (existing == null || boundBlockId != block.id) {
                boundBlockId = block.id
                RichTextEditorController(editText).also { ctrl ->
                    bulletListControllers[block.id] = ctrl
                    ctrl.blockId = block.id
                    editText.controller = ctrl
                    ctrl.loadDocument(JsonUtils.jsonToRichText(block.content))
                    applyDefaultTypingFontSizeIfNeeded(ctrl, editText)
                    applyDefaultLineSpacingIfNeeded(editText)
                    ctrl.setSearchHighlight(searchQuery, searchHighlightPalette)
                    editText.onFocusChangedListener = { _, focused ->
                        if (focused) {
                            retainedFocusStableId = block.id
                            richTextHost.onRichTextEditorFocused(ctrl)
                        } else {
                            if (retainedFocusStableId == block.id) {
                                retainedFocusSelection = editText.selectionEnd
                            }
                            richTextHost.onRichTextEditorBlurred()
                        }
                    }
                    ctrl.onContentChanged = { document ->
                        val itemPosition = bulletListPositions()[block.id] ?: 0
                        onBulletListTextChanged(block.id, JsonUtils.richTextToJson(document), itemPosition)
                    }
                    ctrl.onStyleMetricsChanged = {
                        syncListPrefixTextSize(binding.tvBullet, ctrl)
                    }
                    syncListPrefixTextSize(binding.tvBullet, ctrl)
                }
            }
        }

        fun requestFocus() {
            scheduleRichTextFocus(binding.etText) { pendingFocusBulletListId = null }
        }
    }

    private fun attachListItemActions(
        editText: RichTextEditText,
        isEmpty: () -> Boolean,
        onDeleteWhenEmpty: () -> Unit,
        onAddNext: () -> Unit,
        addNextEvenIfEmpty: Boolean = false
    ) {
        editText.imeOptions = EditorInfo.IME_ACTION_NEXT
        EmptyListItemDeleteHelper.attach(
            editText = editText,
            isEmpty = isEmpty,
            onDeleteWhenEmpty = onDeleteWhenEmpty
        )
        editText.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT ||
                actionId == EditorInfo.IME_ACTION_DONE ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                if (isEmpty() && !addNextEvenIfEmpty) onDeleteWhenEmpty() else onAddNext()
                true
            } else {
                false
            }
        }
    }

    private fun scheduleRichTextFocus(
        editText: RichTextEditText,
        onFocused: (() -> Unit)? = null
    ) {
        fun applyFocus() {
            editText.requestFocus()
            editText.setSelection(editText.text?.length ?: 0)
            editText.controller?.let { richTextHost.onRichTextEditorFocused(it) }
            val imm = editText.context.getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            onFocused?.invoke()
        }
        if (editText.isLaidOut) editText.post { applyFocus() }
        else editText.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (!editText.isLaidOut) return
                editText.viewTreeObserver.removeOnGlobalLayoutListener(this)
                editText.post { applyFocus() }
            }
        })
    }

    inner class MediaRowViewHolder(
        private val binding: ItemNoteMediaRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: NoteContentItem.MediaRow) {
            val endBlock = row.blocks.getOrNull(1)
            bindTile(
                block = row.blocks[0],
                imageView = binding.ivImageStart,
                playOverlay = binding.ivPlayStart,
                labelView = binding.tvLabelStart,
                moreButton = binding.btnMoreStart,
                clickableRoot = binding.cardTileStart
            )
            if (endBlock != null) {
                binding.mediaTileSpacer.visibility = android.view.View.VISIBLE
                binding.mediaTilePlaceholder.visibility = android.view.View.GONE
                binding.cardTileEnd.visibility = android.view.View.VISIBLE
                bindTile(
                    block = endBlock,
                    imageView = binding.ivImageEnd,
                    playOverlay = binding.ivPlayEnd,
                    labelView = binding.tvLabelEnd,
                    moreButton = binding.btnMoreEnd,
                    clickableRoot = binding.cardTileEnd
                )
            } else {
                binding.mediaTileSpacer.visibility = android.view.View.VISIBLE
                binding.mediaTilePlaceholder.visibility = android.view.View.VISIBLE
                binding.cardTileEnd.visibility = android.view.View.GONE
            }
        }

        private fun bindTile(
            block: NoteBlock,
            imageView: android.widget.ImageView,
            playOverlay: android.widget.ImageView,
            labelView: android.widget.TextView,
            moreButton: android.widget.ImageButton,
            clickableRoot: android.view.View
        ) {
            val metadata = AttachmentMetadata.fromBlock(block)
            val isVideo = MediaBlockHelper.isVideo(block)
            val file = File(block.content)
            val displayName = when {
                metadata != null -> metadata.displayLabel()
                file.exists() -> file.name
                else -> ""
            }
            val mimeType = when {
                metadata != null -> metadata.mimeType
                block.type == BlockType.DRAWING -> "image/png"
                else -> "image/*"
            }

            if (isVideo) {
                val thumb = metadata?.thumbnailPath?.let { File(it) }?.takeIf { it.exists() }
                if (thumb != null) {
                    imageView.load(thumb) { crossfade(true) }
                } else {
                    imageView.setImageResource(R.drawable.ic_image)
                }
                imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                playOverlay.visibility = android.view.View.VISIBLE
                labelView.visibility = android.view.View.VISIBLE
                labelView.text = displayName
                clickableRoot.setOnClickListener {
                    onAttachmentVideoOpen(block.id, block.content, displayName, mimeType)
                }
            } else {
                if (file.exists()) {
                    imageView.load(file) { crossfade(true) }
                } else {
                    imageView.setImageResource(R.drawable.ic_image)
                }
                imageView.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                playOverlay.visibility = android.view.View.GONE
                labelView.visibility = android.view.View.GONE
                clickableRoot.setOnClickListener {
                    onAttachmentImageOpen(block.id, block.content, displayName, mimeType)
                }
            }

            moreButton.isEnabled = !isReadingMode
            moreButton.alpha = if (isReadingMode) 0.5f else 1.0f
            moreButton.setOnClickListener { anchor ->
                if (isReadingMode) return@setOnClickListener
                showNoteItemOptions(
                    anchor = anchor,
                    config = NoteItemOptionsConfig(),
                    onShare = { shareBlockFile(anchor, block.content, mimeType) },
                    onView = {
                        when {
                            isVideo -> onAttachmentVideoOpen(block.id, block.content, displayName, mimeType)
                            file.exists() -> onAttachmentImageOpen(block.id, block.content, displayName, mimeType)
                        }
                    },
                    onDelete = { onBlockRemoved(block.id) }
                )
            }
        }
    }

    inner class ImageViewHolder(private val binding: ItemBlockImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            val file = File(block.content)
            binding.ivImage.load(file) { crossfade(true) }
            binding.ivImage.setOnClickListener {
                onAttachmentImageOpen(block.id, block.content, file.name, "image/*")
            }
            binding.icMoreVert.isEnabled = !isReadingMode
            binding.icMoreVert.alpha = if (isReadingMode) 0.5f else 1.0f
            binding.icMoreVert.setOnClickListener { anchor ->
                if (isReadingMode) return@setOnClickListener
                showNoteItemOptions(
                    anchor = anchor,
                    config = NoteItemOptionsConfig(),
                    onShare = { shareBlockFile(anchor, block.content, "image/*") },
                    onView = {
                        onAttachmentImageOpen(block.id, block.content, file.name, "image/*")
                    },
                    onDelete = { onBlockRemoved(block.id) }
                )
            }
        }
    }

    inner class AttachmentImageViewHolder(
        private val binding: ItemAttachmentImageBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            val metadata = AttachmentMetadata.fromBlock(block) ?: return
            val file = File(block.content)
            binding.tvFileName.text = metadata.displayLabel()
            if (file.exists()) {
                binding.ivPreview.visibility = android.view.View.VISIBLE
                binding.tvUnavailable.visibility = android.view.View.GONE
                binding.ivPreview.load(file) { crossfade(true) }
                binding.ivPreview.setOnClickListener {
                    onAttachmentImageOpen(block.id, block.content, metadata.displayLabel(), metadata.mimeType)
                }
            } else {
                binding.ivPreview.visibility = android.view.View.GONE
                binding.tvUnavailable.visibility = android.view.View.VISIBLE
            }
            binding.icMoreVert.isEnabled = !isReadingMode
            binding.icMoreVert.alpha = if (isReadingMode) 0.5f else 1.0f
            binding.icMoreVert.setOnClickListener { anchor ->
                if (isReadingMode) return@setOnClickListener
                showNoteItemOptions(
                    anchor = anchor,
                    config = NoteItemOptionsConfig(),
                    onShare = { shareBlockFile(anchor, block.content, metadata.mimeType) },
                    onView = {
                        if (file.exists()) {
                            onAttachmentImageOpen(block.id, block.content, metadata.displayLabel(), metadata.mimeType)
                        }
                    },
                    onDelete = { onBlockRemoved(block.id) }
                )
            }
        }
    }

    inner class AttachmentVideoViewHolder(
        private val binding: ItemAttachmentVideoBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            val metadata = AttachmentMetadata.fromBlock(block) ?: return
            binding.tvFileName.text = metadata.displayLabel()
            binding.tvDuration.text = formatVoiceDuration(metadata.durationMs)
            val thumb = metadata.thumbnailPath?.let { File(it) }?.takeIf { it.exists() }
            if (thumb != null) {
                binding.ivThumbnail.load(thumb) { crossfade(true) }
            } else {
                binding.ivThumbnail.setImageResource(R.drawable.ic_image)
            }
            binding.root.setOnClickListener {
                onAttachmentVideoOpen(block.id, block.content, metadata.displayLabel(), metadata.mimeType)
            }
            binding.icMoreVert.isEnabled = !isReadingMode
            binding.icMoreVert.alpha = if (isReadingMode) 0.5f else 1.0f
            binding.icMoreVert.setOnClickListener { anchor ->
                if (isReadingMode) return@setOnClickListener
                showNoteItemOptions(
                    anchor = anchor,
                    config = NoteItemOptionsConfig(),
                    onShare = { shareBlockFile(anchor, block.content, metadata.mimeType) },
                    onView = {
                        onAttachmentVideoOpen(block.id, block.content, metadata.displayLabel(), metadata.mimeType)
                    },
                    onDelete = { onBlockRemoved(block.id) }
                )
            }
        }
    }

    inner class AttachmentAudioViewHolder(
        private val binding: ItemAttachmentAudioBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private var isUserSeeking = false
        // Unsaved blocks carry negative temporary ids, so null is the only safe
        // "not bound yet" marker here.
        private var boundBlockId: Long? = null
        private var boundPath: String = ""
        private var boundDurationMs: Long = 0L
        private var boundDisplayName: String? = null

        init {
            binding.btnPlayPause.setOnClickListener {
                val blockId = boundBlockId ?: return@setOnClickListener
                onAttachmentAudioPlayPause(blockId, boundPath, boundDurationMs)
            }
            binding.btnRemove.setOnClickListener {
                val blockId = boundBlockId ?: return@setOnClickListener
                onBlockRemoved(blockId)
            }
            binding.btnRename.setOnClickListener {
                val blockId = boundBlockId ?: return@setOnClickListener
                showRenameDialog(blockId, boundDisplayName)
            }
            binding.tvFileName.setOnClickListener {
                val blockId = boundBlockId ?: return@setOnClickListener
                if (isReadingMode) return@setOnClickListener
                showRenameDialog(blockId, boundDisplayName)
            }
            binding.seekPlayback.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser || boundBlockId == null) return
                    binding.tvPlaybackTime.text = binding.root.context.getString(
                        R.string.voice_playback_time,
                        formatVoiceDuration(progress.toLong()),
                        formatVoiceDuration(boundDurationMs)
                    )
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = false
                    val blockId = boundBlockId ?: return
                    onAttachmentAudioSeek(
                        blockId,
                        boundPath,
                        boundDurationMs,
                        seekBar?.progress?.toLong() ?: 0L
                    )
                }
            })
        }

        fun bind(block: NoteBlock) {
            val metadata = AttachmentMetadata.fromBlock(block) ?: return
            boundBlockId = block.id
            boundPath = block.content
            boundDurationMs = metadata.durationMs
            boundDisplayName = metadata.displayName
            binding.tvFileName.text = metadata.displayLabel()
            binding.btnRemove.visibility = View.VISIBLE
            binding.btnRemove.isEnabled = !isReadingMode
            binding.btnRemove.alpha = if (isReadingMode) 0.5f else 1.0f
            binding.btnRename.visibility = View.VISIBLE
            binding.btnRename.isEnabled = !isReadingMode
            binding.btnRename.alpha = if (isReadingMode) 0.5f else 1.0f
            bindPlayback(block)
        }

        fun bindPlayback(block: NoteBlock) {
            val metadata = AttachmentMetadata.fromBlock(block)
            val playback = attachmentPlaybackState()
            val isActive = playback.activeBlockId == block.id
            val durationMs = if (isActive && playback.duration > 0) {
                playback.duration
            } else {
                metadata?.durationMs ?: boundDurationMs
            }
            boundDurationMs = durationMs
            val positionMs = if (isActive) playback.position else 0L
            val isPlaying = isActive && playback.isPlaying

            binding.tvPlaybackTime.text = binding.root.context.getString(
                R.string.voice_playback_time,
                formatVoiceDuration(positionMs),
                formatVoiceDuration(durationMs)
            )
            binding.seekPlayback.max = durationMs.coerceAtLeast(1).toInt()
            if (!isUserSeeking) {
                binding.seekPlayback.progress = positionMs.coerceIn(0, durationMs).toInt()
            }
            binding.btnPlayPause.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        }

        private fun showRenameDialog(blockId: Long, currentName: String?) {
            val input = android.widget.EditText(binding.root.context).apply {
                setText(currentName.orEmpty())
                hint = binding.root.context.getString(R.string.attachment_name_hint)
                setPadding(48, 32, 48, 32)
            }
            MaterialAlertDialogBuilder(binding.root.context)
                .setTitle(R.string.rename_attachment)
                .setView(input)
                .setPositiveButton(R.string.save) { _, _ ->
                    onAttachmentRename(blockId, input.text.toString().trim())
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    inner class AttachmentDocumentViewHolder(
        private val binding: ItemAttachmentDocumentBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            val metadata = AttachmentMetadata.fromBlock(block) ?: return
            bindFileAttachmentItem(
                binding = binding,
                block = block,
                metadata = metadata
            )
        }
    }

    inner class AttachmentGenericViewHolder(
        private val binding: ItemAttachmentGenericBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            val metadata = AttachmentMetadata.fromBlock(block) ?: return
            bindFileAttachmentItem(
                binding = binding,
                block = block,
                metadata = metadata
            )
        }
    }

    private fun bindFileAttachmentItem(
        binding: ItemAttachmentDocumentBinding,
        block: NoteBlock,
        metadata: AttachmentMetadata
    ) {
        binding.ivFileIcon.setImageResource(
            FileTypeIconResolver.iconFor(metadata.fileName, metadata.mimeType)
        )
        binding.tvFileName.text = metadata.displayLabel()
        binding.tvFileMeta.text = formatAttachmentMeta(metadata.createdAt, metadata.fileSize)
        binding.fileItemContainer.setOnClickListener {
            onAttachmentOpen(block.content, metadata.mimeType)
        }
        binding.ivMore.visibility = View.VISIBLE
        binding.ivMore.isEnabled = !isReadingMode
        binding.ivMore.alpha = if (isReadingMode) 0.5f else 1.0f
        binding.ivMore.setOnClickListener { anchor ->
            if (isReadingMode) return@setOnClickListener
            showNoteItemOptions(
                anchor = anchor,
                config = NoteItemOptionsConfig(),
                onView = { onAttachmentOpen(block.content, metadata.mimeType) },
                onShare = { shareBlockFile(anchor, block.content, metadata.mimeType) },
                onDelete = { onBlockRemoved(block.id) }
            )
        }
    }

    private fun bindFileAttachmentItem(
        binding: ItemAttachmentGenericBinding,
        block: NoteBlock,
        metadata: AttachmentMetadata
    ) {
        binding.ivFileIcon.setImageResource(
            FileTypeIconResolver.iconFor(metadata.fileName, metadata.mimeType)
        )
        binding.tvFileName.text = metadata.displayLabel()
        binding.tvFileMeta.text = formatAttachmentMeta(metadata.createdAt, metadata.fileSize)
        binding.fileItemContainer.setOnClickListener {
            onAttachmentOpen(block.content, metadata.mimeType)
        }
        binding.ivMore.visibility = View.VISIBLE
        binding.ivMore.isEnabled = !isReadingMode
        binding.ivMore.alpha = if (isReadingMode) 0.5f else 1.0f
        binding.ivMore.setOnClickListener { anchor ->
            if (isReadingMode) return@setOnClickListener
            showNoteItemOptions(
                anchor = anchor,
                config = NoteItemOptionsConfig(),
                onView = { onAttachmentOpen(block.content, metadata.mimeType) },
                onShare = { shareBlockFile(anchor, block.content, metadata.mimeType) },
                onDelete = { onBlockRemoved(block.id) }
            )
        }
    }

    inner class LinkViewHolder(private val binding: ItemBlockLinkBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            binding.tvLink.text = block.content
            binding.tvLink.setOnClickListener {
                try {
                    binding.root.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(block.content)))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            binding.ivMore.visibility = View.VISIBLE
            binding.ivMore.isEnabled = !isReadingMode
            binding.ivMore.alpha = if (isReadingMode) 0.5f else 1.0f
            binding.ivMore.setOnClickListener { anchor ->
                if (isReadingMode) return@setOnClickListener
                showNoteItemOptions(
                    anchor = anchor,
                    config = NoteItemOptionsConfig(
                        showView = false,
                        showShare = true,
                        showDelete = true
                    ),
                    onShare = { shareLink(anchor, block.content) },
                    onDelete = { onBlockRemoved(block.id) }
                )
            }
        }
    }

    inner class DrawingViewHolder(private val binding: ItemBlockDrawingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(block: NoteBlock) {
            val file = File(block.content)
            if (file.exists()) {
                binding.ivDrawing.visibility = android.view.View.VISIBLE
                binding.tvDrawingUnavailable.visibility = android.view.View.GONE
                binding.ivDrawing.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                binding.ivDrawing.load(file) { crossfade(true) }
                binding.ivDrawing.setOnClickListener {
                    onAttachmentImageOpen(block.id, block.content, file.name, "image/png")
                }
            } else {
                binding.ivDrawing.visibility = android.view.View.GONE
                binding.tvDrawingUnavailable.visibility = android.view.View.VISIBLE
            }
            binding.btnRemove.visibility = View.VISIBLE
            binding.btnRemove.isEnabled = !isReadingMode
            binding.btnRemove.alpha = if (isReadingMode) 0.5f else 1.0f
            binding.btnRemove.setOnClickListener { onBlockRemoved(block.id) }
        }
    }

    inner class VoiceViewHolder(private val binding: ItemBlockVoiceBinding) : RecyclerView.ViewHolder(binding.root) {
        // Unsaved blocks carry negative temporary ids, so null is the only safe
        // "not bound yet" marker here.
        private var boundBlockId: Long? = null
        private var boundPath: String = ""
        private var boundDurationMs: Long = 0L
        private var isUserSeeking = false

        init {
            binding.btnPlayPause.setOnClickListener {
                val blockId = boundBlockId ?: return@setOnClickListener
                onVoicePlayPause(blockId, boundPath, boundDurationMs)
            }
            binding.waveformView.onWaveformClick = {
                boundBlockId?.let { blockId ->
                    onVoicePlayPause(blockId, boundPath, boundDurationMs)
                }
            }
            binding.waveformView.onSeekProgress = { _ ->
                if (boundBlockId != null && boundDurationMs > 0L) {
                    isUserSeeking = true
                }
            }
            binding.waveformView.onSeekFinished = { fraction ->
                boundBlockId?.let { blockId ->
                    val positionMs = (boundDurationMs * fraction).toLong().coerceAtLeast(0L)
                    isUserSeeking = false
                    onVoiceSeek(blockId, positionMs)
                }
            }
        }

        fun bind(block: NoteBlock) {
            val metadata = VoiceNoteMetadata.fromJson(block.extraData)
            boundBlockId = block.id
            boundPath = block.content
            boundDurationMs = metadata.durationMs
            val waveform = metadata.waveform.ifEmpty {
                VoiceWaveformHelper.fallbackWaveform(block.content).toList()
            }
            binding.waveformView.setWaveform(waveform)
            binding.ivMore.visibility = View.VISIBLE
            binding.ivMore.isEnabled = !isReadingMode
            binding.ivMore.alpha = if (isReadingMode) 0.5f else 1.0f
            binding.ivMore.setOnClickListener { anchor ->
                if (isReadingMode) return@setOnClickListener
                showNoteItemOptions(
                    anchor = anchor,
                    config = NoteItemOptionsConfig(
                        showView = false,
                        showShare = true,
                        showDelete = true
                    ),
                    onShare = { shareBlockFile(anchor, block.content, "audio/mp4") },
                    onDelete = { onBlockRemoved(block.id) }
                )
            }
            bindPlayback(block)
        }

        fun bindPlayback(block: NoteBlock) {
            val metadata = VoiceNoteMetadata.fromJson(block.extraData)
            val playback = voicePlaybackState()
            val isActive = playback.activeBlockId == block.id
            val isPlaying = isActive && playback.isPlaying
            val durationMs = if (isActive && playback.duration > 0) {
                playback.duration
            } else {
                metadata.durationMs
            }
            boundDurationMs = durationMs.coerceAtLeast(1L)
            val positionMs = if (isActive) playback.position else 0L
            if (!isUserSeeking) {
                if (isActive && durationMs > 0) {
                    binding.waveformView.setPlaybackProgress(positionMs.toFloat() / durationMs)
                } else {
                    binding.waveformView.clearPlaybackProgress()
                }
            }
            binding.btnPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause_white else R.drawable.ic_play_white
            )
            binding.btnPlayPause.contentDescription = binding.root.context.getString(
                if (isPlaying) R.string.pause else R.string.play
            )
        }
    }

    private fun showNoteItemOptions(
        anchor: android.view.View,
        config: NoteItemOptionsConfig,
        onShare: (() -> Unit)? = null,
        onView: (() -> Unit)? = null,
        onDelete: (() -> Unit)? = null
    ) {
        NoteItemOptionsPopup.show(
            anchor = anchor,
            config = config,
            listener = NoteItemOptionsListener(
                onShare = onShare,
                onView = onView,
                onDelete = onDelete
            )
        )
    }

    private fun shareBlockFile(anchor: android.view.View, path: String, mimeType: String) {
        AttachmentIntentHelper.shareFile(anchor.context, path, mimeType).onFailure {
            Toast.makeText(
                anchor.context,
                R.string.attachment_missing,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun shareLink(anchor: android.view.View, url: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        anchor.context.startActivity(
            Intent.createChooser(shareIntent, anchor.context.getString(R.string.share))
        )
    }

    data class VoicePlaybackState(
        val activeBlockId: Long? = null,
        val isPlaying: Boolean = false,
        val position: Long = 0,
        val duration: Long = 0
    )

    private class ContentDiffCallback : DiffUtil.ItemCallback<NoteContentItem>() {
        override fun areItemsTheSame(oldItem: NoteContentItem, newItem: NoteContentItem) =
            oldItem.stableId == newItem.stableId

        override fun areContentsTheSame(oldItem: NoteContentItem, newItem: NoteContentItem): Boolean {
            // Live rich-text editors own content. Rebinding after flushContent steals
            // focus/IME after formatting actions — compare metadata only for those types.
            if (oldItem is NoteContentItem.BlockItem && newItem is NoteContentItem.BlockItem) {
                val oldBlock = oldItem.block
                val newBlock = newItem.block
                if (oldBlock.type != newBlock.type) return false
                return when (oldBlock.type) {
                    BlockType.TEXT,
                    BlockType.BULLET_LIST,
                    BlockType.NUMBER_LIST,
                    BlockType.CHECKBOX ->
                        // Ignore content / extraData text — live editors own the body.
                        // Comparing them rebinds on every flush and steals IME focus.
                        oldBlock.id == newBlock.id &&
                            oldBlock.position == newBlock.position &&
                            oldBlock.checked == newBlock.checked &&
                            oldBlock.type == newBlock.type
                    else -> oldBlock == newBlock
                }
            }
            if (oldItem is NoteContentItem.TodoEntry && newItem is NoteContentItem.TodoEntry) {
                val oldTodo = oldItem.item
                val newTodo = newItem.item
                return oldTodo.id == newTodo.id &&
                    oldTodo.isCompleted == newTodo.isCompleted &&
                    oldTodo.position == newTodo.position
            }
            return oldItem == newItem
        }
    }

    companion object {
        private const val VIEW_TEXT = 0
        private const val VIEW_TODO = 1
        private const val VIEW_ADD_TODO_FOOTER = 9
        private const val VIEW_IMAGE = 2
        private const val VIEW_LINK = 4
        private const val VIEW_DRAWING = 5
        private const val VIEW_VOICE = 6
        private const val VIEW_NUMBER_LIST = 7
        private const val VIEW_BULLET_LIST = 8
        private const val VIEW_DIVIDER = 10
        private const val VIEW_MEDIA_ROW = 12
        private const val PAYLOAD_PLAYBACK = "playback"
    }
}
