package com.notebook.notes.checklist.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.ImageViewCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.notebook.notes.checklist.ads.AdRemoteConfig
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.loadScreenAd
import com.notebook.notes.checklist.ads.setting_screen
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivitySettingBinding
import com.notebook.notes.checklist.ui.home.NotesSelectionDialogs
import com.notebook.notes.checklist.helper.BiometricAuthHelper
import com.notebook.notes.checklist.helper.NotePasswordManager
import com.notebook.notes.checklist.helper.PreferenceHelper
import com.notebook.notes.checklist.model.LanguageCatalog
import com.notebook.notes.checklist.repository.NoteRepository
import com.notebook.notes.checklist.ui.note.FontSizeBottomSheet
import com.notebook.notes.checklist.ui.note.FontStyleBottomSheet
import com.notebook.notes.checklist.ui.note.ListItemHeightBottomSheet
import com.notebook.notes.checklist.ui.note.LockManagementBottomSheet
import com.notebook.notes.checklist.ui.note.TimeFormatBottomSheet
import com.notebook.notes.checklist.ui.richtext.RichTextFormats
import com.notebook.notes.checklist.util.DateTimeUtils
import com.notebook.notes.checklist.utils.Constant
import com.notebook.notes.checklist.viewmodel.utils.finishWithSettingBackInterAd
import com.notebook.notes.checklist.viewmodel.utils.launchActivity
import com.notebook.notes.checklist.viewmodel.utils.openWithSettingInterAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SettingActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingBinding
    @Inject
    lateinit var preferenceHelper: PreferenceHelper
    @Inject
    lateinit var passwordManager: NotePasswordManager
    @Inject
    lateinit var noteRepository: NoteRepository
    private var isResetFlow = false
    private var isSecurityQuestionFlow = false
    private var isFingerprintSetupFlow = false
    private var pendingFingerprintEnrollment = false
    private lateinit var appUpdateManager: AppUpdateManager


    private val backgroundPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            updateSettingsUI()
        }
    }

    private val setPasswordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            if (!passwordManager.isAppLockEnabled()) {
                passwordManager.setAppLockEnabled(true)
            }
            updateSettingsUI()
            if (isFingerprintSetupFlow && passwordManager.hasPassword()) {
                isFingerprintSetupFlow = false
                attemptEnableFingerprint()
            }
        } else {
            isFingerprintSetupFlow = false
        }
    }

    private val verifyPasswordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            when {
                isSecurityQuestionFlow -> {
                    isSecurityQuestionFlow = false
                    openWithSettingInterAd {
                        changePasswordLauncher.launch(
                            SetPasswordActivity.createSecurityQuestionIntent(this)
                        )
                    }
                }

                isResetFlow -> {
                    isResetFlow = false
                    openWithSettingInterAd {
                        setPasswordLauncher.launch(
                            SetPasswordActivity.createSetIntent(this, -1L)
                        )
                    }
                }

                else -> {
                    showLockManagement()
                }
            }
        }
    }

    private val changePasswordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            updateSettingsUI()
        }
    }

    private val removePasswordLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            clearAppPasswordAndLocks()
        }
    }

    private fun clearAppPasswordAndLocks() {
        lifecycleScope.launch {
            passwordManager.clearPassword()
            noteRepository.unlockAllNotes()
            updateSettingsUI()
            Toast.makeText(
                this@SettingActivity,
                R.string.password_cleared_all_unlocked,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appUpdateManager = AppUpdateManagerFactory.create(this)

        DateTimeUtils.refresh(this)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupBackPress()
        updateSettingsUI()
        loadLanguageAds()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithSettingBackInterAd()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            updateCurrentLanguage()
        }
        if (pendingFingerprintEnrollment && passwordManager.hasPassword()) {
            if (BiometricAuthHelper.canAuthenticate(this)) {
                pendingFingerprintEnrollment = false
                enableFingerprintWithoutPrompt()
            }
        }
    }

    private fun updateSettingsUI() {
        updateCurrentLanguage()

        val fontSize = preferenceHelper.getInt(
            Constant.PREF_DEFAULT_FONT_SIZE,
            Constant.DEFAULT_FONT_SIZE
        )
        binding.tvCurrentFontSize.text = getString(R.string.font_size_px, fontSize)

        binding.tvCurrentFontStyle.text = displayNameForFontFamily(currentFontFamily())

        val itemHeight = preferenceHelper.getInt(
            Constant.PREF_LIST_ITEM_HEIGHT,
            Constant.DEFAULT_ITEM_HEIGHT
        )
        binding.tvCurrentItemHeight.text = getString(R.string.font_size_px, itemHeight)

        val timeFormat = preferenceHelper.getString(
            Constant.PREF_TIME_FORMAT,
            Constant.TIME_FORMAT_12
        ) ?: Constant.TIME_FORMAT_12
        binding.tvCurrentTimeFormat.text = if (timeFormat == Constant.TIME_FORMAT_24) {
            getString(R.string.time_format_24_short)
        } else {
            getString(R.string.time_format_12_short)
        }

        val dateFormat = preferenceHelper.getString(
            Constant.PREF_DATE_FORMAT,
            Constant.DEFAULT_DATE_FORMAT
        ) ?: Constant.DEFAULT_DATE_FORMAT
        binding.tvCurrentDateFormat.text = dateFormat.uppercase()

        binding.tvSetPasswordTitle.text = if (passwordManager.hasPassword()) {
            getString(R.string.reset_password)
        } else {
            getString(R.string.set_password)
        }

        binding.tvSecurityQuestionTitle.text = if (passwordManager.hasSecurityQuestion()) {
            getString(R.string.reset_security_question)
        } else {
            getString(R.string.set_security_question)
        }

        binding.switchFingerprint.isChecked = passwordManager.isFingerprintEnabled()

        updatePasswordStatus()
        updatePasswordDependentRows(passwordManager.hasPassword())
    }

    private fun updateCurrentLanguage() {
        val savedCode = preferenceHelper.getString(
            Constant.PREF_SELECTED_LANGUAGE_CODE,
            Constant.DEFAULT_LANGUAGE_CODE
        ) ?: Constant.DEFAULT_LANGUAGE_CODE
        binding.tvCurrentLanguage.text = LanguageCatalog.displayNameForCode(savedCode)
    }

    private fun updatePasswordStatus() {
        val isEnabled = passwordManager.hasPassword() && passwordManager.isAppLockEnabled()
        binding.tvCurrentPasswordStatus.text = if (isEnabled) {
            "Enabled"
        } else {
            "Not Enabled"
        }
    }

    private fun updatePasswordDependentRows(hasPassword: Boolean) {
        val titleColor = ContextCompat.getColor(
            this,
            if (hasPassword) R.color.text_black_2 else R.color.grey_1
        )
        val descColor = ContextCompat.getColor(this, R.color.grey_1)
        val greyIcon = ContextCompat.getColor(this, R.color.grey_1)

        // Security Question
        binding.tvSecurityQuestionTitle.setTextColor(titleColor)
        binding.tvSecurityQuestionDesc.setTextColor(descColor)
        setIconTint(
            binding.ivSecurityQuestionIcon,
            if (hasPassword) R.color.setting_icon_security_question else R.color.grey_1
        )
        setChevronEnabled(binding.ivSecurityQuestionChevron, hasPassword)

        // Fingerprint
        binding.tvFingerprintTitle.setTextColor(titleColor)
        binding.tvFingerprintDesc.setTextColor(descColor)
        setIconTint(
            binding.ivFingerprintIcon,
            if (hasPassword) R.color.setting_icon_fingerprint else R.color.grey_1
        )
        binding.switchFingerprint.alpha = if (hasPassword) 1f else 0.4f

        // Clear Password
        binding.tvClearPasswordTitle.setTextColor(titleColor)
        binding.tvClearPasswordDesc.setTextColor(descColor)
        setIconTint(
            binding.ivClearPasswordIcon,
            if (hasPassword) R.color.setting_icon_clear_password else R.color.grey_1
        )
        binding.ivClearPasswordIcon.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(
                    this,
                    if (hasPassword) R.color.setting_icon_clear_password_bg else R.color.white
                )
            )
        setChevronEnabled(binding.ivClearPasswordChevron, hasPassword)
    }

    private fun setIconTint(imageView: ImageView, colorRes: Int) {
        ImageViewCompat.setImageTintList(
            imageView,
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, colorRes))
        )
    }

    private fun setChevronEnabled(imageView: ImageView, enabled: Boolean) {
        ImageViewCompat.setImageTintList(
            imageView,
            if (enabled) {
                null
            } else {
                android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.grey_1)
                )
            }
        )
    }

    private fun showLockManagement() {
        LockManagementBottomSheet.show(
            supportFragmentManager,
            onChangePassword = {
                openWithSettingInterAd {
                    changePasswordLauncher.launch(
                        SetPasswordActivity.createSetIntent(this, -1L)
                    )
                }
            },
            onRemovePassword = {
                openWithSettingInterAd {
                    removePasswordLauncher.launch(
                        SetPasswordActivity.createVerifyIntent(this, -1L)
                    )
                }
            }
        )
    }

    private fun currentFontFamily(): String {
        val saved = preferenceHelper.getString(
            Constant.PREF_DEFAULT_FONT_FAMILY,
            Constant.DEFAULT_FONT_FAMILY
        ) ?: Constant.DEFAULT_FONT_FAMILY
        return if (RichTextFormats.fontStyles.any { it.id == saved }) {
            saved
        } else {
            Constant.DEFAULT_FONT_FAMILY
        }
    }

    private fun displayNameForFontFamily(familyId: String): String {
        if (familyId == Constant.DEFAULT_FONT_FAMILY) return "Default"
        return RichTextFormats.fontStyles.find { it.id == familyId }?.displayName ?: "Default"
    }

    private fun onFingerprintRowClicked() {
        // Already enabled â€” turn off without any prompt.
        if (passwordManager.isFingerprintEnabled()) {
            passwordManager.setFingerprintEnabled(false)
            binding.switchFingerprint.isChecked = false
            Toast.makeText(this, R.string.fingerprint_disabled, Toast.LENGTH_SHORT).show()
            return
        }

        if (!passwordManager.hasPassword()) {
            showFingerprintRequiresLockDialog()
            return
        }

        attemptEnableFingerprint()
    }

    private fun showFingerprintRequiresLockDialog() {
        NotesSelectionDialogs.showConfirmation(
            context = this,
            layoutInflater = layoutInflater,
            titleRes = R.string.fingerprint_requires_app_lock,
            messageRes = R.string.fingerprint_requires_app_lock_message,
            positiveButtonRes = R.string.action_continue,
            iconRes = R.drawable.ic_lock,
            onPositiveClick = {
                isFingerprintSetupFlow = true
                openWithSettingInterAd {
                    setPasswordLauncher.launch(
                        SetPasswordActivity.createSetIntent(this, -1L)
                    )
                }
            }
        )
    }

    private fun attemptEnableFingerprint() {
        val status = BiometricAuthHelper.availabilityStatus(this)
        when (status) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                pendingFingerprintEnrollment = true
                if (!BiometricAuthHelper.openEnrollmentSettings(this)) {
                    pendingFingerprintEnrollment = false
                    Toast.makeText(this, R.string.fingerprint_none_enrolled, Toast.LENGTH_LONG).show()
                }
            }

            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                Toast.makeText(this, R.string.fingerprint_not_available, Toast.LENGTH_LONG).show()
            }

            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                Toast.makeText(this, R.string.fingerprint_hw_unavailable, Toast.LENGTH_LONG).show()
            }

            else -> {
                // Fingerprint already set up on device â€” enable toggle without asking again.
                enableFingerprintWithoutPrompt()
            }
        }
    }

    private fun enableFingerprintWithoutPrompt() {
        passwordManager.setFingerprintEnabled(true)
        binding.switchFingerprint.isChecked = true
        Toast.makeText(this, R.string.fingerprint_enabled, Toast.LENGTH_SHORT).show()
    }

    private fun initViews() {
        binding.btnBack.setOnClickListener {
            finishWithSettingBackInterAd()
        }

        binding.layoutLanguage.setOnClickListener {
            openWithSettingInterAd {
                launchActivity<LanguageActivity> { }
            }
        }

        binding.layoutFontSize.setOnClickListener {
            val currentFontSize = preferenceHelper.getInt(
                Constant.PREF_DEFAULT_FONT_SIZE,
                Constant.DEFAULT_FONT_SIZE
            )
            FontSizeBottomSheet.show(supportFragmentManager, currentFontSize) { newSize ->
                preferenceHelper.putInt(Constant.PREF_DEFAULT_FONT_SIZE, newSize)
                updateSettingsUI()
            }
        }

        binding.layoutFontStyle.setOnClickListener {
            FontStyleBottomSheet.show(
                supportFragmentManager,
                currentFontFamily()
            ) { familyId ->
                preferenceHelper.putString(Constant.PREF_DEFAULT_FONT_FAMILY, familyId)
                updateSettingsUI()
            }
        }

        binding.layoutNoteBackground.setOnClickListener {
            val selectedKey = preferenceHelper
                .getString(Constant.PREF_DEFAULT_NOTE_BACKGROUND)
                ?.takeIf { it.isNotBlank() }
            openWithSettingInterAd {
                backgroundPickerLauncher.launch(
                    NoteBackgroundPickerActivity.createIntent(
                        context = this,
                        selectedKey = selectedKey,
                        saveAsDefault = true
                    )
                )
            }
        }

        binding.layoutItemHeight.setOnClickListener {
            val currentHeight = preferenceHelper.getInt(
                Constant.PREF_LIST_ITEM_HEIGHT,
                Constant.DEFAULT_ITEM_HEIGHT
            )
            ListItemHeightBottomSheet.show(supportFragmentManager, currentHeight) { newHeight ->
                preferenceHelper.putInt(Constant.PREF_LIST_ITEM_HEIGHT, newHeight)
                updateSettingsUI()
            }
        }

        binding.layoutTimeFormat.setOnClickListener {
            val currentTimeFormat = preferenceHelper.getString(
                Constant.PREF_TIME_FORMAT,
                Constant.TIME_FORMAT_12
            ) ?: Constant.TIME_FORMAT_12
            TimeFormatBottomSheet.show(supportFragmentManager, currentTimeFormat) { newFormat ->
                preferenceHelper.putString(Constant.PREF_TIME_FORMAT, newFormat)
                DateTimeUtils.setFormat(newFormat)
                updateSettingsUI()
            }
        }

        binding.layoutDateFormat.setOnClickListener {
            val currentDateFormat = preferenceHelper.getString(
                Constant.PREF_DATE_FORMAT,
                Constant.DEFAULT_DATE_FORMAT
            ) ?: Constant.DEFAULT_DATE_FORMAT
            com.notebook.notes.checklist.ui.note.DateFormatBottomSheet.show(
                supportFragmentManager,
                currentDateFormat
            ) { newFormat ->
                preferenceHelper.putString(Constant.PREF_DATE_FORMAT, newFormat)
                DateTimeUtils.setDateFormat(newFormat)
                updateSettingsUI()
            }
        }

        binding.layoutSetPassword.setOnClickListener {
            if (passwordManager.hasPassword()) {
                isResetFlow = true
                openWithSettingInterAd {
                    verifyPasswordLauncher.launch(
                        SetPasswordActivity.createVerifyIntent(this, -1L)
                    )
                }
            } else {
                isResetFlow = false
                openWithSettingInterAd {
                    setPasswordLauncher.launch(
                        SetPasswordActivity.createSetIntent(this, -1L)
                    )
                }
            }
        }

        binding.layoutSecurityQuestion.setOnClickListener {
            if (passwordManager.hasPassword()) {
                isSecurityQuestionFlow = true
                isResetFlow = false
                openWithSettingInterAd {
                    verifyPasswordLauncher.launch(
                        SetPasswordActivity.createVerifyIntent(this, -1L)
                    )
                }
            } else {
                Toast.makeText(this, "Set password first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutFingerprint.setOnClickListener {
            onFingerprintRowClicked()
        }

        binding.layoutClearPassword.setOnClickListener {
            if (passwordManager.hasPassword()) {
                openWithSettingInterAd {
                    removePasswordLauncher.launch(
                        SetPasswordActivity.createVerifyIntent(this, -1L)
                    )
                }
            } else {
                Toast.makeText(this, "No password set", Toast.LENGTH_SHORT).show()
            }
        }

        binding.layoutPrivacyPolicy.setOnClickListener {
            val url = ADSMainClass.getPrivacyPolicy().orEmpty().trim()
            if (url.isBlank()) return@setOnClickListener
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        binding.layoutCheckUpdate.setOnClickListener {
            checkForUpdate()
        }

        binding.layoutRateUs.setOnClickListener {
            com.notebook.notes.checklist.ui.dialog.RatingBottomSheet().apply {
                setOnRatingSubmittedListener { rating ->
                    if (rating >= 4) {
                        openPlayStore()
                    } else {
                        openWithSettingInterAd {
                            launchActivity<FeedbackActivity>()
                        }
                    }
                }
            }.show(supportFragmentManager, com.notebook.notes.checklist.ui.dialog.RatingBottomSheet.TAG)
        }

        binding.layoutFeedback.setOnClickListener {
            openWithSettingInterAd {
                launchActivity<FeedbackActivity>()
            }
        }
    }

    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                openPlayStore()
            }
        }
    }

    private fun openPlayStore() {
        val packageName = packageName

        try {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        } catch (e: Exception) {
            startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        }
    }

    private fun loadLanguageAds() {
        AdsManager.loadScreenAd(this, AdRemoteConfig.setting_screen)
    }
}
