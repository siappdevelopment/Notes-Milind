package com.notebook.notes.checklist.ui.background

import android.app.Activity
import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.notebook.notes.checklist.databinding.PanelNoteBackgroundBinding
import com.notebook.notes.checklist.model.NoteBgCategory
import com.notebook.notes.checklist.ui.richtext.EditorPanelAnimator

class NoteBackgroundPanel(
    private val binding: PanelNoteBackgroundBinding,
    private val repository: NoteBackgroundRepository,
    private val onBackgroundSelected: (String) -> Unit,
    private val onVisibilityChanged: (Boolean) -> Unit
) {
    private lateinit var adapter: NoteBackgroundAdapter
    private var categories: List<NoteBgCategory> = emptyList()
    private var selectedKey: String? = null
    private var suppressTabCallback = false
    private var tabsConfigured = false

    val isVisible: Boolean
        get() = binding.root.isVisible

    init {
        adapter = NoteBackgroundAdapter(repository) { option ->
            selectedKey = option.key
            adapter.setSelectedKey(selectedKey)
            onBackgroundSelected(option.key)
        }
        binding.rvBackgrounds.layoutManager = GridLayoutManager(binding.root.context, GRID_SPAN)
        binding.rvBackgrounds.adapter = adapter
        binding.btnClose.setOnClickListener { hide() }
        binding.root.visibility = View.GONE
    }

    fun show(selectedKey: String?, animate: Boolean = true) {
        this.selectedKey = selectedKey
        ensureTabs()
        adapter.setSelectedKey(selectedKey)
        selectCategoryForKey(selectedKey)
        adapter.refreshAccessState()

        val activity = binding.root.context as? Activity ?: run {
            EditorPanelAnimator.show(binding.root, animate = animate) {
                onVisibilityChanged(true)
            }
            return
        }
        EditorPanelAnimator.hideKeyboardThen(activity, binding.root) {
            EditorPanelAnimator.show(binding.root, animate = animate) {
                onVisibilityChanged(true)
            }
        }
    }

    fun hide(animate: Boolean = true) {
        if (!isVisible) return
        EditorPanelAnimator.hide(binding.root, animate = animate) {
            onVisibilityChanged(false)
        }
    }

    fun setSelectedKey(key: String?) {
        selectedKey = key
        if (::adapter.isInitialized) {
            adapter.setSelectedKey(key)
        }
    }

    /** Call when returning from Paywall so lock overlays clear immediately. */
    fun refreshAccessState() {
        if (::adapter.isInitialized) {
            adapter.refreshAccessState()
        }
    }

    private fun ensureTabs() {
        if (tabsConfigured) return
        categories = repository.getCategories()
        binding.tabCategories.removeAllTabs()
        categories.forEach { category ->
            binding.tabCategories.addTab(
                binding.tabCategories.newTab().setText(repository.localizedLabel(category))
            )
        }
        binding.tabCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (suppressTabCallback) return
                val category = categories.getOrNull(tab.position) ?: return
                showCategory(category.id)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
        tabsConfigured = true
    }

    private fun selectCategoryForKey(key: String?) {
        if (categories.isEmpty()) return
        val initialIndex = categories.indexOfFirst { category ->
            repository.findCategoryForKey(key)?.id == category.id
        }.takeIf { it >= 0 } ?: 0

        suppressTabCallback = true
        binding.tabCategories.getTabAt(initialIndex)?.select()
        suppressTabCallback = false
        showCategory(categories[initialIndex].id)
    }

    private fun showCategory(categoryId: String) {
        adapter.submitList(repository.getOptionsForCategory(categoryId)) {
            adapter.setSelectedKey(selectedKey)
        }
    }

    companion object {
        private const val GRID_SPAN = 4
    }
}
