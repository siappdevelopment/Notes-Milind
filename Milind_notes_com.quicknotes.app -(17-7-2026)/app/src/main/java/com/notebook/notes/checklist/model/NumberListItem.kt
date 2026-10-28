package com.notebook.notes.checklist.model

import org.json.JSONObject

data class NumberListItem(
    val text: String,
    val order: Int,
    val level: Int = 0,
    val listGroupId: Int = 0
)

object NumberListMetadata {
    fun toJson(item: NumberListItem): String = JSONObject()
        .put("order", item.order)
        .put("level", item.level)
        .put("listGroupId", item.listGroupId)
        .toString()

    fun fromJson(json: String?): NumberListItem? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            NumberListItem(
                text = "",
                order = obj.optInt("order", 1),
                level = obj.optInt("level", 0),
                listGroupId = obj.optInt("listGroupId", 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun fromBlock(block: NoteBlock): NumberListItem {
        val stored = fromJson(block.extraData)
        return NumberListItem(
            text = block.content,
            order = stored?.order ?: 1,
            level = stored?.level ?: 0,
            listGroupId = stored?.listGroupId ?: 0
        )
    }
}

object NumberListHelper {
    private const val MAX_ITEMS = 1000

    fun computeDisplayOrders(blocks: List<NoteBlock>): Map<Long, Int> {
        val sorted = blocks.sortedWith(compareBy({ it.position }, { it.id }))
        val result = linkedMapOf<Long, Int>()
        // Numbering is tracked per list group and is NOT interrupted by normal
        // text (or any other) blocks between numbered items. Only a different
        // listGroupId starts a fresh sequence at 1, so re-opening a numbered list
        // after typing a note continues from the last number instead of resetting.
        val groupCounts = hashMapOf<Int, Int>()

        for (block in sorted) {
            if (block.type == BlockType.NUMBER_LIST) {
                val groupId = NumberListMetadata.fromBlock(block).listGroupId
                val next = (groupCounts[groupId] ?: 0) + 1
                groupCounts[groupId] = next
                result[block.id] = next
            }
        }
        return result
    }

    fun normalizeBlocks(blocks: List<NoteBlock>): List<NoteBlock> {
        val orders = computeDisplayOrders(blocks)
        return blocks.map { block ->
            if (block.type != BlockType.NUMBER_LIST) return@map block
            val metadata = NumberListMetadata.fromBlock(block)
            val order = orders[block.id] ?: 1
            block.copy(
                extraData = NumberListMetadata.toJson(
                    metadata.copy(text = block.content, order = order)
                )
            )
        }
    }

    fun splitLinesToItems(text: String): List<String> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_ITEMS)

    fun formatPrefix(order: Int): String = "$order."
}
