/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Chanrithy Thim
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.android.girish.vlog

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import com.android.girish.vlog.utils.getAppName
import com.android.girish.vlog.utils.getAppTitle
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Exports the most recent logs to a text file.
 *
 * The file goes to a folder named after the host app in `Downloads` on Android 10+, which needs no
 * permission. Older versions write to the `Documents` folder of the app, since a public folder would need
 * the storage permission.
 */
internal object LogExporter {
    const val RECENT_WINDOW_MS = 10_000L

    private val TAG = LogExporter::class.java.simpleName
    private val ILLEGAL_FOLDER_CHARS = Regex("[\\\\/:*?\"<>|]")
    private const val MIME_TYPE = "text/plain"

    // Created on first use, so the pure functions below also work in plain JVM tests
    private val executor by lazy { Executors.newSingleThreadExecutor() }
    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    /**
     * Writes the logs of the last [RECENT_WINDOW_MS] to a file and tells the user where it went with a toast.
     *
     * @param logs the logs to pick from, usually the ones the viewer shows so filters apply
     */
    fun exportRecent(
        context: Context,
        logs: List<VlogModel>,
    ) {
        val appContext = context.applicationContext
        val now = System.currentTimeMillis()
        val recent = recentLogs(logs, now)
        if (recent.isEmpty()) {
            showToast(appContext, appContext.getString(R.string.export_nothing))
            return
        }

        val content = format(appContext.getAppTitle(), now, recent)
        val fileName = fileName(appContext.packageName, now)
        val folder = folderName(appContext.getAppName(), appContext.packageName)
        executor.execute {
            val message =
                try {
                    val location = write(appContext, folder, fileName, content)
                    appContext.getString(R.string.export_done, recent.size, location)
                } catch (e: IOException) {
                    Log.w(TAG, "Unable to export the logs", e)
                    appContext.getString(R.string.export_failed)
                }
            mainHandler.post { showToast(appContext, message) }
        }
    }

    /**
     * The logs written in the [RECENT_WINDOW_MS] before [now], oldest first as given.
     */
    fun recentLogs(
        logs: List<VlogModel>,
        now: Long,
    ): List<VlogModel> = logs.filter { it.timestamp >= now - RECENT_WINDOW_MS }

    /**
     * The file content: a header naming the app and the time span, then one line per log.
     */
    fun format(
        title: String,
        now: Long,
        logs: List<VlogModel>,
    ): String {
        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return buildString {
            appendLine("$title, logs of the last ${RECENT_WINDOW_MS / 1000} seconds, exported ${timeFormat.format(Date(now))}")
            logs.forEach {
                // The id of a grouped log goes after the tag, so the logs of one group can still be matched
                val id = it.id?.let { id -> " [$id]" }.orEmpty()
                appendLine("${timeFormat.format(Date(it.timestamp))} ${it.priorityInitial()}/${it.tag}$id: ${it.logMessage}")
            }
        }
    }

    /**
     * The folder the file goes to: the name of the host app, with the characters a folder name cannot have
     * replaced. Falls back to the package name when nothing is left.
     */
    fun folderName(
        appName: String,
        packageName: String,
    ): String = appName.replace(ILLEGAL_FOLDER_CHARS, "_").trim().ifEmpty { packageName }

    private fun fileName(
        packageName: String,
        now: Long,
    ): String = "vlog_${packageName}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(now))}.txt"

    /**
     * @return where the file ended up, to show to the user
     */
    private fun write(
        context: Context,
        folder: String,
        fileName: String,
        content: String,
    ): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values =
                ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE)
                    put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$folder")
                    // Hidden from other apps until the content is complete
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: throw IOException("Unable to create $fileName")
            try {
                val stream = resolver.openOutputStream(uri) ?: throw IOException("Unable to open $fileName")
                stream.use { it.write(content.toByteArray()) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } catch (e: IOException) {
                resolver.delete(uri, null, null)
                throw e
            }
            return "${Environment.DIRECTORY_DOWNLOADS}/$folder/$fileName"
        }

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: throw IOException("No storage available")
        directory.mkdirs()
        return File(directory, fileName).also { it.writeText(content) }.absolutePath
    }

    private fun showToast(
        context: Context,
        message: String,
    ) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
