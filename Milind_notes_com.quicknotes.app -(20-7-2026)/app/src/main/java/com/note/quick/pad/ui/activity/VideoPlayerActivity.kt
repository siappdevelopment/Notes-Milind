package com.note.quick.pad.ui.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.note.quick.pad.R
import com.note.quick.pad.attachment.AttachmentIntentHelper
import com.note.quick.pad.databinding.ActivityVideoPlayerBinding
import com.note.quick.pad.ui.home.NotesSelectionDialogs
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private val progressHandler = Handler(Looper.getMainLooper())
    private var controlsVisible = true
    private var userSeeking = false
    private var videoDurationMs = 0
    private var videoPath = ""
    private var mimeType = "video/*"
    private var blockId = -1L

    private val progressRunnable = object : Runnable {
        override fun run() {
            if (!userSeeking && binding.videoView.isPlaying) {
                updateProgress(binding.videoView.currentPosition)
            }
            progressHandler.postDelayed(this, PROGRESS_INTERVAL_MS)
        }
    }

    private val hideControlsRunnable = Runnable { setControlsVisible(false) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        videoPath = intent.getStringExtra(EXTRA_VIDEO_PATH).orEmpty()
        mimeType = intent.getStringExtra(EXTRA_MIME_TYPE).orEmpty().ifBlank { "video/*" }
        blockId = intent.getLongExtra(EXTRA_BLOCK_ID, -1L)
        val title = intent.getStringExtra(EXTRA_VIDEO_TITLE).orEmpty()
            .ifBlank { File(videoPath).name }

        if (videoPath.isBlank() || !File(videoPath).exists()) {
            Toast.makeText(this, R.string.attachment_missing, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.tvTitle.text = title
        setupWindowInsets()
        setupActions()
        setupVideo()
        enterImmersiveMode()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.topBar.updatePadding(top = systemBars.top)
            binding.bottomControls.updatePadding(bottom = systemBars.bottom + 8)
            insets
        }
    }

    private fun setupActions() {
        binding.btnBack.setOnClickListener { finish() }
        binding.btnShare.setOnClickListener { shareVideo() }
        binding.btnDelete.setOnClickListener { confirmDelete() }
        binding.btnDelete.visibility = if (blockId > 0L) View.VISIBLE else View.GONE

        binding.btnCenterPlay.setOnClickListener { togglePlayPause() }
        binding.btnPlayPause.setOnClickListener { togglePlayPause() }
        binding.btnSkipPrevious.setOnClickListener { seekBy(-SEEK_STEP_MS) }
        binding.btnSkipNext.setOnClickListener { seekBy(SEEK_STEP_MS) }

        binding.videoView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                toggleControlsVisibility()
            }
            true
        }
        binding.topGradient.setOnClickListener { toggleControlsVisibility() }
        binding.bottomGradient.setOnClickListener { toggleControlsVisibility() }

        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tvCurrentTime.text = formatTime(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                userSeeking = true
                cancelHideControls()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                userSeeking = false
                binding.videoView.seekTo(seekBar?.progress ?: 0)
                scheduleHideControls()
            }
        })
    }

    private fun setupVideo() {
        binding.videoView.setVideoPath(videoPath)
        binding.videoView.setOnPreparedListener { player ->
            videoDurationMs = player.duration.coerceAtLeast(0)
            binding.seekBar.max = videoDurationMs
            binding.tvDuration.text = formatTime(videoDurationMs)
            updateProgress(0)
            player.start()
            updatePlayPauseUi(isPlaying = true)
            scheduleHideControls()
            progressHandler.post(progressRunnable)
        }
        binding.videoView.setOnCompletionListener {
            updateProgress(videoDurationMs)
            updatePlayPauseUi(isPlaying = false)
            setControlsVisible(true)
            cancelHideControls()
        }
        binding.videoView.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, R.string.playback_failed, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun togglePlayPause() {
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
            updatePlayPauseUi(isPlaying = false)
            setControlsVisible(true)
            cancelHideControls()
        } else {
            if (binding.videoView.currentPosition >= videoDurationMs && videoDurationMs > 0) {
                binding.videoView.seekTo(0)
            }
            binding.videoView.start()
            updatePlayPauseUi(isPlaying = true)
            scheduleHideControls()
        }
    }

    private fun seekBy(deltaMs: Int) {
        if (videoDurationMs <= 0) return
        val target = (binding.videoView.currentPosition + deltaMs)
            .coerceIn(0, videoDurationMs)
        binding.videoView.seekTo(target)
        updateProgress(target)
        scheduleHideControls()
    }

    private fun updateProgress(positionMs: Int) {
        binding.seekBar.progress = positionMs.coerceIn(0, binding.seekBar.max)
        binding.tvCurrentTime.text = formatTime(positionMs)
    }

    private fun updatePlayPauseUi(isPlaying: Boolean) {
        val icon = if (isPlaying) R.drawable.ic_pause_white else R.drawable.ic_play_white
        val description = if (isPlaying) R.string.pause else R.string.play
        binding.btnPlayPause.setImageResource(icon)
        binding.btnPlayPause.contentDescription = getString(description)
        binding.btnCenterPlay.visibility = if (isPlaying) View.GONE else View.VISIBLE
    }

    private fun toggleControlsVisibility() {
        setControlsVisible(!controlsVisible)
        if (controlsVisible && binding.videoView.isPlaying) {
            scheduleHideControls()
        } else {
            cancelHideControls()
        }
    }

    private fun setControlsVisible(visible: Boolean) {
        controlsVisible = visible
        val visibility = if (visible) View.VISIBLE else View.GONE
        binding.topBar.visibility = visibility
        binding.topGradient.visibility = visibility
        binding.bottomControls.visibility = visibility
        binding.bottomGradient.visibility = visibility
        if (visible && !binding.videoView.isPlaying) {
            binding.btnCenterPlay.visibility = View.VISIBLE
        } else if (!visible) {
            binding.btnCenterPlay.visibility = View.GONE
        }
    }

    private fun scheduleHideControls() {
        cancelHideControls()
        if (binding.videoView.isPlaying) {
            progressHandler.postDelayed(hideControlsRunnable, HIDE_CONTROLS_DELAY_MS)
        }
    }

    private fun cancelHideControls() {
        progressHandler.removeCallbacks(hideControlsRunnable)
    }

    private fun shareVideo() {
        AttachmentIntentHelper.shareFile(this, videoPath, mimeType).onFailure {
            Toast.makeText(this, R.string.share_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete() {
        if (blockId <= 0L) return
        NotesSelectionDialogs.showConfirmation(
            context = this,
            layoutInflater = layoutInflater,
            titleRes = R.string.delete,
            messageRes = R.string.delete_media_confirm,
            positiveButtonRes = R.string.delete,
            iconRes = R.drawable.ic_recylerbin,
            onPositiveClick = {
                setResult(
                    RESULT_OK,
                    Intent().putExtra(EXTRA_DELETED_BLOCK_ID, blockId)
                )
                finish()
            }
        )
    }

    private fun enterImmersiveMode() {
        WindowInsetsControllerCompat(window, binding.root).apply {
            hide(WindowInsetsCompat.Type.navigationBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.toLong()).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    override fun onPause() {
        super.onPause()
        if (::binding.isInitialized && binding.videoView.isPlaying) {
            binding.videoView.pause()
            updatePlayPauseUi(isPlaying = false)
        }
        cancelHideControls()
        progressHandler.removeCallbacks(progressRunnable)
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            progressHandler.post(progressRunnable)
        }
    }

    override fun onDestroy() {
        progressHandler.removeCallbacksAndMessages(null)
        if (::binding.isInitialized) {
            binding.videoView.stopPlayback()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_VIDEO_PATH = "extra_video_path"
        const val EXTRA_VIDEO_TITLE = "extra_video_title"
        const val EXTRA_MIME_TYPE = "extra_mime_type"
        const val EXTRA_BLOCK_ID = "extra_block_id"
        const val EXTRA_DELETED_BLOCK_ID = "extra_deleted_block_id"

        private const val PROGRESS_INTERVAL_MS = 250L
        private const val HIDE_CONTROLS_DELAY_MS = 3000L
        private const val SEEK_STEP_MS = 10_000

        fun createIntent(
            context: android.content.Context,
            path: String,
            title: String,
            mimeType: String = "video/*",
            blockId: Long = -1L
        ): Intent = Intent(context, VideoPlayerActivity::class.java).apply {
            putExtra(EXTRA_VIDEO_PATH, path)
            putExtra(EXTRA_VIDEO_TITLE, title)
            putExtra(EXTRA_MIME_TYPE, mimeType)
            putExtra(EXTRA_BLOCK_ID, blockId)
        }
    }
}
