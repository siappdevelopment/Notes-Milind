package com.notebook.notes.checklist.ui.note

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.DialogSetReminderBinding
import com.notebook.notes.checklist.model.RepeatType
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.util.SystemBars
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.TimeZone

@AndroidEntryPoint
class SetReminderBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogSetReminderBinding? = null
    private val binding get() = _binding!!

    var onSetReminder: ((Calendar, RepeatType) -> Unit)? = null

    private var selectedRepeatType: RepeatType = RepeatType.NEVER

    private val selectedDateTime: Calendar = Calendar.getInstance().apply {
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.HOUR_OF_DAY, 1)
        set(Calendar.MINUTE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSetReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        DateTimeUtils.refresh(requireContext())

        arguments?.getLong(ARG_INITIAL_TIME, -1L)?.takeIf { it > 0L }?.let { millis ->
            selectedDateTime.timeInMillis = millis
            selectedDateTime.set(Calendar.SECOND, 0)
            selectedDateTime.set(Calendar.MILLISECOND, 0)
        }
        selectedRepeatType = RepeatType.fromStorage(arguments?.getString(ARG_REPEAT_TYPE))

        updateDateText()
        updateTimeText()
        updateRepeatText()

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnCancel.setOnClickListener { dismiss() }

        binding.layoutDate.setOnClickListener { showDatePicker() }
        binding.layoutTime.setOnClickListener { showTimePicker() }

        binding.layoutRepeat.setOnClickListener {
            RepeatOptionsDialog.show(
                parentFragmentManager,
                selectedRepeatType.toDisplayLabel()
            ) { selectedRepeat ->
                selectedRepeatType = RepeatType.fromDisplayLabel(selectedRepeat)
                updateRepeatText()
            }
        }

        binding.btnSet.setOnClickListener {
            onSetReminder?.invoke(selectedDateTime.clone() as Calendar, selectedRepeatType)
            dismiss()
        }
    }

    private fun showDatePicker() {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                selectedDateTime.get(Calendar.YEAR),
                selectedDateTime.get(Calendar.MONTH),
                selectedDateTime.get(Calendar.DAY_OF_MONTH)
            )
        }

        val constraints = CalendarConstraints.Builder()
            .setValidator(DateValidatorPointForward.now())
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.set_reminder)
            .setSelection(utc.timeInMillis)
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { millis ->
            val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = millis
            }
            selectedDateTime.set(
                picked.get(Calendar.YEAR),
                picked.get(Calendar.MONTH),
                picked.get(Calendar.DAY_OF_MONTH),
                selectedDateTime.get(Calendar.HOUR_OF_DAY),
                selectedDateTime.get(Calendar.MINUTE),
                0
            )
            selectedDateTime.set(Calendar.MILLISECOND, 0)
            updateDateText()
        }

        datePicker.show(parentFragmentManager, DATE_PICKER_TAG)
    }

    private fun showTimePicker() {
        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(DateTimeUtils.materialClockFormat())
            .setHour(selectedDateTime.get(Calendar.HOUR_OF_DAY))
            .setMinute(selectedDateTime.get(Calendar.MINUTE))
            .setTitleText(R.string.set_reminder)
            .build()

        timePicker.addOnPositiveButtonClickListener {
            selectedDateTime.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            selectedDateTime.set(Calendar.MINUTE, timePicker.minute)
            selectedDateTime.set(Calendar.SECOND, 0)
            selectedDateTime.set(Calendar.MILLISECOND, 0)
            updateTimeText()
        }

        timePicker.show(parentFragmentManager, TIME_PICKER_TAG)
    }

    private fun updateDateText() {
        binding.tvDate.text = DateTimeUtils.formatReminderDate(selectedDateTime.timeInMillis)
    }

    private fun updateTimeText() {
        binding.tvTime.text = DateTimeUtils.formatTime(selectedDateTime.timeInMillis)
    }

    private fun updateRepeatText() {
        binding.tvRepeat.text = when (selectedRepeatType) {
            RepeatType.NEVER -> getString(R.string.does_not_repeat)
            RepeatType.DAILY -> getString(R.string.daily)
            RepeatType.WEEKLY -> getString(R.string.weekly)
            RepeatType.MONTHLY -> getString(R.string.monthly)
        }
    }

    override fun onStart() {
        super.onStart()
        BottomSheetHelper.prepare(dialog)
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        super.onDismiss(dialog)
        SystemBars.onDialogFragmentDismissed(activity?.window)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "SetReminderBottomSheet"
        private const val DATE_PICKER_TAG = "SetReminderDatePicker"
        private const val TIME_PICKER_TAG = "SetReminderTimePicker"
        private const val ARG_INITIAL_TIME = "arg_initial_time"
        private const val ARG_REPEAT_TYPE = "arg_repeat_type"

        fun show(
            fragmentManager: FragmentManager,
            initialTimeMillis: Long? = null,
            initialRepeatType: RepeatType = RepeatType.NEVER,
            onSet: (Calendar, RepeatType) -> Unit
        ) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            SetReminderBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_INITIAL_TIME, initialTimeMillis ?: -1L)
                    putString(ARG_REPEAT_TYPE, initialRepeatType.name)
                }
                this.onSetReminder = onSet
            }.show(fragmentManager, TAG)
        }
    }
}
