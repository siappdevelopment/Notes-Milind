package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.notebook.notes.checklist.databinding.PanelBgPickerBinding
import com.notebook.notes.checklist.model.BgCategory
import com.notebook.notes.checklist.model.BgResource
import kotlinx.coroutines.CoroutineScope

class BgPanelBinder(
    private val context: Context,
    private val binding: PanelBgPickerBinding,
    private val scope: CoroutineScope,
    private val onClose: () -> Unit,
    private val onBackgroundApplied: (String) -> Unit
) {
    private val bgRepository = BgConfigRepository(context)
    private lateinit var thumbnailAdapter: BgThumbnailAdapter
    private var isLandscapePreview = false
    private var selectedResource: BgResource? = null

    fun bind() {
        binding.btnClose.setOnClickListener { onClose() }
        binding.btnFullScreen.setOnClickListener { toggleOrientation() }
        setupTabs()
        setupRecyclerView()
        showCategory(bgRepository.getCategories().firstOrNull(), applyFirstItem = false)
    }

    private fun setupTabs() {
        binding.tabBgCategories.clearOnTabSelectedListeners()
        binding.tabBgCategories.removeAllTabs()
        bgRepository.getCategories().forEach { category ->
            binding.tabBgCategories.addTab(
                binding.tabBgCategories.newTab().setText(bgRepository.localizedLabel(category))
            )
        }
        binding.tabBgCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                bgRepository.getCategories().getOrNull(tab.position)?.let { category ->
                    showCategory(category, applyFirstItem = true)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit

            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun setupRecyclerView() {
        thumbnailAdapter = BgThumbnailAdapter(
            scope = scope,
            thumbResolver = { resource ->
                bgRepository.thumbFileName(resource, isLandscapePreview)
            },
            onBackgroundSelected = { resource ->
                selectedResource = resource
                thumbnailAdapter.setSelectedResource(resource.id)
                showPreview(resource)
                applyBackground(resource)
            }
        )
        binding.rvBackgrounds.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = thumbnailAdapter
            itemAnimator = null
        }
    }

    private fun showCategory(category: BgCategory?, applyFirstItem: Boolean) {
        if (category == null) return
        val index = bgRepository.getCategories().indexOfFirst { it.id == category.id }
        if (index >= 0 && binding.tabBgCategories.selectedTabPosition != index) {
            binding.tabBgCategories.getTabAt(index)?.select()
        }
        val resources = bgRepository.getResourcesForCategory(category.id)
        thumbnailAdapter.submitList(resources) {
            val firstResource = resources.firstOrNull() ?: return@submitList
            selectedResource = firstResource
            thumbnailAdapter.setSelectedResource(firstResource.id)
            binding.rvBackgrounds.scrollToPosition(0)
            showPreview(firstResource)
            if (applyFirstItem) {
                applyBackground(firstResource)
            }
        }
    }

    private fun toggleOrientation() {
        isLandscapePreview = !isLandscapePreview
        selectedResource?.let { showPreview(it) }
        thumbnailAdapter.notifyDataSetChanged()
    }

    private fun showPreview(resource: BgResource) {
        val fileName = bgRepository.backgroundFileName(resource, isLandscapePreview)
        binding.ivPreview.loadBackgroundAsset(fileName, scope)
    }

    private fun applyBackground(resource: BgResource) {
        val fileName = bgRepository.backgroundFileName(resource, isLandscapePreview)
        onBackgroundApplied(fileName)
    }
}
