package com.notebook.notes.checklist.attachment

import com.notebook.notes.checklist.R
import java.util.Locale

object FileTypeIconResolver {

    fun iconFor(fileName: String, mimeType: String = ""): Int {
        val extension = fileName.substringAfterLast('.', "").lowercase(Locale.US)
        return when (extension) {
            "pdf" -> R.drawable.ic_pdf
            "doc", "docx" -> R.drawable.ic_doc
            "ppt", "pptx" -> R.drawable.ic_ppt
            "txt" -> R.drawable.ic_txt
            "xls", "xlsx", "xlc", "exc", "csv" -> R.drawable.ic_exe
            "mp3", "m4a", "wav", "aac", "ogg", "flac", "3gp", "amr", "opus", "wma" ->
                R.drawable.ic_audio
            else -> iconForMimeType(mimeType)
        }
    }

    private fun iconForMimeType(mimeType: String): Int {
        val mime = mimeType.lowercase(Locale.US)
        return when {
            mime.contains("pdf") -> R.drawable.ic_pdf
            mime.contains("word") || mime.contains("msword") -> R.drawable.ic_doc
            mime.contains("powerpoint") || mime.contains("presentation") -> R.drawable.ic_ppt
            mime.contains("excel") || mime.contains("spreadsheet") -> R.drawable.ic_exe
            mime.startsWith("text/") -> R.drawable.ic_txt
            mime.startsWith("audio/") ||
                mime == "application/ogg" ||
                mime == "application/x-ogg" ||
                mime == "application/x-flac" -> R.drawable.ic_audio
            else -> R.drawable.ic_file
        }
    }
}
