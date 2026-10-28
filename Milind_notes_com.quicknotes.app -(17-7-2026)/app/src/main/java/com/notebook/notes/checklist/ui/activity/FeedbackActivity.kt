package com.notebook.notes.checklist.ui.activity

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.google.android.material.snackbar.Snackbar
import com.notebook.notes.checklist.Advertisement.ADSBannerSmall
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSNativeDisplay
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityFeedbackBinding
import com.notebook.notes.checklist.viewmodel.FeedbackViewModel
import com.notebook.notes.checklist.viewmodel.utils.finishWithSettingBackInterAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FeedbackActivity : BaseActivity() {

    private lateinit var binding: ActivityFeedbackBinding
    private val viewModel: FeedbackViewModel by viewModels()
    private var attachmentUri: Uri? = null
    private var thankYouDialog: AlertDialog? = null
    private var noInternetDialog: AlertDialog? = null
    private var selectedFeedbackTypeId = R.id.chipSuggestion

    companion object {
        private const val TITLE_MAX_LENGTH = 50
        private const val DESCRIPTION_MAX_LENGTH = 200
    }

    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        setAttachment(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFeedbackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupToolbar()
        setupBackPress()
        setupInputLimits()
        observeState()
        setupClicks()
        loadOtherScreenAds()
    }

    private fun setupInputLimits() {
        binding.etTitle.filters = arrayOf(InputFilter.LengthFilter(TITLE_MAX_LENGTH))
        binding.etDescription.filters = arrayOf(InputFilter.LengthFilter(DESCRIPTION_MAX_LENGTH))
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener { finishWithSettingBackInterAd() }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    finishWithSettingBackInterAd()
                }
            }
        )
    }

    private fun setupClicks() {
        binding.btnAddPhoto.setOnClickListener { pickImage.launch("image/*") }
        binding.btnRemovePhoto.setOnClickListener { clearAttachment() }
        binding.btnSubmit.setOnClickListener { onSubmitClicked() }
        binding.chipSuggestion.setOnClickListener { selectFeedbackType(R.id.chipSuggestion) }
        binding.chipBug.setOnClickListener { selectFeedbackType(R.id.chipBug) }
        binding.chipFeature.setOnClickListener { selectFeedbackType(R.id.chipFeature) }
        binding.chipGeneral.setOnClickListener { selectFeedbackType(R.id.chipGeneral) }
        selectFeedbackType(selectedFeedbackTypeId)
    }

    private fun selectFeedbackType(selectedId: Int) {
        selectedFeedbackTypeId = selectedId
        binding.chipSuggestion.isSelected = selectedId == R.id.chipSuggestion
        binding.chipBug.isSelected = selectedId == R.id.chipBug
        binding.chipFeature.isSelected = selectedId == R.id.chipFeature
        binding.chipGeneral.isSelected = selectedId == R.id.chipGeneral
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state.submitted) {
                        showThankYouDialog()
                        return@collect
                    }
                    setSubmitting(state.isSubmitting)
                    state.errorMessage?.let { message ->
                        if (isNetworkErrorMessage(message)) {
                            viewModel.clearError()
                            showNoInternetDialog { onSubmitClicked() }
                        } else {
                            showMessage(message)
                            viewModel.clearError()
                        }
                    }
                }
            }
        }
    }

    private fun onSubmitClicked() {
        val feedbackType = selectedFeedbackType()
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val description = binding.etDescription.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()

        // Validate form first — only then check internet
        when {
            feedbackType.isNullOrBlank() -> {
                showMessage(getString(R.string.feedback_type_required))
                return
            }
            title.isBlank() -> {
                showMessage(getString(R.string.feedback_title_required))
                return
            }
            title.length > TITLE_MAX_LENGTH -> {
                showMessage(getString(R.string.feedback_title_too_long))
                return
            }
            description.isBlank() -> {
                showMessage(getString(R.string.feedback_description_required))
                return
            }
            description.length > DESCRIPTION_MAX_LENGTH -> {
                showMessage(getString(R.string.feedback_description_too_long))
                return
            }
            email.isNotBlank() && !isValidEmail(email) -> {
                showMessage(getString(R.string.feedback_email_invalid))
                binding.etEmail.requestFocus()
                return
            }
        }

        // Form is complete — require internet before submit
        if (!isInternetAvailable()) {
            showNoInternetDialog { onSubmitClicked() }
            return
        }

        val category = feedbackType
        val combinedTitle = "$category - $title"
        viewModel.submit(
            title = combinedTitle,
            description = description,
            feedbackType = category,
            email = email.ifBlank { null },
            imageUri = attachmentUri
        )
    }

    private fun selectedFeedbackType(): String? {
        return when (selectedFeedbackTypeId) {
            R.id.chipSuggestion -> getString(R.string.feedback_type_suggestion)
            R.id.chipBug -> getString(R.string.feedback_type_bug)
            R.id.chipFeature -> getString(R.string.feedback_type_feature)
            R.id.chipGeneral -> getString(R.string.feedback_type_general)
            else -> null
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isInternetAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    )
        } else {
            @Suppress("DEPRECATION")
            val info = cm.activeNetworkInfo
            info != null && info.isConnected
        }
    }

    private fun isNetworkErrorMessage(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("unable to resolve host") ||
            lower.contains("failed to connect") ||
            lower.contains("network") ||
            lower.contains("timeout") ||
            lower.contains("unreachable")
    }

    private fun setSubmitting(isSubmitting: Boolean) {
        binding.btnSubmit.isEnabled = !isSubmitting
        binding.btnSubmit.alpha = if (isSubmitting) 0.6f else 1f
        binding.etTitle.isEnabled = !isSubmitting
        binding.etDescription.isEnabled = !isSubmitting
        binding.etEmail.isEnabled = !isSubmitting
        binding.btnAddPhoto.isEnabled = !isSubmitting
        binding.btnRemovePhoto.isEnabled = !isSubmitting
        binding.chipSuggestion.isEnabled = !isSubmitting
        binding.chipBug.isEnabled = !isSubmitting
        binding.chipFeature.isEnabled = !isSubmitting
        binding.chipGeneral.isEnabled = !isSubmitting
    }

    private fun setAttachment(uri: Uri) {
        attachmentUri = uri
        binding.addPhotoEmpty.isVisible = false
        binding.photoPreview.isVisible = true
        binding.ivPhoto.load(uri) { crossfade(true) }
    }

    private fun clearAttachment() {
        attachmentUri = null
        binding.photoPreview.isVisible = false
        binding.addPhotoEmpty.isVisible = true
        binding.ivPhoto.setImageDrawable(null)
    }

    private fun showThankYouDialog() {
        if (thankYouDialog?.isShowing == true || isFinishing || isDestroyed) return

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_feedback_thank_you, null, false)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialogView.findViewById<TextView>(R.id.btnBackToNotes).setOnClickListener {
            dialog.dismiss()
            finishWithSettingBackInterAd()
        }
        thankYouDialog = dialog
        dialog.show()
    }

    private fun showNoInternetDialog(onRetry: () -> Unit) {
        if (isFinishing || isDestroyed) return
        if (noInternetDialog?.isShowing == true) return

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.internet_massage_box, null, false)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnDismissListener {
            if (noInternetDialog === dialog) noInternetDialog = null
        }
        dialogView.findViewById<TextView>(R.id.btn_try_again).setOnClickListener {
            dialog.dismiss()
            // Retry: re-run submit (validates form + checks internet again)
            onRetry()
        }
        noInternetDialog = dialog
        dialog.show()
    }

    private fun showMessage(message: String) {
        Snackbar.make(binding.main, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun loadOtherScreenAds() {
        if (!ADSMainClass.getOtherAdsShow()) {
            hideOtherScreenAdPlaceholders()
            return
        }
        if (!ADSConsentManager.canRequestAds(this) || ADSMainClass.getAds_Free()) {
            hideOtherScreenAdPlaceholders()
            return
        }
        if (ADSMainClass.getOtherAdsType().equals("native")) {
            ADSNativeDisplay.loadAdmobNativeAdBig(
                ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_NATIVE),
                findViewById(R.id.flNativeSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                "small",
                this
            )
        } else {
            ADSBannerSmall.loadAdMobBanner(
                ADSMainClass.getStringValue(ADSMainClass.OTHER_SCREEN_BANNER),
                findViewById(R.id.flBannerSmallPlaceholder),
                findViewById(R.id.shimmer_container_banner),
                this
            )
        }
    }

    private fun hideOtherScreenAdPlaceholders() {
        findViewById<View>(R.id.llOtherScreenAds)?.visibility = View.GONE
    }

    override fun onDestroy() {
        thankYouDialog?.dismiss()
        thankYouDialog = null
        noInternetDialog?.dismiss()
        noInternetDialog = null
        super.onDestroy()
    }
}
