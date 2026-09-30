package com.notebook.notes.checklist.ui.drawing

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.notebook.notes.checklist.databinding.PanelDoodlePickerBinding
import com.notebook.notes.checklist.model.BgCategory
import com.notebook.notes.checklist.model.BgResource
import kotlinx.coroutines.CoroutineScope

class DoodlePanelBinder(
    private val context: Context,
    private val binding: PanelDoodlePickerBinding,
    private val scope: CoroutineScope,
    private val onClose: () -> Unit,
    private val onDoodleApplied: (String) -> Unit
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

    fun refreshPreview() {
        binding.ivPreview.post {
            val resource = selectedResource ?: firstAvailableResource()
            resource?.let { showPreview(it) }
        }
    }

    private fun firstAvailableResource(): BgResource? {
        val category = bgRepository.getCategories().firstOrNull() ?: return null
        return bgRepository.getResourcesForCategory(category.id).firstOrNull()
    }

    private fun setupTabs() {
        binding.tabDoodleCategories.clearOnTabSelectedListeners()
        binding.tabDoodleCategories.removeAllTabs()
        bgRepository.getCategories().forEach { category ->
            binding.tabDoodleCategories.addTab(
                binding.tabDoodleCategories.newTab().setText(bgRepository.localizedLabel(category))
            )
        }
        binding.tabDoodleCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
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
                applyDoodle(resource)
            }
        )
        binding.rvDoodles.apply {
            layoutManager = GridLayoutManager(context, 4)
            adapter = thumbnailAdapter
            itemAnimator = null
        }
    }

    private fun showCategory(category: BgCategory?, applyFirstItem: Boolean) {
        if (category == null) return
        val index = bgRepository.getCategories().indexOfFirst { it.id == category.id }
        if (index >= 0 && binding.tabDoodleCategories.selectedTabPosition != index) {
            binding.tabDoodleCategories.getTabAt(index)?.select()
        }
        val resources = bgRepository.getResourcesForCategory(category.id)
        thumbnailAdapter.submitList(resources) {
            val firstResource = resources.firstOrNull() ?: return@submitList
            selectedResource = firstResource
            thumbnailAdapter.setSelectedResource(firstResource.id)
            binding.rvDoodles.scrollToPosition(0)
            showPreview(firstResource)
            if (applyFirstItem) {
                applyDoodle(firstResource)
            }
        }
    }

    private fun toggleOrientation() {
        isLandscapePreview = !isLandscapePreview
        selectedResource?.let { showPreview(it) }
        thumbnailAdapter.notifyDataSetChanged()
    }

    private fun showPreview(resource: BgResource) {
        val fileName = bgRepository.thumbFileName(resource, isLandscapePreview)
        binding.ivPreview.loadPreviewAsset(fileName, scope)
        Log.d(
            "DoodlePanelBinder",
            "showPreview resourceId=${resource.id} file=$fileName landscape=$isLandscapePreview"
        )
    }

    private fun applyDoodle(resource: BgResource) {
        val fileName = bgRepository.backgroundFileName(resource, isLandscapePreview)
        Log.d(
            "DoodlePanelBinder",
            "applyDoodle resourceId=${resource.id} file=$fileName landscape=$isLandscapePreview"
        )
        onDoodleApplied(fileName)
    }
}
