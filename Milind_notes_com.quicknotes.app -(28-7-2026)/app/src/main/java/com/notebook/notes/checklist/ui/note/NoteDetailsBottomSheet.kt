package com.notebook.notes.checklist.ui.note

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.util.SystemBars
import dagger.hilt.android.AndroidEntryPoint

data class NoteDetails(
    val createdAt: Long,
    val modifiedAt: Long,
    val wordCount: Int,
    val charCountIncludeSpaces: Int,
    val charCountExcludeSpaces: Int
)

@AndroidEntryPoint
class NoteDetailsBottomSheet : BottomSheetDialogFragment() {

    private var noteDetails: NoteDetails? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_note_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DateTimeUtils.refresh(requireContext())

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        noteDetails?.let { details ->
            view.findViewById<TextView>(R.id.tvCreatedAt).text =
                DateTimeUtils.formatDateTime(details.createdAt)
            view.findViewById<TextView>(R.id.tvModifiedAt).text =
                DateTimeUtils.formatDateTime(details.modifiedAt)
            view.findViewById<TextView>(R.id.tvWordCount).text = details.wordCount.toString()
            view.findViewById<TextView>(R.id.tvCharCountIncludeSpaces).text =
                details.charCountIncludeSpaces.toString()
            view.findViewById<TextView>(R.id.tvCharCountExcludeSpaces).text =
                details.charCountExcludeSpaces.toString()
        }
    }

    override fun onStart() {
        super.onStart()
        BottomSheetHelper.prepare(dialog)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        SystemBars.onDialogFragmentDismissed(activity?.window)
    }

    companion object {
        private const val TAG = "NoteDetailsBottomSheet"

        fun show(
            fragmentManager: FragmentManager,
            details: NoteDetails
        ) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            NoteDetailsBottomSheet().apply {
                this.noteDetails = details
            }.show(fragmentManager, TAG)
        }
    }
}
