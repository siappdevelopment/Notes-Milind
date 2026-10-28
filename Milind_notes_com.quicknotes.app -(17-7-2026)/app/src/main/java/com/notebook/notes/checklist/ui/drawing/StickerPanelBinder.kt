package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PanelStickyNotesBinding
import com.notebook.notes.checklist.model.StickerCategory
import com.notebook.notes.checklist.model.StickerResource
import kotlinx.coroutines.CoroutineScope

class StickerPanelBinder(
    private val context: Context,
    private val binding: PanelStickyNotesBinding,
    private val scope: CoroutineScope,
    private val onClose: () -> Unit,
    private val onStickerSelected: (StickerResource) -> Unit
) {
    private val stickerRepository = StickerConfigRepository(context)
    private lateinit var categoryAdapter: StickerCategoryAdapter
    private lateinit var thumbnailAdapter: StickerThumbnailAdapter
    private val failedStickerIds = mutableSetOf<Int>()
    private var currentCategoryId: String? = null

    fun bind() {
        binding.tvTitle.setText(R.string.sticker)
        binding.btnClose.setOnClickListener { onClose() }
        setupCategoryTabs()
        setupRecyclerView()
        showCategory(stickerRepository.getCategories().firstOrNull(), applyFirstItem = false)
    }

    private fun setupCategoryTabs() {
        categoryAdapter = StickerCategoryAdapter(
            scope = scope,
            repository = stickerRepository
        ) { category ->
            showCategory(category, applyFirstItem = false)
        }
        categoryAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.rvStickyCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(12)
        }
        categoryAdapter.submitList(stickerRepository.getCategories())
    }

    private fun setupRecyclerView() {
        thumbnailAdapter = StickerThumbnailAdapter(
            scope = scope,
            repository = stickerRepository,
            onStickerFailed = { id ->
                if (failedStickerIds.add(id)) {
                    refreshCurrentCategory()
                }
            }
        ) { resource ->
            thumbnailAdapter.setSelectedResource(resource.id)
            onStickerSelected(resource)
        }
        thumbnailAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.rvStickyNotes.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = thumbnailAdapter
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(24)
        }
    }

    private fun showCategory(category: StickerCategory?, applyFirstItem: Boolean) {
        if (category == null) return
        currentCategoryId = category.id
        categoryAdapter.setSelectedCategory(category.id)
        refreshCurrentCategory(applyFirstItem)
    }

    private fun refreshCurrentCategory(applyFirstItem: Boolean = false) {
        val categoryId = currentCategoryId ?: return
        val resources = stickerRepository.getResourcesForCategory(categoryId)
            .filter { it.id !in failedStickerIds }
        
        thumbnailAdapter.submitList(resources) {
            if (resources.isNotEmpty() && applyFirstItem) {
                val firstResource = resources.first()
                thumbnailAdapter.setSelectedResource(firstResource.id)
                binding.rvStickyNotes.scrollToPosition(0)
                onStickerSelected(firstResource)
            }
        }
    }
}
