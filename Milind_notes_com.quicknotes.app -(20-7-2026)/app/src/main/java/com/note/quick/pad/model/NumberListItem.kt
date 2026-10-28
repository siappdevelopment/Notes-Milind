package com.note.quick.pad.model

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
        var currentNumber = 0
        var currentGroupId: Int? = null

        for (block in sorted) {
            if (block.type == BlockType.NUMBER_LIST) {
                val groupId = NumberListMetadata.fromBlock(block).listGroupId
                if (groupId != currentGroupId) {
                    currentNumber = 0
                    currentGroupId = groupId
                }
                currentNumber++
                result[block.id] = currentNumber
            } else {
                currentNumber = 0
                currentGroupId = null
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
