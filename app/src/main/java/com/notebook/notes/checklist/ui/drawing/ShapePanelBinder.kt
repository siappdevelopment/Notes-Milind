package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PanelStickyNotesBinding
import com.notebook.notes.checklist.model.TapeCategory
import com.notebook.notes.checklist.model.TapeResource
import kotlinx.coroutines.CoroutineScope

class ShapePanelBinder(
    private val context: Context,
    private val binding: PanelStickyNotesBinding,
    private val scope: CoroutineScope,
    private val onClose: () -> Unit,
    private val onShapeSelected: (TapeResource) -> Unit
) {
    private val tapeRepository = TapeConfigRepository(context)
    private lateinit var categoryAdapter: TapeCategoryAdapter
    private lateinit var thumbnailAdapter: TapeThumbnailAdapter
    private val failedTapeIds = mutableSetOf<Int>()
    private var currentCategoryId: String? = null

    fun bind() {
        binding.tvTitle.setText(R.string.shape)
        binding.btnClose.setOnClickListener { onClose() }
        setupCategoryTabs()
        setupRecyclerView()
        showCategory(tapeRepository.getCategories().firstOrNull(), applyFirstItem = false)
    }

    private fun setupCategoryTabs() {
        categoryAdapter = TapeCategoryAdapter(scope) { category ->
            showCategory(category, applyFirstItem = false)
        }
        categoryAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.rvStickyCategories.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            itemAnimator = null
        }
        categoryAdapter.submitList(tapeRepository.getCategories())
    }

    private fun setupRecyclerView() {
        thumbnailAdapter = TapeThumbnailAdapter(
            scope = scope,
            onTapeFailed = { id ->
                if (failedTapeIds.add(id)) {
                    refreshCurrentCategory()
                }
            }
        ) { resource ->
            thumbnailAdapter.setSelectedResource(resource.id)
            onShapeSelected(resource)
        }
        thumbnailAdapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        binding.rvStickyNotes.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = thumbnailAdapter
            itemAnimator = null
        }
    }

    private fun showCategory(category: TapeCategory?, applyFirstItem: Boolean) {
        if (category == null) return
        currentCategoryId = category.id
        categoryAdapter.setSelectedCategory(category.id)
        refreshCurrentCategory(applyFirstItem)
    }

    private fun refreshCurrentCategory(applyFirstItem: Boolean = false) {
        val categoryId = currentCategoryId ?: return
        val resources = tapeRepository.getResourcesForCategory(categoryId)
            .filter { it.id !in failedTapeIds }

        thumbnailAdapter.submitList(resources) {
            if (resources.isNotEmpty() && applyFirstItem) {
                val firstResource = resources.first()
                thumbnailAdapter.setSelectedResource(firstResource.id)
                binding.rvStickyNotes.scrollToPosition(0)
                onShapeSelected(firstResource)
            }
        }
    }
}
