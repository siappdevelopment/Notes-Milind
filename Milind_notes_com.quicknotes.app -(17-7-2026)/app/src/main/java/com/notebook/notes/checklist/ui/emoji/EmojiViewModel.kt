package com.notebook.notes.checklist.ui.emoji

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class EmojiViewModel @Inject constructor(
    private val repository: EmojiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmojiUiState())
    val uiState: StateFlow<EmojiUiState> = _uiState.asStateFlow()

    init {
        selectCategory(repository.getDefaultCategoryId())
    }

    fun selectCategory(categoryId: String) {
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                emojis = repository.getEmojisForCategory(categoryId)
            )
        }
    }

    fun onEmojiSelected(emoji: String): String {
        repository.addRecent(emoji)
        if (_uiState.value.selectedCategoryId == EmojiCategory.RECENT.id) {
            selectCategory(EmojiCategory.RECENT.id)
        }
        return emoji
    }
}
