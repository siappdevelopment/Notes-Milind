package com.note.quick.pad.ui.activity

import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.note.quick.pad.R
import com.note.quick.pad.attachment.AttachmentIntentHelper
import com.note.quick.pad.databinding.ActivityImageViewerBinding
import com.note.quick.pad.model.MediaGalleryHelper
import com.note.quick.pad.model.MediaViewerItem
import com.note.quick.pad.ui.adapter.ImageViewerPagerAdapter
import com.note.quick.pad.ui.home.NotesSelectionDialogs
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private lateinit var items: MutableList<MediaViewerItem>
    private var currentIndex = 0
    private lateinit var pagerAdapter: ImageViewerPagerAdapter
    private var isSharing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        items = loadItems().toMutableList()
        if (items.isEmpty()) {
            finish()
            return
        }
        currentIndex = intent.getIntExtra(EXTRA_START_INDEX, 0).coerceIn(0, items.lastIndex)

        setupWindowInsets()
        setupPager()
        setupActions()
        updateCounter()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = systemBars.top)
            binding.bottomBar.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupPager() {
        pagerAdapter = ImageViewerPagerAdapter(items)
        binding.mediaPager.adapter = pagerAdapter
        binding.mediaPager.offscreenPageLimit = 1
        binding.mediaPager.setCurrentItem(currentIndex, false)
        binding.mediaPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentIndex = position
                updateCounter()
            }
        })
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener { finish() }

        binding.actionDelete.setOnClickListener { confirmDeleteCurrent() }
        binding.actionSave.setOnClickListener { saveCurrentToAlbum() }
        binding.actionShare.setOnClickListener { shareCurrent() }
    }

    private fun updateCounter() {
        binding.tvCounter.text = getString(
            R.string.image_viewer_counter,
            currentIndex + 1,
            items.size
        )
    }

    private fun currentItem(): MediaViewerItem? = items.getOrNull(currentIndex)

    private fun confirmDeleteCurrent() {
        val item = currentItem() ?: return
        NotesSelectionDialogs.showConfirmation(
            context = this,
            layoutInflater = layoutInflater,
            titleRes = R.string.delete,
            messageRes = R.string.delete_media_confirm,
            positiveButtonRes = R.string.delete,
            iconRes = R.drawable.ic_recylerbin,
            onPositiveClick = { deleteCurrent(item) }
        )
    }

    private fun deleteCurrent(item: MediaViewerItem) {
        setResult(
            RESULT_OK,
            Intent().putExtra(EXTRA_DELETED_BLOCK_ID, item.blockId)
        )
        finish()
    }

    private fun shareCurrent() {
        if (isSharing) return
        val item = currentItem() ?: return
        isSharing = true
        AttachmentIntentHelper.shareFile(this, item.path, item.mimeType).onFailure {
            isSharing = false
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        isSharing = false
    }

    private fun saveCurrentToAlbum() {
        val item = currentItem() ?: return
        val source = File(item.path)
        if (!source.exists()) {
            Toast.makeText(this, R.string.attachment_missing, Toast.LENGTH_SHORT).show()
            return
        }
        runCatching {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, source.name)
                put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_PICTURES}/QuickNotes"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = contentResolver.insert(collection, values)
                ?: error("insert_failed")
            contentResolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("write_failed")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            }
        }.onSuccess {
            Toast.makeText(this, R.string.saved_to_album, Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(this, R.string.save_to_album_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadItems(): List<MediaViewerItem> {
        intent.getStringExtra(EXTRA_ITEMS_JSON)?.let { json ->
            val type = object : TypeToken<List<MediaViewerItem>>() {}.type
            return Gson().fromJson<List<MediaViewerItem>>(json, type).orEmpty()
        }
        val path = intent.getStringExtra(EXTRA_IMAGE_PATH).orEmpty()
        if (path.isBlank()) return emptyList()
        return listOf(
            MediaViewerItem(
                blockId = intent.getLongExtra(EXTRA_BLOCK_ID, -1L),
                path = path,
                title = intent.getStringExtra(EXTRA_IMAGE_TITLE).orEmpty(),
                mimeType = intent.getStringExtra(EXTRA_MIME_TYPE).orEmpty()
            )
        )
    }

    companion object {
        const val EXTRA_ITEMS_JSON = "extra_items_json"
        const val EXTRA_START_INDEX = "extra_start_index"
        const val EXTRA_DELETED_BLOCK_ID = "extra_deleted_block_id"

        const val EXTRA_IMAGE_PATH = "extra_image_path"
        const val EXTRA_IMAGE_TITLE = "extra_image_title"
        const val EXTRA_MIME_TYPE = "extra_mime_type"
        const val EXTRA_BLOCK_ID = "extra_block_id"

        fun createIntent(
            context: android.content.Context,
            items: List<MediaViewerItem>,
            startIndex: Int
        ): Intent = Intent(context, ImageViewerActivity::class.java).apply {
            putExtra(EXTRA_ITEMS_JSON, Gson().toJson(items))
            putExtra(EXTRA_START_INDEX, startIndex)
        }

        fun createIntent(
            context: android.content.Context,
            path: String,
            title: String,
            mimeType: String,
            blockId: Long = -1L
        ): Intent = createIntent(
            context = context,
            items = listOf(
                MediaViewerItem(
                    blockId = blockId,
                    path = path,
                    title = title,
                    mimeType = mimeType
                )
            ),
            startIndex = 0
        )

        fun collectFromBlocks(
            blocks: List<com.note.quick.pad.model.NoteBlock>,
            selectedPath: String
        ): Pair<List<MediaViewerItem>, Int> {
            val items = MediaGalleryHelper.collectViewableItems(blocks)
            val index = MediaGalleryHelper.indexOfPath(items, selectedPath)
            return items to index
        }
    }
}
