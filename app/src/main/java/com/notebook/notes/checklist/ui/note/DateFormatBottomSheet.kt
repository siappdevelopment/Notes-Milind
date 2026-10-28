package com.notebook.notes.checklist.ui.note

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ItemDateFormatBinding
import com.notebook.notes.checklist.databinding.PanelDateFormatBinding
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.SystemBars
import com.notebook.notes.checklist.utils.Constant
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DateFormatBottomSheet : BottomSheetDialogFragment() {

    private var _binding: PanelDateFormatBinding? = null
    private val binding get() = _binding!!

    private var selectedFormat: String = Constant.DEFAULT_DATE_FORMAT
    private var onFormatSelected: ((String) -> Unit)? = null

    private val dateFormats = listOf(
        "dd/MM/yyyy",
        "yyyy-MM-dd",
        "dd-MM-yyyy",
        "MM/dd/yyyy",
        "dd.MM.yyyy",
        "yyyy.MM.dd",
        "yyyy.dd.MM"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PanelDateFormatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectedFormat = arguments?.getString(ARG_DATE_FORMAT, Constant.DEFAULT_DATE_FORMAT)
            ?: Constant.DEFAULT_DATE_FORMAT

        setupRecyclerView()

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setupRecyclerView() {
        val adapter = DateFormatAdapter(dateFormats, selectedFormat) { format ->
            selectedFormat = format
            onFormatSelected?.invoke(format)
            dismiss()
        }
        binding.rvDateFormats.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDateFormats.adapter = adapter
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
        _binding = null
    }

    inner class DateFormatAdapter(
        private val formats: List<String>,
        private val currentSelected: String,
        private val onSelected: (String) -> Unit
    ) : RecyclerView.Adapter<DateFormatAdapter.ViewHolder>() {

        private val exampleDate = Calendar.getInstance().apply {
            set(2026, 0, 7) // Jan 7, 2026 to match screenshot example
        }.time

        inner class ViewHolder(val itemBinding: ItemDateFormatBinding) :
            RecyclerView.ViewHolder(itemBinding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemDateFormatBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val format = formats[position]
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            holder.itemBinding.tvDateFormat.text = sdf.format(exampleDate)

            if (format == currentSelected) {
                holder.itemBinding.ivRadio.setImageResource(R.drawable.ic_time_redio)
            } else {
                holder.itemBinding.ivRadio.setImageResource(R.drawable.ic_radio_unchecked)
            }

            holder.itemBinding.rootLayout.setOnClickListener {
                onSelected(format)
            }
            
            holder.itemBinding.divider.visibility = if (position == formats.size - 1) View.GONE else View.VISIBLE
        }

        override fun getItemCount(): Int = formats.size
    }

    companion object {
        private const val TAG = "DateFormatBottomSheet"
        private const val ARG_DATE_FORMAT = "arg_date_format"

        fun show(
            fragmentManager: FragmentManager,
            currentDateFormat: String,
            onSelected: (String) -> Unit
        ) {
            val fragment = DateFormatBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_DATE_FORMAT, currentDateFormat)
                }
                onFormatSelected = onSelected
            }
            fragment.show(fragmentManager, TAG)
        }
    }
}
