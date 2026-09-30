package com.notebook.notes.checklist.ui.emoji

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.databinding.BottomSheetEmojiBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EmojiBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEmojiBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EmojiViewModel by viewModels()

    private lateinit var emojiAdapter: EmojiAdapter
    private lateinit var categoryAdapter: EmojiCategoryAdapter

    var onEmojiSelected: ((String) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetEmojiBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapters()
        setupActions()
        observeUiState()
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupAdapters() {
        emojiAdapter = EmojiAdapter { item ->
            val emoji = viewModel.onEmojiSelected(item.emoji)
            onEmojiSelected?.invoke(emoji)
        }
        binding.rvEmoji.apply {
            layoutManager = GridLayoutManager(requireContext(), GRID_SPAN_COUNT)
            adapter = emojiAdapter
            itemAnimator = null
            setHasFixedSize(true)
        }

        categoryAdapter = EmojiCategoryAdapter { category ->
            viewModel.selectCategory(category.id)
        }
        categoryAdapter.submitList(EmojiCategory.displayCategories)
        binding.rvCategories.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
            itemAnimator = null
        }
    }

    private fun setupActions() {
        binding.btnClose.setOnClickListener { dismiss() }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    categoryAdapter.setSelectedCategory(state.selectedCategoryId)
                    emojiAdapter.submitList(state.emojis)
                }
            }
        }
    }

    companion object {
        private const val TAG = "EmojiBottomSheet"
        private const val GRID_SPAN_COUNT = 8

        fun show(
            fragmentManager: FragmentManager,
            onEmojiSelected: (String) -> Unit
        ) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            EmojiBottomSheetFragment().apply {
                this.onEmojiSelected = onEmojiSelected
            }.show(fragmentManager, TAG)
        }
    }
}
