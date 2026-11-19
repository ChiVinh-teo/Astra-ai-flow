package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipInputStream

data class SelectedDocumentInfo(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val pdfPageCount: Int = 0
) {
    val isPdf: Boolean get() = mimeType.contains("pdf", ignoreCase = true) || displayName.endsWith(".pdf", true)
    val isLargePdf: Boolean get() = isPdf && sizeBytes >= LARGE_PDF_THRESHOLD_BYTES
    val sizeMbFormatted: String get() = String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0))

    companion object {
        const val LARGE_PDF_THRESHOLD_BYTES = 8L * 1024L * 1024L // 8MB
    }
}

sealed class PreparedDocumentPart {
    data class TextPart(val fileName: String, val text: String) : PreparedDocumentPart()
    data class InlineBlob(val fileName: String, val mimeType: String, val base64Data: String) : PreparedDocumentPart()
    data class LargePdfForFilesApi(val fileName: String, val uri: Uri, val sizeBytes: Long) : PreparedDocumentPart()
}

enum class LargePdfStrategy {
    GEMINI_FILES_API,
    PDF_RENDERER_PAGE_RANGE
}

object DocumentProcessor {

    suspend fun inspectUri(context: Context, uri: Uri): SelectedDocumentInfo = withContext(Dispatchers.IO) {
        var name = "tai_lieu"
        var size = 0L
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst()) {
                if (nameIdx >= 0) name = cursor.getString(nameIdx) ?: name
                if (sizeIdx >= 0) size = cursor.getLong(sizeIdx)
            }
        }
        val mime = context.contentResolver.getType(uri) ?: guessMimeFromName(name)
        var pageCount = 0
        if (mime.contains("pdf", ignoreCase = true) || name.endsWith(".pdf", true)) {
            runCatching {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        pageCount = renderer.pageCount
                    }
                }
            }
        }
        SelectedDocumentInfo(
            uri = uri,
            displayName = name,
            mimeType = mime,
            sizeBytes = size,
            pdfPageCount = pageCount
        )
    }

    suspend fun prepareDocumentParts(
        context: Context,
        info: SelectedDocumentInfo,
        largePdfStrategy: LargePdfStrategy = LargePdfStrategy.GEMINI_FILES_API,
        pageStart: Int = 1,
        pageEnd: Int = 10
    ): List<PreparedDocumentPart> = withContext(Dispatchers.IO) {
        val nameLower = info.displayName.lowercase()
        when {
            info.mimeType.startsWith("image/") -> {
                val b64 = encodeImageUriToBase64Jpeg(context, info.uri)
                if (b64 != null) listOf(PreparedDocumentPart.InlineBlob(info.displayName, "image/jpeg", b64)) else emptyList()
            }
            nameLower.endsWith(".docx") || info.mimeType.contains("wordprocessingml") -> {
                val extracted = extractDocxText(context, info.uri)
                listOf(PreparedDocumentPart.TextPart(info.displayName, extracted.take(45_000)))
            }
            nameLower.endsWith(".txt") || nameLower.endsWith(".md") || info.mimeType.startsWith("text/") -> {
                val text = context.contentResolver.openInputStream(info.uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                listOf(PreparedDocumentPart.TextPart(info.displayName, text.take(45_000)))
            }
            info.isPdf -> {
                if (!info.isLargePdf) {
                    val bytes = context.contentResolver.openInputStream(info.uri)?.use { it.readBytes() } ?: ByteArray(0)
                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    listOf(PreparedDocumentPart.InlineBlob(info.displayName, "application/pdf", b64))
                } else if (largePdfStrategy == LargePdfStrategy.GEMINI_FILES_API) {
                    listOf(PreparedDocumentPart.LargePdfForFilesApi(info.displayName, info.uri, info.sizeBytes))
                } else {
                    renderPdfPageRangeToJpegs(context, info, pageStart, pageEnd)
                }
            }
            else -> {
                val fallbackText = context.contentResolver.openInputStream(info.uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                listOf(PreparedDocumentPart.TextPart(info.displayName, fallbackText.take(35_000)))
            }
        }
    }

    suspend fun encodeImageUriToBase64Jpeg(context: Context, uri: Uri, maxDim: Int = 1280): String? = withContext(Dispatchers.IO) {
        runCatching {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, boundsOpts)
            }
            var sample = 1
            val largest = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
            while (largest / sample > maxDim) sample *= 2

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return@runCatching null

            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, bos)
            bitmap.recycle()
            Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }

    private fun renderPdfPageRangeToJpegs(
        context: Context,
        info: SelectedDocumentInfo,
        pageStart: Int,
        pageEnd: Int
    ): List<PreparedDocumentPart> {
        val parts = mutableListOf<PreparedDocumentPart>()
        context.contentResolver.openFileDescriptor(info.uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val total = renderer.pageCount
                val startIdx = (pageStart - 1).coerceIn(0, maxOf(0, total - 1))
                val endIdx = (pageEnd - 1).coerceIn(startIdx, minOf(total - 1, startIdx + 14))
                for (i in startIdx..endIdx) {
                    renderer.openPage(i).use { page ->
                        val scale = minOf(1024f / maxOf(1, page.width), 1.5f)
                        val w = (page.width * scale).toInt().coerceAtLeast(320)
                        val h = (page.height * scale).toInt().coerceAtLeast(320)
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        Canvas(bmp).drawColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val bos = ByteArrayOutputStream()
                        bmp.compress(Bitmap.CompressFormat.JPEG, 76, bos)
                        bmp.recycle()
                        val b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
                        parts += PreparedDocumentPart.InlineBlob(
                            fileName = "${info.displayName} (Trang ${i + 1})",
                            mimeType = "image/jpeg",
                            base64Data = b64
                        )
                    }
                }
            }
        }
        return parts
    }

    private fun extractDocxText(context: Context, uri: Uri): String {
        val sb = StringBuilder()
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name == "word/document.xml") {
                            val xml = zip.bufferedReader(Charsets.UTF_8).readText()
                            val clean = xml
                                .replace(Regex("</w:p>"), "\n")
                                .replace(Regex("<[^>]+>"), " ")
                                .replace("&amp;", "&")
                                .replace("&lt;", "<")
                                .replace("&gt;", ">")
                                .replace(Regex("[ \\t]+"), " ")
                            sb.append(clean)
                            break
                        }
                        entry = zip.nextEntry
                    }
                }
            }
        }
        return sb.toString().trim().ifBlank { "Không đọc được nội dung DOCX." }
    }

    fun createTempCameraUri(context: Context): Uri {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun guessMimeFromName(name: String): String = when {
        name.endsWith(".pdf", true) -> "application/pdf"
        name.endsWith(".docx", true) -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        name.endsWith(".md", true) -> "text/markdown"
        name.endsWith(".txt", true) -> "text/plain"
        name.endsWith(".png", true) -> "image/png"
        name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) -> "image/jpeg"
        else -> "application/octet-stream"
    }
}
