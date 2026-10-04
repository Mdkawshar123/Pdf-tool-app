package com.example.core.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.core.storage.FileUtils
import com.example.domain.model.CompressionLevel
import com.example.domain.model.ImageToPdfConfig
import com.example.domain.model.PageNumberConfig
import com.example.domain.model.PageNumberFormat
import com.example.domain.model.PageNumberPosition
import com.example.domain.model.PageOrientation
import com.example.domain.model.PageRotationAngle
import com.example.domain.model.PdfOperationResult
import com.example.domain.model.PdfPageSize
import com.example.domain.model.SplitMode
import com.example.domain.model.WatermarkConfig
import com.example.domain.model.WatermarkPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.coroutineContext

object PdfProcessingEngine {

    suspend fun getPdfPageCount(file: File): Int = withContext(Dispatchers.IO) {
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            }
        } catch (e: Exception) {
            1
        }
    }

    suspend fun renderPageToBitmap(
        file: File,
        pageIndex: Int,
        targetWidth: Int = 1080
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (pageIndex < 0 || pageIndex >= renderer.pageCount) return@withContext null
                    renderer.openPage(pageIndex).use { page ->
                        val scale = targetWidth.toFloat() / page.width.toFloat()
                        val targetHeight = (page.height * scale).toInt()
                        val bitmap = Bitmap.createBitmap(
                            targetWidth.coerceAtLeast(1),
                            targetHeight.coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888
                        )
                        // Fill white background
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmap
                    }
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Merge multiple PDF files into one output PDF
     */
    suspend fun mergePdfs(
        context: Context,
        inputFiles: List<File>,
        onProgress: (current: Int, total: Int) -> Unit
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        val outputFile = FileUtils.createOutputPdfFile(context, "merged_document")
        val outDoc = PdfDocument()
        var totalPages = 0
        var processedPages = 0

        try {
            // Count total pages first
            for (file in inputFiles) {
                totalPages += getPdfPageCount(file)
            }
            if (totalPages == 0) totalPages = inputFiles.size

            var docPageIndex = 0
            for (file in inputFiles) {
                if (!coroutineContext.isActive) return@withContext PdfOperationResult(
                    isSuccess = false,
                    errorMessage = "Operation cancelled"
                )

                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        for (i in 0 until renderer.pageCount) {
                            if (!coroutineContext.isActive) return@withContext PdfOperationResult(
                                isSuccess = false,
                                errorMessage = "Operation cancelled"
                            )

                            renderer.openPage(i).use { page ->
                                val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, docPageIndex + 1).create()
                                val outPage = outDoc.startPage(pageInfo)
                                val canvas = outPage.canvas
                                canvas.drawColor(Color.WHITE)

                                val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                                canvas.drawBitmap(bmp, 0f, 0f, null)
                                bmp.recycle()

                                outDoc.finishPage(outPage)
                                docPageIndex++
                                processedPages++
                                onProgress(processedPages, totalPages)
                            }
                        }
                    }
                }
            }

            FileOutputStream(outputFile).use { outDoc.writeTo(it) }
            outDoc.close()

            PdfOperationResult(
                isSuccess = true,
                outputFile = outputFile,
                outputFileName = outputFile.name,
                pageCount = docPageIndex,
                fileSizeBytes = outputFile.length()
            )
        } catch (e: Exception) {
            outDoc.close()
            outputFile.delete()
            PdfOperationResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Failed to merge PDF files."
            )
        }
    }

    /**
     * Split PDF by ranges or selected pages
     */
    suspend fun splitPdf(
        context: Context,
        inputFile: File,
        mode: SplitMode,
        customRangeStr: String,
        selectedPages: Set<Int>,
        everyN: Int = 1,
        onProgress: (current: Int, total: Int) -> Unit
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        val outputFile = FileUtils.createOutputPdfFile(context, "split_document")
        val outDoc = PdfDocument()

        try {
            ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val totalSrcPages = renderer.pageCount
                    val pagesToInclude = mutableListOf<Int>()

                    when (mode) {
                        SplitMode.ALL_PAGES, SplitMode.EXTRACT_SELECTED -> {
                            if (selectedPages.isNotEmpty()) {
                                pagesToInclude.addAll(selectedPages.filter { it in 0 until totalSrcPages }.sorted())
                            } else {
                                pagesToInclude.addAll(0 until totalSrcPages)
                            }
                        }
                        SplitMode.CUSTOM_RANGE -> {
                            pagesToInclude.addAll(parsePageRanges(customRangeStr, totalSrcPages))
                        }
                        SplitMode.EVERY_N_PAGES -> {
                            val interval = everyN.coerceAtLeast(1)
                            for (p in 0 until totalSrcPages step interval) {
                                pagesToInclude.add(p)
                            }
                        }
                    }

                    if (pagesToInclude.isEmpty()) {
                        return@withContext PdfOperationResult(
                            isSuccess = false,
                            errorMessage = "No matching pages found for the specified range."
                        )
                    }

                    var outPageIdx = 0
                    for ((idx, pageNum) in pagesToInclude.withIndex()) {
                        if (!coroutineContext.isActive) return@withContext PdfOperationResult(
                            isSuccess = false,
                            errorMessage = "Operation cancelled"
                        )

                        renderer.openPage(pageNum).use { page ->
                            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, outPageIdx + 1).create()
                            val outPage = outDoc.startPage(pageInfo)
                            val canvas = outPage.canvas
                            canvas.drawColor(Color.WHITE)

                            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            canvas.drawBitmap(bmp, 0f, 0f, null)
                            bmp.recycle()

                            outDoc.finishPage(outPage)
                            outPageIdx++
                            onProgress(idx + 1, pagesToInclude.size)
                        }
                    }

                    FileOutputStream(outputFile).use { outDoc.writeTo(it) }
                    outDoc.close()

                    PdfOperationResult(
                        isSuccess = true,
                        outputFile = outputFile,
                        outputFileName = outputFile.name,
                        pageCount = outPageIdx,
                        fileSizeBytes = outputFile.length()
                    )
                }
            }
        } catch (e: Exception) {
            outDoc.close()
            outputFile.delete()
            PdfOperationResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Failed to split PDF."
            )
        }
    }

    /**
     * Parse page range string like "1-3, 5, 7-10" into 0-based indices
     */
    fun parsePageRanges(rangeStr: String, maxPages: Int): List<Int> {
        val result = mutableSetOf<Int>()
        val parts = rangeStr.split(",")
        for (part in parts) {
            val trimmed = part.trim()
            if (trimmed.contains("-")) {
                val bounds = trimmed.split("-")
                if (bounds.size == 2) {
                    val start = bounds[0].trim().toIntOrNull()
                    val end = bounds[1].trim().toIntOrNull()
                    if (start != null && end != null) {
                        val minP = minOf(start, end)
                        val maxP = maxOf(start, end)
                        for (p in minP..maxP) {
                            val zeroIndex = p - 1
                            if (zeroIndex in 0 until maxPages) result.add(zeroIndex)
                        }
                    }
                }
            } else {
                val p = trimmed.toIntOrNull()
                if (p != null) {
                    val zeroIndex = p - 1
                    if (zeroIndex in 0 until maxPages) result.add(zeroIndex)
                }
            }
        }
        return result.sorted()
    }

    /**
     * Compress PDF by re-sampling rendered pages with JPEG compression
     */
    suspend fun compressPdf(
        context: Context,
        inputFile: File,
        level: CompressionLevel,
        customQuality: Int = 65,
        onProgress: (current: Int, total: Int) -> Unit
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        val outputFile = FileUtils.createOutputPdfFile(context, "compressed_document")
        val outDoc = PdfDocument()

        val quality = if (level == CompressionLevel.CUSTOM) customQuality.coerceIn(15, 95) else level.jpegQuality
        val scale = level.scaleFactor

        try {
            ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val totalPages = renderer.pageCount

                    for (i in 0 until totalPages) {
                        if (!coroutineContext.isActive) return@withContext PdfOperationResult(
                            isSuccess = false,
                            errorMessage = "Operation cancelled"
                        )

                        renderer.openPage(i).use { page ->
                            val renderWidth = (page.width * scale).toInt().coerceAtLeast(300)
                            val renderHeight = (page.height * scale).toInt().coerceAtLeast(400)

                            val bmp = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.RGB_565)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            // Compress through JPEG stream
                            val stream = ByteArrayOutputStream()
                            bmp.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                            val compressedBytes = stream.toByteArray()
                            bmp.recycle()

                            val compressedBmp = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)

                            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                            val outPage = outDoc.startPage(pageInfo)
                            val pageCanvas = outPage.canvas
                            pageCanvas.drawBitmap(
                                compressedBmp,
                                null,
                                Rect(0, 0, page.width, page.height),
                                null
                            )
                            compressedBmp.recycle()

                            outDoc.finishPage(outPage)
                            onProgress(i + 1, totalPages)
                        }
                    }

                    FileOutputStream(outputFile).use { outDoc.writeTo(it) }
                    outDoc.close()

                    PdfOperationResult(
                        isSuccess = true,
                        outputFile = outputFile,
                        outputFileName = outputFile.name,
                        pageCount = totalPages,
                        fileSizeBytes = outputFile.length()
                    )
                }
            }
        } catch (e: Exception) {
            outDoc.close()
            outputFile.delete()
            PdfOperationResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Failed to compress PDF."
            )
        }
    }

    /**
     * Apply Watermark to PDF
     */
    suspend fun applyWatermark(
        context: Context,
        inputFile: File,
        config: WatermarkConfig,
        onProgress: (current: Int, total: Int) -> Unit
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        val outputFile = FileUtils.createOutputPdfFile(context, "watermarked_document")
        val outDoc = PdfDocument()

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = config.fontSize
            color = config.textColor
            alpha = (config.opacity * 255).toInt().coerceIn(0, 255)
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            isFakeBoldText = true
        }

        try {
            ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val totalPages = renderer.pageCount

                    for (i in 0 until totalPages) {
                        if (!coroutineContext.isActive) return@withContext PdfOperationResult(
                            isSuccess = false,
                            errorMessage = "Operation cancelled"
                        )

                        renderer.openPage(i).use { page ->
                            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                            val outPage = outDoc.startPage(pageInfo)
                            val canvas = outPage.canvas
                            canvas.drawColor(Color.WHITE)

                            // Render base page
                            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            canvas.drawBitmap(bmp, 0f, 0f, null)
                            bmp.recycle()

                            // Draw watermark
                            canvas.save()
                            val cx = when (config.position) {
                                WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> page.width * 0.25f
                                WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> page.width * 0.75f
                                else -> page.width / 2f
                            }
                            val cy = when (config.position) {
                                WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT -> page.height * 0.2f
                                WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_CENTER, WatermarkPosition.BOTTOM_RIGHT -> page.height * 0.85f
                                else -> page.height / 2f
                            }

                            canvas.rotate(config.rotationDegrees, cx, cy)
                            canvas.drawText(config.text, cx, cy, textPaint)
                            canvas.restore()

                            outDoc.finishPage(outPage)
                            onProgress(i + 1, totalPages)
                        }
                    }

                    FileOutputStream(outputFile).use { outDoc.writeTo(it) }
                    outDoc.close()

                    PdfOperationResult(
                        isSuccess = true,
                        outputFile = outputFile,
                        outputFileName = outputFile.name,
                        pageCount = totalPages,
                        fileSizeBytes = outputFile.length()
                    )
                }
            }
        } catch (e: Exception) {
            outDoc.close()
            outputFile.delete()
            PdfOperationResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Failed to add watermark."
            )
        }
    }

    /**
     * Add Page Numbers to PDF
     */
    suspend fun addPageNumbers(
        context: Context,
        inputFile: File,
        config: PageNumberConfig,
        onProgress: (current: Int, total: Int) -> Unit
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        val outputFile = FileUtils.createOutputPdfFile(context, "numbered_document")
        val outDoc = PdfDocument()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = config.fontSize
            color = Color.DKGRAY
            style = Paint.Style.FILL
        }

        try {
            ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val totalPages = renderer.pageCount

                    for (i in 0 until totalPages) {
                        if (!coroutineContext.isActive) return@withContext PdfOperationResult(
                            isSuccess = false,
                            errorMessage = "Operation cancelled"
                        )

                        renderer.openPage(i).use { page ->
                            val pageInfo = PdfDocument.PageInfo.Builder(page.width, page.height, i + 1).create()
                            val outPage = outDoc.startPage(pageInfo)
                            val canvas = outPage.canvas
                            canvas.drawColor(Color.WHITE)

                            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            canvas.drawBitmap(bmp, 0f, 0f, null)
                            bmp.recycle()

                            val currentNum = config.startNumber + i
                            val text = when (config.format) {
                                PageNumberFormat.NUMBER_ONLY -> "$currentNum"
                                PageNumberFormat.NUMBER_SLASH_TOTAL -> "$currentNum / $totalPages"
                                PageNumberFormat.PAGE_NUMBER -> "Page $currentNum"
                                PageNumberFormat.PAGE_NUMBER_OF_TOTAL -> "Page $currentNum of $totalPages"
                            }

                            val textWidth = paint.measureText(text)
                            val margin = 36f

                            val x = when (config.position) {
                                PageNumberPosition.TOP_LEFT, PageNumberPosition.BOTTOM_LEFT -> margin
                                PageNumberPosition.TOP_RIGHT, PageNumberPosition.BOTTOM_RIGHT -> page.width - margin - textWidth
                                else -> (page.width - textWidth) / 2f
                            }
                            val y = when (config.position) {
                                PageNumberPosition.TOP_LEFT, PageNumberPosition.TOP_CENTER, PageNumberPosition.TOP_RIGHT -> margin + config.fontSize
                                else -> page.height - margin
                            }

                            canvas.drawText(text, x, y, paint)
                            outDoc.finishPage(outPage)
                            onProgress(i + 1, totalPages)
                        }
                    }

                    FileOutputStream(outputFile).use { outDoc.writeTo(it) }
                    outDoc.close()

                    PdfOperationResult(
                        isSuccess = true,
                        outputFile = outputFile,
                        outputFileName = outputFile.name,
                        pageCount = totalPages,
                        fileSizeBytes = outputFile.length()
                    )
                }
            }
        } catch (e: Exception) {
            outDoc.close()
            outputFile.delete()
            PdfOperationResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Failed to add page numbers."
            )
        }
    }

    /**
     * Rotate PDF pages
     */
    suspend fun rotatePdf(
        context: Context,
        inputFile: File,
        angle: PageRotationAngle,
        selectedPages: Set<Int>, // 0-based
        rotateAll: Boolean = true,
        onProgress: (current: Int, total: Int) -> Unit
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        val outputFile = FileUtils.createOutputPdfFile(context, "rotated_document")
        val outDoc = PdfDocument()

        try {
            ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val totalPages = renderer.pageCount

                    for (i in 0 until totalPages) {
                        if (!coroutineContext.isActive) return@withContext PdfOperationResult(
                            isSuccess = false,
                            errorMessage = "Operation cancelled"
                        )

                        renderer.openPage(i).use { page ->
                            val shouldRotate = rotateAll || selectedPages.contains(i)
                            val rot = if (shouldRotate) angle.degrees else 0

                            val isSwapDims = (rot == 90 || rot == 270)
                            val outWidth = if (isSwapDims) page.height else page.width
                            val outHeight = if (isSwapDims) page.width else page.height

                            val pageInfo = PdfDocument.PageInfo.Builder(outWidth, outHeight, i + 1).create()
                            val outPage = outDoc.startPage(pageInfo)
                            val canvas = outPage.canvas
                            canvas.drawColor(Color.WHITE)

                            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)

                            if (rot != 0) {
                                val matrix = Matrix().apply {
                                    postRotate(rot.toFloat(), page.width / 2f, page.height / 2f)
                                    if (rot == 90) {
                                        postTranslate((outWidth - page.width) / 2f, (outHeight - page.height) / 2f)
                                    } else if (rot == 270) {
                                        postTranslate((outWidth - page.width) / 2f, (outHeight - page.height) / 2f)
                                    }
                                }
                                canvas.drawBitmap(bmp, matrix, null)
                            } else {
                                canvas.drawBitmap(bmp, 0f, 0f, null)
                            }
                            bmp.recycle()

                            outDoc.finishPage(outPage)
                            onProgress(i + 1, totalPages)
                        }
                    }

                    FileOutputStream(outputFile).use { outDoc.writeTo(it) }
                    outDoc.close()

                    PdfOperationResult(
                        isSuccess = true,
                        outputFile = outputFile,
                        outputFileName = outputFile.name,
                        pageCount = totalPages,
                        fileSizeBytes = outputFile.length()
                    )
                }
            }
        } catch (e: Exception) {
            outDoc.close()
            outputFile.delete()
            PdfOperationResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Failed to rotate PDF."
            )
        }
    }

    /**
     * Image to PDF
     */
    suspend fun imagesToPdf(
        context: Context,
        imageUris: List<Uri>,
        config: ImageToPdfConfig,
        onProgress: (current: Int, total: Int) -> Unit
    ): PdfOperationResult = withContext(Dispatchers.IO) {
        val outputFile = FileUtils.createOutputPdfFile(context, "images_to_pdf")
        val outDoc = PdfDocument()

        try {
            for ((idx, uri) in imageUris.withIndex()) {
                if (!coroutineContext.isActive) return@withContext PdfOperationResult(
                    isSuccess = false,
                    errorMessage = "Operation cancelled"
                )

                val bitmap = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: continue

                val (pageWidth, pageHeight) = when (config.pageSize) {
                    PdfPageSize.ORIGINAL -> bitmap.width to bitmap.height
                    else -> {
                        val isLandscape = when (config.orientation) {
                            PageOrientation.PORTRAIT -> false
                            PageOrientation.LANDSCAPE -> true
                            PageOrientation.AUTO -> bitmap.width > bitmap.height
                        }
                        if (isLandscape) config.pageSize.heightPoints to config.pageSize.widthPoints
                        else config.pageSize.widthPoints to config.pageSize.heightPoints
                    }
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx + 1).create()
                val page = outDoc.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                val margin = config.marginDp.toFloat()
                val availW = (pageWidth - margin * 2).coerceAtLeast(10f)
                val availH = (pageHeight - margin * 2).coerceAtLeast(10f)

                // Calculate aspect ratio fit
                val scale = minOf(availW / bitmap.width.toFloat(), availH / bitmap.height.toFloat())
                val destW = bitmap.width * scale
                val destH = bitmap.height * scale
                val left = margin + (availW - destW) / 2f
                val top = margin + (availH - destH) / 2f

                canvas.drawBitmap(bitmap, null, Rect(left.toInt(), top.toInt(), (left + destW).toInt(), (top + destH).toInt()), null)
                bitmap.recycle()

                outDoc.finishPage(page)
                onProgress(idx + 1, imageUris.size)
            }

            FileOutputStream(outputFile).use { outDoc.writeTo(it) }
            outDoc.close()

            PdfOperationResult(
                isSuccess = true,
                outputFile = outputFile,
                outputFileName = outputFile.name,
                pageCount = imageUris.size,
                fileSizeBytes = outputFile.length()
            )
        } catch (e: Exception) {
            outDoc.close()
            outputFile.delete()
            PdfOperationResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Failed to convert images to PDF."
            )
        }
    }

    /**
     * PDF to JPG: Render pages to JPEG files
     */
    suspend fun pdfToJpg(
        context: Context,
        inputFile: File,
        quality: Int = 85,
        onProgress: (current: Int, total: Int) -> Unit
    ): Pair<PdfOperationResult, List<File>> = withContext(Dispatchers.IO) {
        val outputImages = mutableListOf<File>()

        try {
            ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    val totalPages = renderer.pageCount

                    for (i in 0 until totalPages) {
                        if (!coroutineContext.isActive) return@withContext Pair(
                            PdfOperationResult(isSuccess = false, errorMessage = "Operation cancelled"),
                            emptyList()
                        )

                        renderer.openPage(i).use { page ->
                            val renderW = (page.width * 2).coerceIn(600, 2400)
                            val renderH = (page.height * 2).coerceIn(800, 3200)

                            val bmp = Bitmap.createBitmap(renderW, renderH, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(Color.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val imgFile = FileUtils.createOutputImageFile(context, i + 1)
                            FileOutputStream(imgFile).use { fos ->
                                bmp.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                            }
                            bmp.recycle()
                            outputImages.add(imgFile)

                            onProgress(i + 1, totalPages)
                        }
                    }

                    Pair(
                        PdfOperationResult(
                            isSuccess = true,
                            outputFile = outputImages.firstOrNull(),
                            outputFileName = "Exported ${outputImages.size} images",
                            pageCount = totalPages,
                            fileSizeBytes = outputImages.sumOf { it.length() }
                        ),
                        outputImages
                    )
                }
            }
        } catch (e: Exception) {
            Pair(
                PdfOperationResult(
                    isSuccess = false,
                    errorMessage = e.localizedMessage ?: "Failed to export PDF to JPG."
                ),
                emptyList()
            )
        }
    }
}
