package com.notebook.notes.checklist.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.notebook.notes.checklist.model.RichTextDocument
import com.notebook.notes.checklist.model.RichTextSpan
import com.notebook.notes.checklist.model.TextBlockData

object JsonUtils {
    private val gson = Gson()
    private val spanListType = object : TypeToken<List<RichTextSpan>>() {}.type

    fun jsonToRichText(json: String): RichTextDocument {
        if (json.isBlank()) return RichTextDocument()
        return try {
            when {
                json.trimStart().startsWith("[") -> {
                    val spans: List<RichTextSpan> = gson.fromJson(json, spanListType)
                    RichTextDocument(spans = spans.ifEmpty { listOf(RichTextSpan()) })
                }
                json.trimStart().startsWith("{") -> {
                    if (json.contains("\"spans\"")) {
                        val document = gson.fromJson(json, RichTextDocument::class.java)
                        RichTextDocument(
                            spans = document.spans.ifEmpty { listOf(RichTextSpan()) },
                            alignment = document.alignment
                        )
                    } else {
                        val legacy = gson.fromJson(json, TextBlockData::class.java)
                        RichTextDocument(
                            spans = listOf(
                                RichTextSpan(
                                    text = legacy?.text.orEmpty(),
                                    bold = legacy?.bold ?: false,
                                    italic = legacy?.italic ?: false,
                                    underline = legacy?.underline ?: false,
                                    fontSize = legacy?.fontSize ?: 16,
                                    fontFamily = legacy?.fontFamily ?: "default",
                                    textColor = legacy?.textColor ?: "#000000"
                                )
                            )
                        )
                    }
                }
                else -> RichTextDocument(spans = listOf(RichTextSpan(text = json)))
            }
        } catch (_: Exception) {
            RichTextDocument(spans = listOf(RichTextSpan(text = json)))
        }
    }

    fun richTextToJson(document: RichTextDocument): String = gson.toJson(document)

    fun blockContentPlainText(content: String): String = jsonToRichText(content).plainText()

    fun plainTextToRichJson(text: String): String = richTextToJson(jsonToRichText(text))

    fun emptyRichJson(
        fontSize: Int = RichTextSpan().fontSize,
        fontFamily: String = RichTextSpan().fontFamily
    ): String =
        richTextToJson(
            RichTextDocument(
                spans = listOf(RichTextSpan(fontSize = fontSize, fontFamily = fontFamily))
            )
        )

    @Deprecated("Use richTextToJson")
    fun textBlockToJson(data: TextBlockData): String = richTextToJson(
        RichTextDocument(
            spans = listOf(
                RichTextSpan(
                    text = data.text,
                    bold = data.bold,
                    italic = data.italic,
                    underline = data.underline,
                    fontSize = data.fontSize,
                    fontFamily = data.fontFamily,
                    textColor = data.textColor
                )
            )
        )
    )

    @Deprecated("Use jsonToRichText")
    fun jsonToTextBlock(json: String): TextBlockData {
        val doc = jsonToRichText(json)
        val span = doc.spans.firstOrNull() ?: RichTextSpan()
        return TextBlockData(
            text = doc.plainText(),
            bold = span.bold,
            italic = span.italic,
            underline = span.underline,
            fontSize = span.fontSize,
            fontFamily = span.fontFamily,
            textColor = span.textColor
        )
    }
}
