package com.notebook.notes.checklist.ui.emoji

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.BreakIterator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmojiRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val allItems: List<EmojiItem> by lazy { buildCatalog() }
    private val itemsByCategory: Map<String, List<EmojiItem>> by lazy {
        allItems.groupBy { it.categoryId }
    }

    fun getCategories(): List<EmojiCategory> = EmojiCategory.displayCategories

    fun getEmojisForCategory(categoryId: String): List<EmojiItem> {
        return when (categoryId) {
            EmojiCategory.RECENT.id -> loadRecent().mapNotNull { emoji ->
                val visible = normalizeEmoji(emoji) ?: return@mapNotNull null
                allItems.find { it.emoji == visible }
                    ?: EmojiItem(
                        id = "recent:$visible",
                        emoji = visible,
                        categoryId = EmojiCategory.RECENT.id
                    )
            }
            else -> itemsByCategory[categoryId].orEmpty()
        }
    }

    fun getDefaultCategoryId(): String {
        return if (loadRecent().isEmpty()) {
            EmojiCategory.SMILEYS.id
        } else {
            EmojiCategory.RECENT.id
        }
    }

    fun addRecent(emoji: String) {
        val normalized = normalizeEmoji(emoji) ?: return
        val updated = buildList {
            add(normalized)
            loadRecent().forEach { existing ->
                if (existing != normalized && size < MAX_RECENT) add(existing)
            }
        }
        prefs.edit()
            .putString(KEY_RECENT, updated.joinToString(DELIMITER))
            .apply()
    }

    fun loadRecent(): List<String> {
        val stored = prefs.getString(KEY_RECENT, null)
            ?.split(DELIMITER)
            .orEmpty()
        val cleaned = stored.mapNotNull { normalizeEmoji(it) }
        if (cleaned != stored) {
            persistRecent(cleaned)
        }
        return cleaned
    }

    private fun persistRecent(emojis: List<String>) {
        prefs.edit()
            .putString(KEY_RECENT, emojis.joinToString(DELIMITER))
            .apply()
    }

    private fun normalizeEmoji(value: String): String? {
        val grapheme = splitGraphemes(value).firstOrNull { hasVisibleEmojiContent(it) }
        return grapheme?.takeIf { hasVisibleEmojiContent(it) }
    }

    private fun buildCatalog(): List<EmojiItem> {
        val items = mutableListOf<EmojiItem>()
        var nextId = 0
        categoryStrings.forEach { (category, value) ->
            splitGraphemes(value).forEach { emoji ->
                items += EmojiItem(
                    id = "${category.id}:${nextId++}",
                    emoji = emoji,
                    categoryId = category.id
                )
            }
        }
        return items
    }

    private val categoryStrings: List<Pair<EmojiCategory, String>> = listOf(
        EmojiCategory.SMILEYS to SMILEYS,
        EmojiCategory.FLOWER to FLOWER,
        EmojiCategory.FOOD to FOOD,
        EmojiCategory.TRAVEL to TRAVEL,
        EmojiCategory.BOOK to BOOK,
        EmojiCategory.MUSIC to MUSIC
    )

    private fun splitGraphemes(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        val result = LinkedHashSet<String>()
        val iterator = BreakIterator.getCharacterInstance()
        iterator.setText(value)
        var start = iterator.first()
        var end = iterator.next()
        while (end != BreakIterator.DONE) {
            val grapheme = value.substring(start, end)
            if (hasVisibleEmojiContent(grapheme)) {
                result.add(grapheme)
            }
            start = end
            end = iterator.next()
        }
        return result.toList()
    }

    private fun hasVisibleEmojiContent(grapheme: String): Boolean {
        if (grapheme.isBlank()) return false
        return grapheme.codePoints().anyMatch { codePoint ->
            when (codePoint) {
                0x200D,
                0x200C -> false
                in 0xFE00..0xFE0F -> false
                in 0x1F3FB..0x1F3FF -> false
                in 0xE0020..0xE007F -> false
                else -> true
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "emoji_recent_store"
        private const val KEY_RECENT = "recent_emojis"
        private const val DELIMITER = "\u0001"
        private const val MAX_RECENT = 30

        private const val SMILEYS =
            "😀😃😄😁😆😅🤣😂🙂🙃😉😊😇🥰😍🤩😘😗☺️😚😙😋😛😜🤪😝🤑🤗🤭🤫🤔🤐🤨😐😑😶😏😒🙄😬🤥😌😔😪🤤😴😷🤒🤕🤢🤮🤧🥵🥶🥴😵🤯🤠🥳🥸😎🤓🧐😕😟🙁☹️😮😯😲😳🥺😦😧😨😰😥😢😭😱😖😣😞😓😩😫🥱😤😡😠🤬😈👿💀☠️💩🤡👹👺👻👽👾🤖😺😸😹😻😼😽🙀😿😾"

        private const val FLOWER =
            "🌱🌿☘️🍀🎍🎋🍃🍂🍁🌾🌵🎄🌲🌳🌴🌺🌻🌹🥀🌷🌼🌸💐🌿🍄🌰🌴🌳🌲🌱🌾🌺🌻🌹🌷🌼🌸🌞☀️🌝🌜🌛🌙⭐🌟✨💫🌠☁️⛅🌤️🌥️🌦️🌧️⛈️🌩️🌨️❄️☃️⛄🌬️🌪️🌫️🌀☔⚡🔥💧💦🌊🌈🐝🦋🐛🐌🐞🦗🐜🐢🐸🐦🕊️🐤🐣🐥🐟🐠🐬🐳"

        private const val FOOD =
            "🍏🍎🍐🍊🍋🍌🍉🍇🍓🍈🍒🍑🥭🍍🥥🥝🍅🍆🥑🥦🥬🥒🌶️🌽🥕🧄🧅🥔🍠🥐🥯🍞🥖🥨🧀🥚🍳🧈🥞🧇🥓🥩🍗🍖🌭🍔🍟🍕🥪🥙🌮🌯🥗🥘🥫🍝🍜🍲🍛🍣🍱🥟🍤🍙🍚🍘🍥🥠🥮🍢🍡🍧🍨🍦🥧🧁🍰🎂🍮🍭🍬🍫🍿🍩🍪🌰🥜🍯🥛🍼☕🍵🧃🥤🍶🍺🍻🥂🍷🥃🍸🍹🍾🧊🥄🍴🍽️🥣🥡🧂🍗🍖🍤🦀🦞🦐🐟🍎🍐🍊🍋🍌🍇🍓🍒🍑🍍🥝🍉"
        private const val TRAVEL =
            "🚗🚕🚙🚌🚎🏎️🚑🚒🚐🚚🚛🚜🛴🚲🛵🏍️🛺🚨🚔🚍🚘🚖🚡🚠🚟🚃🚋🚞🚝🚄🚅🚈🚂🚆🚇🚊🚉✈️🛫🛬🛩️💺🛰️🚀🚁🛶⛵🚤🛥️🛳️⛴️🚢⚓⛽🚧🚦🚥🗺️🗿🗽🗼🏰🏯🏟️🎡🎢🎠⛲⛱️🏖️🏝️🏜️🌋⛰️🏔️🗻🏕️⛺🏠🏡🏘️🏚️🏗️🏭🏢🏬🏣🏤🏥🏦🏨🏪🏫🏩💒🏛️⛪🕌🕍🕋⛩️🛤️🛣️🗾🎑🏞️🌅🌄🌠🎇🎆🌇🌆🏙️🌃🌌🌉🌁"
        private const val BOOK =
            "📖📚📕📗📘📙📓📒📔📑📜📃📄📰🗞️📁📂🗂️📇🗃️📋📌📍🔖🏷️✏️✒️🖊️🖋️🖌️🖍️📐📏✂️📎🖇️🔗🔍🔎📝🎓🏫🎬🎥📹📽️🎞️🍿🎭🎪🎨🎤🎧🎼🎵🎶🎸🎹🎺🎷🥁🎻🎫🎟️"
        private const val MUSIC =
            "🎵🎶🎼🎤🎙️🎹🥁🪘🎷🎺🪗🎸🪕🎻🎧🎚️🎛️📻💿📀🔊🔉🔈🔇🔔🔕📣📢🎬🎭🎪🤹🩰💃🕺🎟️🎫🎉✨🌟🎵🎶"
    }
}
