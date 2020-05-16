package com.hyejeanmoon.scopedstoragedemo

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.BufferedSink
import okio.buffer
import okio.sink
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FileDownload {

    suspend fun storageFile(url: String, context: Context): FileDownloadResult =
        suspendCoroutine { continuation ->

            var isHasError = false

            val result = kotlin.runCatching {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    var dirPath = ""
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        dirPath =
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                .toString()
                        val dir = File(dirPath)
                        if (!dir.exists()) {
                            dir.mkdir()
                        }
                    }

                    val file = dirPath + "/" + Uri.parse(url).lastPathSegment
                    val downloadedFile = File(file)

                    val fileExtension =
                        MimeTypeMap.getFileExtensionFromUrl(downloadedFile.absolutePath)
                    val mimeType = MimeTypeMap.getSingleton()
                        .getMimeTypeFromExtension(fileExtension.toLowerCase(Locale.ROOT))
                    val values = ContentValues()

                    val sink: BufferedSink

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        values.put(MediaStore.Images.Media.DESCRIPTION, downloadedFile.name)
                        values.put(MediaStore.Images.Media.DISPLAY_NAME, downloadedFile.name)
                        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                        values.put(MediaStore.Images.Media.TITLE, downloadedFile.name)
                        values.put(
                            MediaStore.Images.Media.RELATIVE_PATH,
                            "${Environment.DIRECTORY_PICTURES}/${downloadedFile.name}"
                        )
                        val insertUri = context.contentResolver.insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values
                        )

                        dirPath = context.getExternalFilesDir("")?.absolutePath ?: ""

                        if (insertUri != null) {
                            val outputStream = context.contentResolver.openOutputStream(insertUri)
                            if (outputStream != null) {
                                sink = outputStream.sink().buffer()
                            } else {
                                return@runCatching FileDownloadResult.OthersError
                            }
                        } else {
                            return@runCatching FileDownloadResult.OthersError
                        }
                    } else {
                        sink = downloadedFile.sink(true).buffer()
                    }

                    val responseBody =
                        response.body ?: return@runCatching FileDownloadResult.OthersError

                    try {
                        val contentLength = responseBody.contentLength()
                        if (contentLength > FileUtil.getAvailableSize(dirPath)) {

                            continuation.resume(FileDownloadResult.StorageError)
                        }
                        var totalRead: Long = 0
                        var lastRead: Long

                        do {
                            lastRead = responseBody.source().read(sink.buffer(), BUFFER_SIZE)
                            if (lastRead == -1L) {
                                break
                            }
                            totalRead += lastRead
                            sink.emitCompleteSegments()
                        } while (true)
                        sink.writeAll(responseBody.source())
                        sink.close()
                        responseBody.close()

                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                            values.put(
                                MediaStore.Images.Media.DATA,
                                downloadedFile.absolutePath
                            )
                            context.contentResolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                values
                            )
                        }
                    } catch (e: IOException) {
                        responseBody.close()
                        sink.close()
                    }

                }
                return@runCatching FileDownloadResult.Successful
            }.onFailure {
                isHasError = true
                continuation.resumeWithException(it)
            }

            if (!isHasError) continuation.resumeWith(result)

        }

    companion object {
        internal const val BUFFER_SIZE = 4096L
    }
}