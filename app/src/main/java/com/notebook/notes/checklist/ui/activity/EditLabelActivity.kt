package com.notebook.notes.checklist.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.notebook.notes.checklist.ads.AdRemoteConfig
import com.notebook.notes.checklist.ads.AdsManager
import com.notebook.notes.checklist.ads.loadScreenAd
import com.notebook.notes.checklist.ads.other_screen
import com.notebook.notes.checklist.Advertisement.ADSConsentManager
import com.notebook.notes.checklist.Advertisement.ADSMainClass
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.ActivityEditLabelBinding
import com.notebook.notes.checklist.helper.NotePasswordManager
import com.notebook.notes.checklist.iap.PremiumFeature
import com.notebook.notes.checklist.iap.PremiumFeatureGate
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.model.CategoryWithCount
import com.notebook.notes.checklist.ui.adapter.EditLabelAdapter
import com.notebook.notes.checklist.ui.category.CategoryDialogHelper
import com.notebook.notes.checklist.ui.category.CreateLabelBottomSheet
import com.notebook.notes.checklist.ui.category.EditLabelOptionsPopup
import com.notebook.notes.checklist.viewmodel.CategoryViewModel
import com.notebook.notes.checklist.viewmodel.utils.finishWithEditLabelBackInterAd
import com.notebook.notes.checklist.viewmodel.utils.openWithEditLabelInterAd
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class EditLabelActivity : BaseActivity() {

    @Inject lateinit var notePasswordManager: NotePasswordManager

    private lateinit var binding: ActivityEditLabelBinding
    private val viewModel: CategoryViewModel by viewModels()
    private lateinit var adapter: EditLabelAdapter
    private var pendingLockCategoryId: Long = -1L
    private var pendingUnlockCategoryId: Long = -1L

    private val setPasswordLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val categoryId = pendingLockCategoryId
            pendingLockCategoryId = -1L
            if (result.resultCode != RESULT_OK || categoryId <= 0L) return@registerForActivityResult
            lockCategoryNotes(categoryId)
        }

    private val verifyPasswordLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val categoryId = pendingUnlockCategoryId
            pendingUnlockCategoryId = -1L
            if (result.resultCode != RESULT_OK || categoryId <= 0L) return@registerForActivityResult
            unlockCategoryNotes(categoryId)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditLabelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupWindowInsets()
        setupToolbar()
        setupBackPress()
        setupRecyclerView()
        setupAddButton()
        observeUiState()
        loadOtherScreenAds()
    }

    private fun loadOtherScreenAds() {
        AdsManager.loadScreenAd(this, AdRemoteConfig.other_screen)
    }


    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val topPad = systemBars.top + (12 * resources.displayMetrics.density).toInt()
            binding.clTopbar.updatePadding(top = topPad)
            // Keep bottom inset off btnAdd (fixed height); otherwise popup-triggered nav insets clip its text.
            binding.llOtherScreenAds.root.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            finishWithEditLabelBackInterAd()
        }
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishWithEditLabelBackInterAd()
            }
        })
    }

    private fun setupAddButton() {
        binding.btnAdd.setOnClickListener {
            showCreateLabelSheet()
        }
    }

    private fun showCreateLabelSheet() {
        CreateLabelBottomSheet.show(
            activity = this,
            existingCategories = viewModel.uiState.value.categories,
            onLabelAdded = { },
            onCreateLabel = { name, onSuccess ->
                viewModel.createCategory(name, onSuccess)
            },
            rejectIfExists = true
        )
    }

    private fun setupRecyclerView() {
        adapter = EditLabelAdapter { view, category ->
            showOptionsPopup(view, category)
        }
        binding.rvLabels.adapter = adapter
    }

    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.categoriesWithCount)
                    state.error?.let { message ->
                        Toast.makeText(this@EditLabelActivity, message, Toast.LENGTH_SHORT).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }

    private fun showOptionsPopup(view: View, category: CategoryWithCount) {
        viewModel.areCategoryNotesLocked(category.id) { areNotesLocked ->
            EditLabelOptionsPopup.show(
                context = this,
                anchor = view,
                category = category,
                areNotesLocked = areNotesLocked,
                onRename = { selected ->
                    // Wait for the options popup to dismiss so the centered dialog attaches cleanly.
                    binding.root.post { showRenameSheet(selected) }
                },
                onDelete = { selected ->
                    binding.root.post { showDeleteConfirmation(selected) }
                },
                onLockToggle = { selected, locked ->
                    if (locked) {
                        unlockCategory(selected)
                    } else {
                        lockCategory(selected)
                    }
                }
            )
        }
    }

    private fun lockCategory(category: CategoryWithCount) {
        if (category.count <= 0) {
            Toast.makeText(this, R.string.category_no_notes_to_lock, Toast.LENGTH_SHORT).show()
            return
        }

        PremiumFeatureGate.requirePremium(this, PremiumFeature.LOCK_NOTE) {
            if (notePasswordManager.hasPassword()) {
                lockCategoryNotes(category.id)
            } else {
                pendingLockCategoryId = category.id
                openWithEditLabelInterAd {
                    setPasswordLauncher.launch(SetPasswordActivity.createSetIntent(this, -1L))
                }
            }
        }
    }

    private fun unlockCategory(category: CategoryWithCount) {
        if (!notePasswordManager.hasPassword()) {
            unlockCategoryNotes(category.id)
            return
        }

        pendingUnlockCategoryId = category.id
        openWithEditLabelInterAd {
            verifyPasswordLauncher.launch(
                SetPasswordActivity.createVerifyIntent(this, -1L)
            )
        }
    }

    private fun lockCategoryNotes(categoryId: Long) {
        viewModel.lockCategoryNotes(categoryId) { lockedCount ->
            val messageRes = if (lockedCount > 0) {
                R.string.category_notes_locked
            } else {
                R.string.category_no_notes_to_lock
            }
            Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
        }
    }

    private fun unlockCategoryNotes(categoryId: Long) {
        viewModel.unlockCategoryNotes(categoryId) {
            Toast.makeText(this, R.string.category_notes_unlocked, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRenameSheet(category: CategoryWithCount) {
        val categories = viewModel.uiState.value.categories
        val categoryToEdit = categories.find { it.id == category.id }
            ?: Category(
                id = category.id,
                name = category.name,
                icon = category.icon,
                isDefault = category.isDefault
            )

        CreateLabelBottomSheet.show(
            activity = this,
            existingCategories = categories,
            onLabelAdded = { },
            onCreateLabel = { _, _ -> },
            categoryToEdit = categoryToEdit,
            onEditLabel = { id, newName, onSuccess ->
                viewModel.updateCategory(id, newName, onSuccess)
            }
        )
    }

    private fun showDeleteConfirmation(category: CategoryWithCount) {
        CategoryDialogHelper.showDeleteCategoryDialog(this) {
            viewModel.deleteCategory(category.id)
        }
    }
}
