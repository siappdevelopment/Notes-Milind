package com.notebook.notes.checklist.ui.note

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.BottomSheetInsertAudioBinding
import com.notebook.notes.checklist.model.formatVoiceDuration
import com.notebook.notes.checklist.viewmodel.VoiceNoteViewModel
import com.notebook.notes.checklist.util.BottomSheetHelper
import com.notebook.notes.checklist.util.SystemBars
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

object InsertAudioBottomSheet {

    fun show(
        activity: AppCompatActivity,
        voiceNoteViewModel: VoiceNoteViewModel,
        onStartRecording: () -> Unit
    ) {
        val dialog = BottomSheetHelper.create(activity)
        val binding = BottomSheetInsertAudioBinding.inflate(LayoutInflater.from(activity))
        var confirmed = false
        var stateJob: Job? = null

        var pulseAnimator: ObjectAnimator? = null

        fun startPulseAnimation() {
            binding.pulseRingOuter.visibility = View.VISIBLE
            binding.pulseRingInner.visibility = View.VISIBLE
            pulseAnimator?.cancel()
            pulseAnimator = ObjectAnimator.ofFloat(binding.pulseRingOuter, View.ALPHA, 0.35f, 0.08f).apply {
                duration = 900
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = LinearInterpolator()
                start()
            }
            ObjectAnimator.ofFloat(binding.pulseRingInner, View.ALPHA, 0.5f, 0.15f).apply {
                duration = 900
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                interpolator = LinearInterpolator()
                start()
            }
        }

        fun stopPulseAnimation() {
            pulseAnimator?.cancel()
            pulseAnimator = null
            binding.pulseRingOuter.visibility = View.INVISIBLE
            binding.pulseRingInner.visibility = View.INVISIBLE
        }

        fun updateUi(isRecording: Boolean, isPaused: Boolean, durationMs: Long, amplitude: Int) {
            binding.tvTimer.text = formatVoiceDuration(durationMs)
            binding.waveformView.setAmplitude(amplitude, isRecording && !isPaused)
            binding.btnCancel.visibility = if (isRecording) View.VISIBLE else View.INVISIBLE
            binding.btnConfirm.visibility = if (isRecording) View.VISIBLE else View.INVISIBLE

            when {
                !isRecording -> {
                    binding.btnRecord.setImageResource(R.drawable.ic_mic_white)
                    binding.btnRecord.contentDescription =
                        binding.root.context.getString(R.string.recording)
                    stopPulseAnimation()
                }
                isPaused -> {
                    binding.btnRecord.setImageResource(R.drawable.ic_play_white)
                    binding.btnRecord.contentDescription =
                        binding.root.context.getString(R.string.resume)
                    stopPulseAnimation()
                }
                else -> {
                    binding.btnRecord.setImageResource(R.drawable.ic_pause_white)
                    binding.btnRecord.contentDescription =
                        binding.root.context.getString(R.string.pause)
                    startPulseAnimation()
                }
            }
        }

        binding.btnClose.setOnClickListener { dialog.dismiss() }

        binding.btnRecord.setOnClickListener {
            val state = voiceNoteViewModel.uiState.value
            when {
                !state.isRecording -> onStartRecording()
                state.isPaused -> voiceNoteViewModel.resumeRecording()
                else -> voiceNoteViewModel.pauseRecording()
            }
        }

        binding.btnCancel.setOnClickListener {
            voiceNoteViewModel.cancelRecording()
            dialog.dismiss()
        }

        binding.btnConfirm.setOnClickListener {
            confirmed = true
            if (voiceNoteViewModel.uiState.value.isRecording) {
                voiceNoteViewModel.stopRecording()
            }
            dialog.dismiss()
        }

        dialog.setOnDismissListener {
            SystemBars.hideNavigationBar(activity)
            stateJob?.cancel()
            stopPulseAnimation()
            if (!confirmed && voiceNoteViewModel.uiState.value.isRecording) {
                voiceNoteViewModel.cancelRecording()
            }
        }

        dialog.setContentView(binding.root)
        dialog.setOnShowListener {
            BottomSheetHelper.prepare(dialog)
        }

        stateJob = activity.lifecycleScope.launch {
            voiceNoteViewModel.uiState.collect { state ->
                updateUi(
                    isRecording = state.isRecording,
                    isPaused = state.isPaused,
                    durationMs = state.currentDuration,
                    amplitude = state.recordingAmplitude
                )
            }
        }

        updateUi(
            isRecording = voiceNoteViewModel.uiState.value.isRecording,
            isPaused = voiceNoteViewModel.uiState.value.isPaused,
            durationMs = voiceNoteViewModel.uiState.value.currentDuration,
            amplitude = voiceNoteViewModel.uiState.value.recordingAmplitude
        )

        dialog.show()
    }
}
