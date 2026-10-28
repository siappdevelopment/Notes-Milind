package com.notebook.notes.checklist.ui.background

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.notebook.notes.checklist.databinding.DialogNoteBackgroundBinding
import com.notebook.notes.checklist.model.NoteBgCategory
import com.notebook.notes.checklist.util.SystemBars

class NoteBackgroundPickerDialog : BottomSheetDialogFragment() {

    private var _binding: DialogNoteBackgroundBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: NoteBackgroundRepository
    private lateinit var adapter: NoteBackgroundAdapter
    private var categories: List<NoteBgCategory> = emptyList()
    private var selectedKey: String? = null
    private var onBackgroundSelected: ((String) -> Unit)? = null
    private var suppressTabCallback = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogNoteBackgroundBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = NoteBackgroundRepository(requireContext())
        selectedKey = arguments?.getString(ARG_SELECTED_KEY)
        categories = repository.getCategories()

        adapter = NoteBackgroundAdapter(repository) { option ->
            selectedKey = option.key
            adapter.setSelectedKey(selectedKey)
            onBackgroundSelected?.invoke(option.key)
            dismissAllowingStateLoss()
        }
        adapter.setSelectedKey(selectedKey)

        binding.rvBackgrounds.layoutManager = GridLayoutManager(requireContext(), GRID_SPAN)
        binding.rvBackgrounds.adapter = adapter

        setupTabs()
        binding.btnClose.isVisible = true
        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }

        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
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
        adapter.submitList(repository.getOptionsForCategory(categoryId))
    }

    override fun onStart() {
        super.onStart()
        SystemBars.onDialogFragmentStarted(dialog)
        if (::adapter.isInitialized) {
            adapter.refreshAccessState()
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        SystemBars.onDialogFragmentDismissed(activity?.window)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "NoteBackgroundPickerDialog"
        private const val ARG_SELECTED_KEY = "selected_key"
        private const val GRID_SPAN = 4

        fun show(
            fragmentManager: FragmentManager,
            selectedKey: String?,
            onSelected: (String) -> Unit
        ) {
            (fragmentManager.findFragmentByTag(TAG) as? NoteBackgroundPickerDialog)?.dismissAllowingStateLoss()
            NoteBackgroundPickerDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_SELECTED_KEY, selectedKey)
                }
                onBackgroundSelected = onSelected
            }.show(fragmentManager, TAG)
        }
    }
}
