package com.notebook.notes.checklist.ui.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.tabs.TabLayout
import com.notebook.notes.checklist.ads.AdRemoteConfig
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.loadScreenAd
import com.notebook.notes.checklist.ads.other_screen
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityNoteBackgroundPickerBinding
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.model.NoteBgCategory
import com.notebook.notes.checklist.ui.background.NoteBackgroundAdapter
import com.notebook.notes.checklist.ui.background.NoteBackgroundRepository
import com.notebook.notes.checklist.utils.Constant
import com.notebook.notes.checklist.viewmodel.utils.finishWithNoteBackgroundPickerBackInterAd
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NoteBackgroundPickerActivity : BaseActivity() {

    @Inject
    lateinit var preferenceHelper: PreferenceHelper

    private lateinit var binding: ActivityNoteBackgroundPickerBinding
    private lateinit var repository: NoteBackgroundRepository
    private lateinit var adapter: NoteBackgroundAdapter
    private var categories: List<NoteBgCategory> = emptyList()
    private var selectedKey: String? = null
    private var saveAsDefault = false
    private var suppressTabCallback = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteBackgroundPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        repository = NoteBackgroundRepository(this)
        saveAsDefault = intent.getBooleanExtra(EXTRA_SAVE_AS_DEFAULT, false)
        selectedKey = intent.getStringExtra(EXTRA_SELECTED_KEY)
            ?.takeIf { it.isNotBlank() }
            ?: if (saveAsDefault) {
                preferenceHelper.getString(Constant.PREF_DEFAULT_NOTE_BACKGROUND)
                    ?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        categories = repository.getCategories()

        adapter = NoteBackgroundAdapter(repository) { option ->
            selectedKey = option.key
            adapter.setSelectedKey(selectedKey)
            // Same as NoteBackgroundPickerDialog callback used by Settings:
            // persist default bg so new notes open with this theme.
            if (saveAsDefault) {
                preferenceHelper.putString(Constant.PREF_DEFAULT_NOTE_BACKGROUND, option.key)
            }
            setResult(
                Activity.RESULT_OK,
                Intent().putExtra(EXTRA_RESULT_KEY, option.key)
            )
            finishWithNoteBackgroundPickerBackInterAd()
        }
        adapter.setSelectedKey(selectedKey)

        binding.rvBackgrounds.layoutManager = GridLayoutManager(this, GRID_SPAN)
        binding.rvBackgrounds.adapter = adapter
        setupTabs()
        binding.btnBack.isVisible = true
        binding.btnBack.setOnClickListener { finishWithNoteBackgroundPickerBackInterAd() }
        setupBackPress()
        loadOtherScreenAds()
    }

    override fun onResume() {
        super.onResume()
        if (::adapter.isInitialized) {
            adapter.refreshAccessState()
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithNoteBackgroundPickerBackInterAd()
            }
        })
    }

    private fun loadOtherScreenAds() {
        AdsManager.loadScreenAd(this, AdRemoteConfig.other_screen)
    }


    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val topPad = systemBars.top + (12 * resources.displayMetrics.density).toInt()
            binding.clTopbar.updatePadding(top = topPad)
            binding.rvBackgrounds.updatePadding(bottom = systemBars.bottom)
            insets
        }
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
        val options = repository.getOptionsForCategory(categoryId)
        adapter.submitList(options) {
            val selectedIndex = adapter.indexOfKey(selectedKey)
            if (selectedIndex >= 0) {
                binding.rvBackgrounds.scrollToPosition(selectedIndex)
            }
        }
    }

    companion object {
        const val EXTRA_SELECTED_KEY = "selected_key"
        const val EXTRA_RESULT_KEY = "result_key"
        const val EXTRA_SAVE_AS_DEFAULT = "save_as_default"
        private const val GRID_SPAN = 4

        fun createIntent(
            context: Context,
            selectedKey: String?,
            saveAsDefault: Boolean = false
        ): Intent {
            return Intent(context, NoteBackgroundPickerActivity::class.java).apply {
                putExtra(EXTRA_SELECTED_KEY, selectedKey)
                putExtra(EXTRA_SAVE_AS_DEFAULT, saveAsDefault)
            }
        }
    }
}
