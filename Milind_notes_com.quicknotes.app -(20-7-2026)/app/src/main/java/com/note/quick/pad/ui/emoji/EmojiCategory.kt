package com.note.quick.pad.ui.emoji

import androidx.annotation.DrawableRes
import com.note.quick.pad.R

enum class EmojiCategory(
    val id: String,
    @DrawableRes val tabIconRes: Int
) {
    RECENT("recent", R.drawable.ic_recent),
    SMILEYS("smileys", R.drawable.ic_em_emoji),
    FLOWER("flower", R.drawable.ic_em_flover),
    FOOD("food", R.drawable.ic_em_burger),
    TRAVEL("travel", R.drawable.ic_em_plane),
    BOOK("book", R.drawable.ic_em_book),
    MUSIC("music", R.drawable.ic_em_music);

    companion object {
        val displayCategories: List<EmojiCategory> = entries

        fun fromId(id: String): EmojiCategory? = entries.find { it.id == id }
    }
}
