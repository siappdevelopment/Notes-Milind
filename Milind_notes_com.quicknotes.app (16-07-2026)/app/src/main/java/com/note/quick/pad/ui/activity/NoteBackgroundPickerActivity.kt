package com.note.quick.pad.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.note.quick.pad.databinding.ActivityNoteBackgroundPickerBinding
import com.note.quick.pad.model.NoteBgCategory
import com.note.quick.pad.ui.background.NoteBackgroundAdapter
import com.note.quick.pad.ui.background.NoteBackgroundRepository

class NoteBackgroundPickerActivity : BaseActivity() {

    private lateinit var binding: ActivityNoteBackgroundPickerBinding
    private lateinit var repository: NoteBackgroundRepository
    private lateinit var adapter: NoteBackgroundAdapter
    private var categories: List<NoteBgCategory> = emptyList()
    private var selectedKey: String? = null
    private var suppressTabCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBackgroundPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = NoteBackgroundRepository(this)
        selectedKey = intent.getStringExtra(EXTRA_SELECTED_KEY)
        categories = repository.getCategories()

        adapter = NoteBackgroundAdapter(repository) { option ->
            selectedKey = option.key
            adapter.setSelectedKey(selectedKey)
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(EXTRA_RESULT_KEY, option.key)
            )
            finish()
        }
        adapter.setSelectedKey(selectedKey)

        binding.rvBackgrounds.layoutManager = GridLayoutManager(this, GRID_SPAN)
        binding.rvBackgrounds.adapter = adapter

        setupTabs()
        binding.btnBack.isVisible = true
        binding.btnBack.setOnClickListener { finish() }
    }

    private fun setupTabs() {
        binding.tabCategories.removeAllTabs()
        categories.forEach { category ->
            binding.tabCategories.addTab(
                binding.tabCategories.newTab().setText(repository.localizedLabel(category))
            )
        }

        val initialIndex = categories.indexOfFirst { category ->
            repository.findCategoryForKey(selectedKey)?.id == category.id
        }.takeIf { it >= 0 } ?: 0

        suppressTabCallback = true
        binding.tabCategories.getTabAt(initialIndex)?.select()
        suppressTabCallback = false
        showCategory(categories[initialIndex].id)

        binding.tabCategories.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (suppressTabCallback) return
                val category = categories.getOrNull(tab.position) ?: return
                showCategory(category.id)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) = Unit
            override fun onTabReselected(tab: TabLayout.Tab) = Unit
        })
    }

    private fun showCategory(categoryId: String) {
        adapter.submitList(repository.getOptionsForCategory(categoryId))
    }

    companion object {
        const val EXTRA_SELECTED_KEY = "selected_key"
        const val EXTRA_RESULT_KEY = "result_key"
        private const val GRID_SPAN = 4

        fun createIntent(context: Context, selectedKey: String?): Intent {
            return Intent(context, NoteBackgroundPickerActivity::class.java).apply {
                putExtra(EXTRA_SELECTED_KEY, selectedKey)
            }
        }
    }
}
