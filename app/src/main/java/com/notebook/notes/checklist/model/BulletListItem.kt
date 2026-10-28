package com.notebook.notes.checklist.model

import org.json.JSONObject

data class BulletListItem(
    val text: String,
    val position: Int,
    val level: Int = 0,
    val listGroupId: Int = 0
)

object BulletListMetadata {
    fun toJson(item: BulletListItem): String = JSONObject()
        .put("position", item.position)
        .put("level", item.level)
        .put("listGroupId", item.listGroupId)
        .toString()

    fun fromJson(json: String?): BulletListItem? {
        if (json.isNullOrBlank()) return null
        return try {
            val obj = JSONObject(json)
            BulletListItem(
                text = "",
                position = obj.optInt("position", 0),
                level = obj.optInt("level", 0),
                listGroupId = obj.optInt("listGroupId", 0)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun fromBlock(block: NoteBlock): BulletListItem {
        val stored = fromJson(block.extraData)
        return BulletListItem(
            text = block.content,
            position = stored?.position ?: 0,
            level = stored?.level ?: 0,
            listGroupId = stored?.listGroupId ?: 0
        )
    }
}

object BulletListHelper {
    private const val MAX_ITEMS = 1000
    const val BULLET_PREFIX = "\u2022"

    fun computeDisplayPositions(blocks: List<NoteBlock>): Map<Long, Int> {
        val sorted = blocks.sortedWith(compareBy({ it.position }, { it.id }))
        val result = linkedMapOf<Long, Int>()
        var currentPosition = -1
        var currentGroupId: Int? = null

        for (block in sorted) {
            if (block.type == BlockType.BULLET_LIST) {
                val groupId = BulletListMetadata.fromBlock(block).listGroupId
                if (groupId != currentGroupId) {
                    currentPosition = -1
                    currentGroupId = groupId
                }
                currentPosition++
                result[block.id] = currentPosition
            } else {
                currentPosition = -1
                currentGroupId = null
            }
        }
        return result
    }

    fun normalizeBlocks(blocks: List<NoteBlock>): List<NoteBlock> {
        val positions = computeDisplayPositions(blocks)
        return blocks.map { block ->
            if (block.type != BlockType.BULLET_LIST) return@map block
            val metadata = BulletListMetadata.fromBlock(block)
            val position = positions[block.id] ?: 0
            block.copy(
                extraData = BulletListMetadata.toJson(
                    metadata.copy(text = block.content, position = position)
                )
            )
        }
    }

    fun removeEmptyBlocks(blocks: List<NoteBlock>): List<NoteBlock> =
        blocks.filter { block ->
            block.type != BlockType.BULLET_LIST ||
                com.notebook.notes.checklist.utils.JsonUtils.blockContentPlainText(block.content).isNotBlank()
        }

    fun splitLinesToItems(text: String): List<String> =
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_ITEMS)

    fun formatPrefix(): String = "$BULLET_PREFIX "
}
