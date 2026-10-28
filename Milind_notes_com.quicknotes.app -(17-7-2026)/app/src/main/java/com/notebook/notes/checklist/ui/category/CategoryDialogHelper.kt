package com.notebook.notes.checklist.ui.category

import android.content.Context
import android.view.LayoutInflater
import android.widget.EditText
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.ui.home.NotesSelectionDialogs

object CategoryDialogHelper {

    fun showSelectionDialog(
        context: Context,
        categories: List<Category>,
        selectedCategoryId: Long,
        onCategorySelected: (Category) -> Unit,
        onCreateCategory: () -> Unit,
        onRenameCategory: (Category) -> Unit,
        onDeleteCategory: (Category) -> Unit
    ) {
        val selectableCategories = categories.filter { !it.isAllNotes }
        val names = selectableCategories.map { category ->
            if (category.id == selectedCategoryId) "✓ ${category.name}" else category.name
        }.toMutableList()
        names.add(context.getString(R.string.create_category))

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.select_category)
            .setItems(names.toTypedArray()) { d, which ->
                if (which == names.lastIndex) {
                    onCreateCategory()
                } else {
                    onCategorySelected(selectableCategories[which])
                }
                d.dismiss()
            }
            .create()

        dialog.show()
        dialog.listView?.setOnItemLongClickListener { _, _, position, _ ->
            if (position >= selectableCategories.size) return@setOnItemLongClickListener false
            val category = selectableCategories[position]
            showManageCategoryDialog(context, category, onRenameCategory, onDeleteCategory)
            true
        }
    }

    private fun showManageCategoryDialog(
        context: Context,
        category: Category,
        onRenameCategory: (Category) -> Unit,
        onDeleteCategory: (Category) -> Unit
    ) {
        val options = buildList {
            if (category.canRename) add(context.getString(R.string.rename_category))
            if (category.canDelete) add(context.getString(R.string.delete_category))
        }
        if (options.isEmpty()) return

        MaterialAlertDialogBuilder(context)
            .setTitle(category.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    context.getString(R.string.rename_category) -> onRenameCategory(category)
                    context.getString(R.string.delete_category) -> {
                        showDeleteCategoryDialog(context) {
                            onDeleteCategory(category)
                        }
                    }
                }
            }
            .show()
    }

    fun showEditCategoryDialog(
        context: Context,
        initialName: String? = null,
        onSave: (String) -> Unit
    ) {
        val title = if (initialName == null) {
            context.getString(R.string.create_category)
        } else {
            context.getString(R.string.rename_category)
        }
        showNameInputDialog(
            context = context,
            title = title,
            initialName = initialName.orEmpty(),
            onSave = onSave
        )
    }

    fun showCreateCategoryDialog(
        context: Context,
        onSave: (String) -> Unit
    ) {
        showEditCategoryDialog(context, null, onSave)
    }

    fun showUpdateCategoryDialog(
        context: Context,
        category: Category,
        onSave: (String) -> Unit
    ) {
        showEditCategoryDialog(context, category.name, onSave)
    }

    fun showDeleteCategoryDialog(
        context: Context,
        onConfirm: () -> Unit
    ) {
        NotesSelectionDialogs.showConfirmation(
            context = context,
            layoutInflater = LayoutInflater.from(context),
            titleRes = R.string.delete_category_title,
            messageRes = R.string.delete_category_message,
            positiveButtonRes = R.string.delete_category,
            iconRes = R.drawable.ic_delete,
            onPositiveClick = onConfirm
        )
    }

    private fun showNameInputDialog(
        context: Context,
        title: String,
        initialName: String,
        onSave: (String) -> Unit
    ) {
        val input = LayoutInflater.from(context)
            .inflate(R.layout.dialog_category_name_input, null, false) as EditText
        input.setText(initialName)
        input.setSelection(initialName.length)

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(input)
            .setPositiveButton(R.string.save) { _, _ ->
                onSave(input.text?.toString().orEmpty())
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
