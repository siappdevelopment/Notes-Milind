package com.notebook.notes.checklist.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notebook.notes.checklist.model.Category
import com.notebook.notes.checklist.model.CategoryWithCount
import com.notebook.notes.checklist.repository.CategoryRepository
import com.notebook.notes.checklist.util.CategoryIconResolver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryUiState(
    val categories: List<Category> = emptyList(),
    val categoriesWithCount: List<CategoryWithCount> = emptyList(),
    val selectedCategoryId: Long? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val repository: CategoryRepository
) : ViewModel() {

    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<CategoryUiState> = combine(
        repository.getAllCategories(),
        repository.getCategoriesWithCount(),
        repository.selectedFilterCategoryId,
        _isLoading,
        _error
    ) { categories, categoriesWithCount, selectedId, isLoading, error ->
        CategoryUiState(
            categories = categories,
            categoriesWithCount = categoriesWithCount,
            selectedCategoryId = selectedId,
            isLoading = isLoading,
            error = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CategoryUiState())

    init {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
            _isLoading.value = false
        }
    }

    fun selectCategory(categoryId: Long) {
        repository.selectFilterCategory(categoryId)
    }

    fun createCategory(name: String, onSuccess: (Category) -> Unit = {}) {
        viewModelScope.launch {
            repository.createCategory(name, CategoryIconResolver.iconNameForCategory(name))
                .onSuccess { category ->
                    _error.value = null
                    onSuccess(category)
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun updateCategory(categoryId: Long, newName: String, onSuccess: (Category) -> Unit = {}) {
        viewModelScope.launch {
            repository.updateCategory(categoryId, newName)
                .onSuccess { category ->
                    _error.value = null
                    onSuccess(category)
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun deleteCategory(categoryId: Long, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteCategory(categoryId)
                .onSuccess {
                    _error.value = null
                    onSuccess()
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun lockCategoryNotes(categoryId: Long, onSuccess: (Int) -> Unit = {}) {
        viewModelScope.launch {
            repository.lockCategoryNotes(categoryId)
                .onSuccess { lockedCount ->
                    _error.value = null
                    onSuccess(lockedCount)
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun unlockCategoryNotes(categoryId: Long, onSuccess: (Int) -> Unit = {}) {
        viewModelScope.launch {
            repository.unlockCategoryNotes(categoryId)
                .onSuccess { unlockedCount ->
                    _error.value = null
                    onSuccess(unlockedCount)
                }
                .onFailure { _error.value = it.message }
        }
    }

    fun areCategoryNotesLocked(categoryId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(repository.areCategoryNotesLocked(categoryId))
        }
    }

    fun clearError() {
        _error.value = null
    }
}
