package com.notebook.notes.checklist.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.notebook.notes.checklist.Advertisement.ADSBannerSmall
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.Advertisement.ADSNativeDisplay
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivitySetPasswordBinding
import com.notebook.notes.checklist.helper.BiometricAuthHelper
import com.notebook.notes.checklist.helper.NotePasswordManager
import com.notebook.notes.checklist.repository.NoteRepository
import com.notebook.notes.checklist.ui.password.PatternLockView
import com.notebook.notes.checklist.ui.password.SecurityQuestionPopup
import com.notebook.notes.checklist.viewmodel.NoteEditViewModel
import com.notebook.notes.checklist.viewmodel.utils.finishWithSetPasswordBackInterAd
import com.notebook.notes.checklist.viewmodel.utils.openWithSetPasswordInterAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SetPasswordActivity : BaseActivity() {

    @Inject lateinit var passwordManager: NotePasswordManager
    @Inject lateinit var noteRepository: NoteRepository

    private lateinit var binding: ActivitySetPasswordBinding

    private var mode = MODE_SET
    private var noteId = -1L
    private var openNoteOnSuccess = false
    private var isPatternMode = true
    private var awaitingConfirm = false
    private var firstPattern: List<Int>? = null
    private var firstPin: String? = null
    private val pinBuilder = StringBuilder()
    private var showingSecurityQuestion = false
    private var recoveringWithSecurityQuestion = false

    private val pinDots by lazy {
        listOf(binding.pinDot1, binding.pinDot2, binding.pinDot3, binding.pinDot4)
    }

    private val securityQuestions by lazy {
        resources.getStringArray(R.array.security_questions).toList()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_SET
        noteId = intent.getLongExtra(EXTRA_NOTE_ID, -1L)
        openNoteOnSuccess = intent.getBooleanExtra(EXTRA_OPEN_NOTE_ON_SUCCESS, false)

        if (mode == MODE_SECURITY_QUESTION) {
            showSecurityQuestionSetup()
        } else if (mode == MODE_VERIFY) {
            binding.tvTitle.setText(R.string.enter_password)
            binding.tvHeadline.setText(R.string.authenticate_to_open)
            when (passwordManager.getPasswordType()) {
                NotePasswordManager.PasswordType.PIN -> {
                    isPatternMode = false
                    binding.tvSubtitle.setText(R.string.enter_number_password)
                }
                else -> {
                    isPatternMode = true
                    binding.tvSubtitle.setText(R.string.draw_a_pattern)
                }
            }
            binding.btnForgotPassword.isVisible = passwordManager.hasSecurityQuestion()
        } else if (mode == MODE_SET && passwordManager.hasPassword()) {
            binding.tvTitle.setText(R.string.reset_password)
        }

        setupSecurityQuestionUi()
        applyInputMode()
        setupClicks()
        setupBackPress()

        if (mode == MODE_VERIFY) {
            maybeStartFingerprintPrompt()
        }
        loadOtherScreenAds()
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    recoveringWithSecurityQuestion -> showPasswordStepFromRecovery()
                    showingSecurityQuestion -> finishPasswordFlow()
                    else -> finishWithSetPasswordBackInterAd()
                }
            }
        })
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
        findViewById<android.view.View>(R.id.shimmer_container_banner).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.flNativeSmallPlaceholder).visibility = android.view.View.GONE
        findViewById<android.view.View>(R.id.flBannerSmallPlaceholder).visibility = android.view.View.GONE
    }

    private fun maybeStartFingerprintPrompt() {
        if (!passwordManager.isFingerprintEnabled()) return
        if (!BiometricAuthHelper.canAuthenticate(this)) return

        binding.root.post {
            if (isFinishing || showingSecurityQuestion || recoveringWithSecurityQuestion) return@post
            BiometricAuthHelper.showPrompt(
                activity = this,
                title = getString(R.string.unlock_with_fingerprint),
                subtitle = getString(R.string.unlock_app_using_fingerprint_scan),
                negativeButtonText = getString(R.string.use_password),
                onSuccess = { onUnlocked() }
            )
        }
    }

    private fun setupSecurityQuestionUi() {
        binding.tvSecurityQuestion.text = securityQuestions.firstOrNull().orEmpty()
        binding.layoutSecurityQuestion.setOnClickListener {
            SecurityQuestionPopup.show(
                this,
                binding.layoutSecurityQuestion,
                securityQuestions
            ) { question ->
                binding.tvSecurityQuestion.text = question
            }
        }
        binding.etSecurityAnswer.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onSecurityQuestionSave()
                true
            } else {
                false
            }
        }
    }

    private fun setupClicks() {
        binding.btnBack.setOnClickListener {
            when {
                recoveringWithSecurityQuestion -> showPasswordStepFromRecovery()
                showingSecurityQuestion -> finishPasswordFlow()
                else -> finishWithSetPasswordBackInterAd()
            }
        }

        binding.btnSwitchMode.setOnClickListener {
            if (mode == MODE_VERIFY) return@setOnClickListener
            isPatternMode = !isPatternMode
            awaitingConfirm = false
            firstPattern = null
            firstPin = null
            pinBuilder.clear()
            updatePinDots()
            binding.patternLockView.clearPattern()
            applyInputMode()
        }

        binding.btnForgotPassword.setOnClickListener {
            showSecurityQuestionRecovery()
        }

        binding.btnSaveSecurityQuestion.setOnClickListener { onSecurityQuestionSave() }
        binding.btnSkipSecurityQuestion.setOnClickListener {
            if (recoveringWithSecurityQuestion) {
                showPasswordStepFromRecovery()
            } else {
                finishPasswordFlow()
            }
        }

        binding.patternLockView.listener = object : PatternLockView.Listener {
            override fun onPatternComplete(pattern: List<Int>) {
                handlePattern(pattern)
            }
        }

        listOf(
            binding.key0 to "0",
            binding.key1 to "1",
            binding.key2 to "2",
            binding.key3 to "3",
            binding.key4 to "4",
            binding.key5 to "5",
            binding.key6 to "6",
            binding.key7 to "7",
            binding.key8 to "8",
            binding.key9 to "9"
        ).forEach { (view, digit) ->
            view.setOnClickListener { onPinDigit(digit) }
        }
        binding.keyDelete.setOnClickListener {
            if (pinBuilder.isNotEmpty()) {
                pinBuilder.deleteCharAt(pinBuilder.length - 1)
                updatePinDots()
            }
        }
    }

    private fun applyInputMode() {
        binding.patternLockView.isVisible = isPatternMode
        binding.pinContainer.isVisible = !isPatternMode

        if (mode == MODE_VERIFY) {
            binding.btnSwitchMode.isVisible = false
            binding.btnForgotPassword.isVisible =
                passwordManager.hasSecurityQuestion() && !recoveringWithSecurityQuestion
            return
        }

        binding.btnSwitchMode.isVisible = true
        binding.btnForgotPassword.isVisible = false
        if (isPatternMode) {
            binding.ivSwitchMode.setImageResource(R.drawable.ic_number_password)
            binding.tvSwitchMode.setText(R.string.number_password)
            binding.btnSwitchMode.contentDescription = getString(R.string.cd_number_password)
            binding.tvSubtitle.setText(
                if (awaitingConfirm) R.string.draw_pattern_again else R.string.draw_a_pattern
            )
        } else {
            binding.ivSwitchMode.setImageResource(R.drawable.ic_pattern_password)
            binding.tvSwitchMode.setText(R.string.pattern_password)
            binding.btnSwitchMode.contentDescription = getString(R.string.cd_pattern_password)
            binding.tvSubtitle.setText(
                if (awaitingConfirm) R.string.confirm_number_password else R.string.enter_number_password
            )
        }
    }

    private fun handlePattern(pattern: List<Int>) {
        if (pattern.size < NotePasswordManager.MIN_PATTERN_LENGTH) {
            Toast.makeText(this, R.string.pattern_too_short, Toast.LENGTH_SHORT).show()
            binding.patternLockView.clearPattern(withError = true)
            return
        }

        when (mode) {
            MODE_VERIFY -> {
                if (passwordManager.verifyPattern(pattern)) {
                    onUnlocked()
                } else {
                    Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show()
                    binding.patternLockView.clearPattern(withError = true)
                }
            }
            else -> {
                if (!awaitingConfirm) {
                    firstPattern = pattern
                    awaitingConfirm = true
                    binding.tvSubtitle.setText(R.string.draw_pattern_again)
                    binding.patternLockView.clearPattern()
                } else if (pattern == firstPattern) {
                    passwordManager.savePattern(pattern)
                    onPasswordSet()
                } else {
                    Toast.makeText(this, R.string.pattern_mismatch, Toast.LENGTH_SHORT).show()
                    awaitingConfirm = false
                    firstPattern = null
                    binding.tvSubtitle.setText(R.string.draw_a_pattern)
                    binding.patternLockView.clearPattern(withError = true)
                }
            }
        }
    }

    private fun onPinDigit(digit: String) {
        if (pinBuilder.length >= NotePasswordManager.PIN_LENGTH) return
        pinBuilder.append(digit)
        updatePinDots()
        if (pinBuilder.length == NotePasswordManager.PIN_LENGTH) {
            handlePin(pinBuilder.toString())
        }
    }

    private fun handlePin(pin: String) {
        when (mode) {
            MODE_VERIFY -> {
                if (passwordManager.verifyPin(pin)) {
                    onUnlocked()
                } else {
                    Toast.makeText(this, R.string.wrong_password, Toast.LENGTH_SHORT).show()
                    pinBuilder.clear()
                    updatePinDots()
                }
            }
            else -> {
                if (!awaitingConfirm) {
                    firstPin = pin
                    awaitingConfirm = true
                    pinBuilder.clear()
                    updatePinDots()
                    binding.tvSubtitle.setText(R.string.confirm_number_password)
                } else if (pin == firstPin) {
                    passwordManager.savePin(pin)
                    onPasswordSet()
                } else {
                    Toast.makeText(this, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
                    awaitingConfirm = false
                    firstPin = null
                    pinBuilder.clear()
                    updatePinDots()
                    binding.tvSubtitle.setText(R.string.enter_number_password)
                }
            }
        }
    }

    private fun updatePinDots() {
        pinDots.forEachIndexed { index, view ->
            view.setBackgroundResource(
                if (index < pinBuilder.length) R.drawable.bg_pin_dot_filled
                else R.drawable.bg_pin_dot_empty
            )
        }
    }

    private fun onPasswordSet() {
        if (passwordManager.hasSecurityQuestion()) {
            finishPasswordFlow()
        } else {
            showSecurityQuestionSetup()
        }
    }

    private fun showSecurityQuestionSetup() {
        showingSecurityQuestion = true
        recoveringWithSecurityQuestion = false
        binding.tvTitle.text = if (passwordManager.hasSecurityQuestion()) {
            getString(R.string.reset_security_question)
        } else {
            getString(R.string.set_security_question)
        }
        binding.passwordStep.isVisible = false
        binding.securityQuestionStep.isVisible = true
        binding.btnSkipSecurityQuestion.isVisible = true
        binding.btnSkipSecurityQuestion.setText(R.string.skip)
        binding.tvSecurityHeadline.setText(R.string.choose_security_question)
        binding.layoutSecurityQuestion.isEnabled = true
        binding.tvSecurityQuestion.text = securityQuestions.firstOrNull().orEmpty()
        binding.etSecurityAnswer.setText("")
        binding.etSecurityAnswer.hint = getString(R.string.input_your_answer)
    }

    private fun showSecurityQuestionRecovery() {
        val question = passwordManager.getSecurityQuestion()
        if (question.isNullOrBlank()) {
            Toast.makeText(this, R.string.no_security_question, Toast.LENGTH_SHORT).show()
            return
        }
        showingSecurityQuestion = true
        recoveringWithSecurityQuestion = true
        binding.tvTitle.setText(R.string.forgot_password)
        binding.passwordStep.isVisible = false
        binding.securityQuestionStep.isVisible = true
        binding.btnSkipSecurityQuestion.isVisible = true
        binding.btnSkipSecurityQuestion.setText(R.string.cancel)
        binding.tvSecurityHeadline.setText(R.string.verify_security_answer)
        binding.layoutSecurityQuestion.isEnabled = false
        binding.tvSecurityQuestion.text = question
        binding.etSecurityAnswer.setText("")
        binding.etSecurityAnswer.hint = getString(R.string.input_your_answer)
        binding.etSecurityAnswer.requestFocus()
    }

    private fun showPasswordStepFromRecovery() {
        showingSecurityQuestion = false
        recoveringWithSecurityQuestion = false
        binding.securityQuestionStep.isVisible = false
        binding.passwordStep.isVisible = true
        binding.tvTitle.setText(R.string.enter_password)
        binding.etSecurityAnswer.setText("")
        applyInputMode()
    }

    private fun onSecurityQuestionSave() {
        val answer = binding.etSecurityAnswer.text?.toString().orEmpty().trim()
        if (answer.isEmpty()) {
            Toast.makeText(this, R.string.security_answer_required, Toast.LENGTH_SHORT).show()
            return
        }

        if (recoveringWithSecurityQuestion) {
            if (passwordManager.verifySecurityAnswer(answer)) {
                onUnlocked()
            } else {
                Toast.makeText(this, R.string.wrong_security_answer, Toast.LENGTH_SHORT).show()
                binding.etSecurityAnswer.setText("")
            }
            return
        }

        val question = binding.tvSecurityQuestion.text?.toString().orEmpty().trim()
            .ifBlank { securityQuestions.firstOrNull().orEmpty() }
        if (question.isBlank()) return

        passwordManager.saveSecurityQuestion(question, answer)
        Toast.makeText(this, R.string.security_question_saved, Toast.LENGTH_SHORT).show()
        finishPasswordFlow()
    }

    private fun finishPasswordFlow() {
        if (noteId > 0L) {
            lifecycleScope.launch {
                noteRepository.setLocked(noteId, true)
                Toast.makeText(
                    this@SetPasswordActivity,
                    R.string.password_set_success,
                    Toast.LENGTH_SHORT
                ).show()
                setResult(RESULT_OK, Intent().putExtra(EXTRA_NOTE_ID, noteId))
                finish()
            }
        } else {
            // noteId is 0 / unset (e.g. set password from Settings — no note locked)
            if (mode != MODE_SECURITY_QUESTION) {
                Toast.makeText(this, R.string.app_password_set_success, Toast.LENGTH_SHORT).show()
            }
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun onUnlocked() {
        if (openNoteOnSuccess && noteId > 0L) {
            openWithSetPasswordInterAd {
                startActivity(
                    Intent(this, CreateNotesActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(NoteEditViewModel.EXTRA_NOTE_ID, noteId)
                        putExtra(NoteEditViewModel.EXTRA_LOCK_AUTHENTICATED, true)
                    }
                )
                finish()
            }
            return
        }
        setResult(RESULT_OK, Intent().putExtra(EXTRA_NOTE_ID, noteId))
        finish()
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_NOTE_ID = "extra_note_id"
        const val EXTRA_OPEN_NOTE_ON_SUCCESS = "extra_open_note_on_success"
        const val MODE_SET = "set"
        const val MODE_VERIFY = "verify"
        const val MODE_SECURITY_QUESTION = "security_question"

        fun createSetIntent(context: Context, noteId: Long): Intent =
            Intent(context, SetPasswordActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_SET)
                putExtra(EXTRA_NOTE_ID, noteId)
            }

        fun createVerifyIntent(context: Context, noteId: Long): Intent =
            Intent(context, SetPasswordActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_VERIFY)
                putExtra(EXTRA_NOTE_ID, noteId)
            }

        fun createVerifyAndOpenNoteIntent(context: Context, noteId: Long): Intent =
            Intent(context, SetPasswordActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_MODE, MODE_VERIFY)
                putExtra(EXTRA_NOTE_ID, noteId)
                putExtra(EXTRA_OPEN_NOTE_ON_SUCCESS, true)
            }

        fun createSecurityQuestionIntent(context: Context): Intent =
            Intent(context, SetPasswordActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_SECURITY_QUESTION)
            }
    }
}
