package com.notebook.notes.checklist.ui.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.DialogRateUsBinding
import com.notebook.notes.checklist.util.SystemBars

class RatingBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogRateUsBinding? = null
    private val binding get() = _binding!!

    private var rating = 1
    private var onRatingSubmitted: ((Int) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRateUsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let(SystemBars::hideNavigationBar)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.window?.let(SystemBars::hideNavigationBar)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val stars = listOf(binding.star1, binding.star2, binding.star3, binding.star4, binding.star5)

        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                updateRating(index + 1, stars)
            }
        }

        // Initialize with default rating 1 as shown in image
        updateRating(1, stars)

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.btnMaybeLater.setOnClickListener {
            dismiss()
        }

        binding.btnSubmit.setOnClickListener {
            onRatingSubmitted?.invoke(rating)
            dismiss()
        }
    }

    private fun updateRating(newRating: Int, stars: List<ImageView>) {
        rating = newRating
        stars.forEachIndexed { index, imageView ->
            if (index < rating) {
                imageView.setImageResource(R.drawable.ic_star_filled)
                imageView.imageTintList = null
            } else {
                imageView.setImageResource(R.drawable.ic_star_outline)
                imageView.imageTintList = ContextCompat.getColorStateList(requireContext(), R.color.grey_1)
            }
        }
    }

    fun setOnRatingSubmittedListener(listener: (Int) -> Unit) {
        onRatingSubmitted = listener
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "RatingBottomSheet"
    }
}
