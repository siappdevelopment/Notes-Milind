package com.notebook.notes.checklist.ui.note

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.PanelListItemHeightBinding
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.SystemBars

class ListItemHeightBottomSheet : BottomSheetDialogFragment() {

    private var _viewBinding: PanelListItemHeightBinding? = null
    private val viewBinding get() = _viewBinding!!

    private var selectedHeight: Int = 20
    private var onHeightSelected: ((Int) -> Unit)? = null

    private val heights = listOf(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 24, 28, 32, 36, 40)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _viewBinding = PanelListItemHeightBinding.inflate(inflater, container, false)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedHeight = arguments?.getInt(ARG_HEIGHT, 20) ?: 20

        updatePreview(selectedHeight)

        val initialIndex = heights.indexOfFirst { it >= selectedHeight }.coerceAtLeast(0)

        viewBinding.sliderItemHeight.max = heights.size - 1
        viewBinding.sliderItemHeight.progress = initialIndex

        viewBinding.sliderItemHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                selectedHeight = heights[progress]
                updatePreview(selectedHeight)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        viewBinding.btnClose.setOnClickListener {
            dismiss()
        }

        viewBinding.btnCancel.setOnClickListener {
            dismiss()
        }

        viewBinding.btnSet.setOnClickListener {
            onHeightSelected?.invoke(selectedHeight)
            dismiss()
        }
    }

    private fun updatePreview(heightPx: Int) {
        viewBinding.tvHeightValue.text = getString(R.string.font_size_px, heightPx)
        // setLineSpacing(add, mult) -> add is extra space in pixels
        viewBinding.tvPreview.setLineSpacing(heightPx.toFloat(), 1.0f)
    }

    override fun onStart() {
        super.onStart()
        BottomSheetHelper.prepare(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        SystemBars.onDialogFragmentDismissed(activity?.window)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _viewBinding = null
    }

    companion object {
        private const val TAG = "ListItemHeightBottomSheet"
        private const val ARG_HEIGHT = "arg_height"

        fun show(
            fragmentManager: FragmentManager,
            currentHeight: Int,
            onSelected: (Int) -> Unit
        ) {
            val fragment = ListItemHeightBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_HEIGHT, currentHeight)
                }
                onHeightSelected = onSelected
            }
            fragment.show(fragmentManager, TAG)
        }
    }
}
