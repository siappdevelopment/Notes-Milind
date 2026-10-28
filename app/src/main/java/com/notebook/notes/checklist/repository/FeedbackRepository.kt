package com.notebook.notes.checklist.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.notebook.notes.checklist.R
import com.notebook.notes.checklist.flow.RemoteConfigManager
import com.notebook.notes.checklist.util.DeviceIdProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class FeedbackRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remoteConfigManager: RemoteConfigManager
) {

    /**
     * POST multipart: image?, deviceId, appName, packageName, title, description,
     * feedbackType?, email?
     * URL: Firebase `feedback_url` + `/api/apps/upload` (plain URL, no encrypt).
     */
    suspend fun submitFeedback(
        title: String,
        description: String,
        feedbackType: String,
        email: String?,
        imageUri: Uri?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val apiUrl = try {
                remoteConfigManager.buildFeedbackUploadUrl()
            } catch (e: IllegalArgumentException) {
                throw IOException(
                    context.getString(R.string.feedback_submit_failed),
                    e
                )
            }
            val deviceId = DeviceIdProvider.getDeviceId(context)
            val appName = context.getString(R.string.app_name)
            val packageName = context.packageName
            // `title` already includes category + user title (concatenated in Activity)
            val uploadDescription = buildString {
                append(description)
                if (!email.isNullOrBlank()) {
                    append("\n\nContact Email: ").append(email)
                }
            }

            Log.d(TAG, "──────── FEEDBACK REQUEST ────────")
            Log.d(TAG, "apiUrl      = $apiUrl")
            Log.d(TAG, "deviceId    = $deviceId")
            Log.d(TAG, "appName     = $appName")
            Log.d(TAG, "packageName = $packageName")
            Log.d(TAG, "type        = $feedbackType")
            Log.d(TAG, "title       = $title")
            Log.d(TAG, "description = $uploadDescription")
            Log.d(TAG, "email       = ${email.orEmpty().ifBlank { "(not sent)" }}")

            var tempImage: File? = null
            try {
                if (imageUri != null) {
                    tempImage = copyUriToCache(imageUri)
                    Log.d(TAG, "imageName   = ${tempImage.name}")
                    Log.d(TAG, "imageSize   = ${tempImage.length() / 1024}KB")
                } else {
                    Log.d(TAG, "image       = (not sent)")
                }

                val boundary = "----FeedbackBoundary${System.currentTimeMillis()}"
                val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true
                    useCaches = false
                    connectTimeout = 30_000
                    readTimeout = 30_000
                    setRequestProperty("Connection", "Keep-Alive")
                    setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                }

                DataOutputStream(connection.outputStream).use { output ->
                    fun writeField(name: String, value: String) {
                        output.writeBytes("--$boundary\r\n")
                        output.writeBytes("Content-Disposition: form-data; name=\"$name\"\r\n\r\n")
                        output.writeBytes(value)
                        output.writeBytes("\r\n")
                    }

                    writeField("deviceId", deviceId)
                    writeField("appName", appName)
                    writeField("packageName", packageName)
                    writeField("title", title)
                    writeField("description", uploadDescription)
                    writeField("feedbackType", feedbackType)
                    if (!email.isNullOrBlank()) {
                        writeField("email", email)
                    }

                    val imageFile = tempImage
                    if (imageFile != null) {
                        val mime = guessMimeType(imageUri!!, imageFile) ?: "image/jpeg"
                        val fileName = imageFile.name.ifBlank { "feedback.jpg" }
                        output.writeBytes("--$boundary\r\n")
                        output.writeBytes(
                            "Content-Disposition: form-data; name=\"image\"; filename=\"$fileName\"\r\n"
                        )
                        output.writeBytes("Content-Type: $mime\r\n\r\n")
                        imageFile.inputStream().use { input -> input.copyTo(output) }
                        output.writeBytes("\r\n")
                    }

                    output.writeBytes("--$boundary--\r\n")
                    output.flush()
                }

                val code = connection.responseCode
                val raw = try {
                    val stream = if (code in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream ?: connection.inputStream
                    }
                    stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                } finally {
                    connection.disconnect()
                }

                Log.d(TAG, "──────── FEEDBACK RESPONSE ────────")
                Log.d(TAG, "httpCode    = $code")
                Log.d(TAG, "rawBody     = $raw")

                if (code !in 200..299) {
                    throw IOException(
                        raw.takeIf { it.isNotBlank() } ?: "Feedback upload failed ($code)"
                    )
                }
            } finally {
                tempImage?.delete()
            }
        }
    }

    private fun copyUriToCache(uri: Uri): File {
        val extension = guessExtension(uri) ?: "jpg"
        val dest = File(context.cacheDir, "feedback_${System.currentTimeMillis()}.$extension")
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException("Cannot read selected image")
        if (!dest.exists() || dest.length() == 0L) {
            throw IOException("Selected image is empty")
        }
        return dest
    }

    private fun guessMimeType(uri: Uri, file: File): String? {
        context.contentResolver.getType(uri)?.let { return it }
        val ext = file.extension.lowercase()
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun guessExtension(uri: Uri): String? {
        val type = context.contentResolver.getType(uri)
        if (!type.isNullOrBlank()) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(type)?.let { return it }
        }
        return uri.lastPathSegment
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() && it.length <= 5 }
    }

    companion object {
        private const val TAG = "FeedbackRepository"
    }
}
