package com.note.quick.pad.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.note.quick.pad.R
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

        view.findViewById<View>(R.id.btnClose).setOnClickListener {
            dismiss()
        }

        noteDetails?.let { details ->
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

            view.findViewById<TextView>(R.id.tvCreatedAt).text = dateFormat.format(Date(details.createdAt))
            view.findViewById<TextView>(R.id.tvModifiedAt).text = dateFormat.format(Date(details.modifiedAt))
            view.findViewById<TextView>(R.id.tvWordCount).text = details.wordCount.toString()
            view.findViewById<TextView>(R.id.tvCharCountIncludeSpaces).text = details.charCountIncludeSpaces.toString()
            view.findViewById<TextView>(R.id.tvCharCountExcludeSpaces).text = details.charCountExcludeSpaces.toString()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
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
