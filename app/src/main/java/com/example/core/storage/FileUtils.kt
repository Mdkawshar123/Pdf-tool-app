package com.example.core.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat

object FileUtils {

    fun getFileName(context: Context, uri: Uri): String {
        var name = "document.pdf"
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        } else if (uri.scheme == "file") {
            uri.path?.let { path ->
                name = File(path).name
            }
        }
        return name
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        var size = 0L
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1 && cursor.moveToFirst()) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        } else if (uri.scheme == "file") {
            uri.path?.let { path ->
                size = File(path).length()
            }
        }
        return size
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val index = digitGroups.coerceIn(0, units.size - 1)
        val value = bytes / Math.pow(1024.0, index.toDouble())
        return DecimalFormat("#,##0.#").format(value) + " " + units[index]
    }

    fun copyUriToTempFile(context: Context, uri: Uri, prefix: String = "pdf_tmp_"): File {
        val extension = getFileName(context, uri).substringAfterLast(".", "pdf")
        val tempDir = File(context.cacheDir, "pdf_workspace").apply { mkdirs() }
        val tempFile = File.createTempFile(prefix, ".$extension", tempDir)
        
        context.contentResolver.openInputStream(uri)?.use { input: InputStream ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    fun createOutputPdfFile(context: Context, baseName: String): File {
        val outputDir = File(context.filesDir, "MasterPdfOutputs").apply { mkdirs() }
        val cleanName = baseName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val timestamp = System.currentTimeMillis()
        return File(outputDir, "${cleanName}_$timestamp.pdf")
    }

    fun createOutputImageFile(context: Context, pageNum: Int): File {
        val outputDir = File(context.filesDir, "MasterPdfImages").apply { mkdirs() }
        val timestamp = System.currentTimeMillis()
        return File(outputDir, "page_${pageNum}_$timestamp.jpg")
    }

    fun clearTempFiles(context: Context): Long {
        var freedBytes = 0L
        val tempDir = File(context.cacheDir, "pdf_workspace")
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                freedBytes += file.length()
                file.delete()
            }
        }
        return freedBytes
    }

    fun getTempFilesSize(context: Context): Long {
        var totalBytes = 0L
        val tempDir = File(context.cacheDir, "pdf_workspace")
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                totalBytes += file.length()
            }
        }
        return totalBytes
    }

    fun shareFile(context: Context, file: File, mimeType: String = "application/pdf") {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
    }

    fun openFile(context: Context, file: File, mimeType: String = "application/pdf") {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, file)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(viewIntent, "Open with"))
        } catch (e: Exception) {
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
        }
    }
}
