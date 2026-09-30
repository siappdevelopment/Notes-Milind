package com.notebook.notes.checklist.ui.category

import android.app.Dialog
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.databinding.BottomSheetCreateLabelBinding
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.ui.adapter.LabelSuggestionAdapter
import com.notebook.notes.checklist.util.FlowLayoutManager
import com.notebook.notes.checklist.util.SystemBars

object CreateLabelBottomSheet {

    private val SUGGESTIONS = listOf(
        "Diary",
        "Study",
        "Recipes",
        "School",
        "Math",
        "Songs"
    )

    fun show(
        activity: AppCompatActivity,
        existingCategories: List<Category>,
        onLabelAdded: (Category) -> Unit,
        onCreateLabel: (String, (Category) -> Unit) -> Unit,
        categoryToEdit: Category? = null,
        onEditLabel: ((Long, String, (Category) -> Unit) -> Unit)? = null,
        rejectIfExists: Boolean = false
    ) {
        val editCategory = categoryToEdit
        val isEditMode = editCategory != null
        val binding = BottomSheetCreateLabelBinding.inflate(LayoutInflater.from(activity))

        binding.root.setBackgroundResource(R.drawable.bg_dialog_rounded)
        binding.tvTitle.setText(
            if (isEditMode) R.string.edit_label else R.string.create_new_label
        )
        binding.btnAdd.setText(
            if (isEditMode) R.string.edit_label_action else R.string.add_label
        )

        if (editCategory != null) {
            binding.etLabelName.setText(editCategory.name)
            binding.etLabelName.setSelection(editCategory.name.length)
            binding.rvSuggestions.visibility = View.GONE
        } else {
            val suggestionAdapter = LabelSuggestionAdapter { suggestion ->
                binding.etLabelName.setText(suggestion)
                binding.etLabelName.setSelection(suggestion.length)
            }
            binding.rvSuggestions.apply {
                layoutManager = FlowLayoutManager(activity)
                adapter = suggestionAdapter
                itemAnimator = null
            }
            suggestionAdapter.submitList(SUGGESTIONS)
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .create()
            .also { alert ->
                alert.window?.setBackgroundDrawableResource(android.R.color.transparent)
            }

        binding.btnCancel.setOnClickListener { dialog.dismiss() }

        val submitLabel = submitLabel@{
            val name = binding.etLabelName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) {
                Toast.makeText(activity, R.string.category_name_empty, Toast.LENGTH_SHORT).show()
                return@submitLabel
            }

            if (editCategory != null) {
                if (name.equals(editCategory.name, ignoreCase = true)) {
                    dialog.dismiss()
                    return@submitLabel
                }

                val duplicate = existingCategories.find {
                    it.id != editCategory.id &&
                        !it.isAllNotes &&
                        it.name.equals(name, ignoreCase = true)
                }
                if (duplicate != null) {
                    Toast.makeText(activity, R.string.category_already_exists, Toast.LENGTH_SHORT)
                        .show()
                    return@submitLabel
                }

                onEditLabel?.invoke(editCategory.id, name) {
                    onLabelAdded(it)
                    dialog.dismiss()
                }
            } else {
                val existing = existingCategories.find {
                    !it.isAllNotes && it.name.equals(name, ignoreCase = true)
                }
                if (existing != null) {
                    if (rejectIfExists) {
                        Toast.makeText(
                            activity,
                            R.string.category_already_exists,
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        onLabelAdded(existing)
                        dialog.dismiss()
                    }
                } else {
                    onCreateLabel(name) { category ->
                        onLabelAdded(category)
                        dialog.dismiss()
                    }
                }
            }
        }

        binding.btnAdd.setOnClickListener { submitLabel() }

        binding.etLabelName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                submitLabel()
                true
            } else {
                false
            }
        }

        dialog.setOnShowListener {
            SystemBars.hideNavigationBar(dialog)
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            val margin = (12 * activity.resources.displayMetrics.density).toInt()
            dialog.window?.setLayout(
                activity.resources.displayMetrics.widthPixels - margin * 2,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            binding.etLabelName.post {
                binding.etLabelName.requestFocus()
                showKeyboard(dialog, binding.etLabelName)
            }
        }
        dialog.setOnDismissListener {
            SystemBars.hideNavigationBar(activity)
        }
        dialog.show()
        SystemBars.hideNavigationBar(dialog)
    }

    private fun showKeyboard(dialog: Dialog, target: View) {
        val window = dialog.window ?: return
        target.requestFocus()
        WindowCompat.getInsetsController(window, target)
            .show(WindowInsetsCompat.Type.ime())
        val imm = target.context.getSystemService(InputMethodManager::class.java)
        imm?.showSoftInput(target, InputMethodManager.SHOW_IMPLICIT)
    }
}
