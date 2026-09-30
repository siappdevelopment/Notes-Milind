package com.note.quick.pad.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.note.quick.pad.model.Category
import com.note.quick.pad.repository.CategoryRepository
import com.note.quick.pad.util.CategoryIconResolver
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
        repository.selectedFilterCategoryId,
        _isLoading,
        _error
    ) { categories, selectedId, isLoading, error ->
        CategoryUiState(
            categories = categories,
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

    fun clearError() {
        _error.value = null
    }
}
