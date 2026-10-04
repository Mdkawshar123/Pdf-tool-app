package com.example.presentation.tools

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PdfMasterApplication
import com.example.core.pdf.DocumentConversionEngine
import com.example.core.pdf.PdfProcessingEngine
import com.example.core.storage.FileUtils
import com.example.domain.model.CompressionLevel
import com.example.domain.model.ImageToPdfConfig
import com.example.domain.model.PageNumberConfig
import com.example.domain.model.PageNumberFormat
import com.example.domain.model.PageNumberPosition
import com.example.domain.model.PageOrientation
import com.example.domain.model.PageRotationAngle
import com.example.domain.model.PdfFileItem
import com.example.domain.model.PdfOperationResult
import com.example.domain.model.PdfPageSize
import com.example.domain.model.SplitMode
import com.example.domain.model.WatermarkConfig
import com.example.domain.model.WatermarkPosition
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ToolRunnerUiState(
    val toolId: String = "",
    val selectedFiles: List<PdfFileItem> = emptyList(),
    val isProcessing: Boolean = false,
    val progressCurrent: Int = 0,
    val progressTotal: Int = 0,
    val progressPercent: Float = 0f,
    val progressStatusText: String = "",
    val result: PdfOperationResult? = null,
    val exportedImages: List<File> = emptyList(),
    val errorMessage: String? = null,

    // Tool specific configs
    val compressionLevel: CompressionLevel = CompressionLevel.RECOMMENDED,
    val customCompressionQuality: Int = 65,

    val splitMode: SplitMode = SplitMode.ALL_PAGES,
    val splitCustomRange: String = "1-3",
    val splitSelectedPages: Set<Int> = emptySet(),
    val splitEveryN: Int = 1,

    val watermarkText: String = "CONFIDENTIAL",
    val watermarkFontSize: Float = 42f,
    val watermarkOpacity: Float = 0.35f,
    val watermarkRotation: Float = -45f,
    val watermarkPosition: WatermarkPosition = WatermarkPosition.CENTER,

    val pageNumberStart: Int = 1,
    val pageNumberFormat: PageNumberFormat = PageNumberFormat.PAGE_NUMBER_OF_TOTAL,
    val pageNumberPosition: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER,

    val rotateAngle: PageRotationAngle = PageRotationAngle.DEGREES_90,
    val rotateAllPages: Boolean = true,
    val rotateSelectedPages: Set<Int> = emptySet(),

    val imagePageSize: PdfPageSize = PdfPageSize.A4,
    val imageOrientation: PageOrientation = PageOrientation.AUTO,
    val imageMarginDp: Int = 16,

    val isExcelLandscape: Boolean = false
)

class ToolRunnerViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PdfMasterApplication
    private val historyRepo = app.historyRepository

    private val _uiState = MutableStateFlow(ToolRunnerUiState())
    val uiState: StateFlow<ToolRunnerUiState> = _uiState.asStateFlow()

    private var currentJob: Job? = null

    fun setToolId(id: String) {
        _uiState.update { it.copy(toolId = id, result = null, errorMessage = null) }
    }

    fun addFiles(uris: List<Uri>) {
        viewModelScope.launch {
            val items = uris.map { uri ->
                val name = FileUtils.getFileName(app, uri)
                val size = FileUtils.getFileSize(app, uri)
                val tempFile = FileUtils.copyUriToTempFile(app, uri)
                val pages = if (name.endsWith(".pdf", ignoreCase = true)) {
                    PdfProcessingEngine.getPdfPageCount(tempFile)
                } else 1
                PdfFileItem(uri = uri, name = name, sizeBytes = size, pageCount = pages, localCachedFile = tempFile)
            }
            _uiState.update { current ->
                current.copy(selectedFiles = current.selectedFiles + items, errorMessage = null)
            }
        }
    }

    fun removeFile(index: Int) {
        _uiState.update { current ->
            val list = current.selectedFiles.toMutableList()
            if (index in list.indices) {
                list[index].localCachedFile?.delete()
                list.removeAt(index)
            }
            current.copy(selectedFiles = list)
        }
    }

    fun moveFile(from: Int, to: Int) {
        _uiState.update { current ->
            val list = current.selectedFiles.toMutableList()
            if (from in list.indices && to in list.indices) {
                val item = list.removeAt(from)
                list.add(to, item)
            }
            current.copy(selectedFiles = list)
        }
    }

    fun clearFiles() {
        _uiState.value.selectedFiles.forEach { it.localCachedFile?.delete() }
        _uiState.update { it.copy(selectedFiles = emptyList(), result = null, errorMessage = null) }
    }

    // Config setters
    fun setCompressionLevel(level: CompressionLevel) = _uiState.update { it.copy(compressionLevel = level) }
    fun setCustomQuality(q: Int) = _uiState.update { it.copy(customCompressionQuality = q) }

    fun setSplitMode(mode: SplitMode) = _uiState.update { it.copy(splitMode = mode) }
    fun setSplitRange(range: String) = _uiState.update { it.copy(splitCustomRange = range) }
    fun toggleSplitPage(pageIdx: Int) = _uiState.update { current ->
        val set = current.splitSelectedPages.toMutableSet()
        if (set.contains(pageIdx)) set.remove(pageIdx) else set.add(pageIdx)
        current.copy(splitSelectedPages = set)
    }

    fun setWatermarkText(text: String) = _uiState.update { it.copy(watermarkText = text) }
    fun setWatermarkPosition(pos: WatermarkPosition) = _uiState.update { it.copy(watermarkPosition = pos) }
    fun setWatermarkOpacity(op: Float) = _uiState.update { it.copy(watermarkOpacity = op) }
    fun setWatermarkRotation(rot: Float) = _uiState.update { it.copy(watermarkRotation = rot) }

    fun setPageNumberPosition(pos: PageNumberPosition) = _uiState.update { it.copy(pageNumberPosition = pos) }
    fun setPageNumberFormat(fmt: PageNumberFormat) = _uiState.update { it.copy(pageNumberFormat = fmt) }
    fun setPageNumberStart(start: Int) = _uiState.update { it.copy(pageNumberStart = start) }

    fun setRotateAngle(angle: PageRotationAngle) = _uiState.update { it.copy(rotateAngle = angle) }
    fun setRotateAll(all: Boolean) = _uiState.update { it.copy(rotateAllPages = all) }

    fun setImagePageSize(size: PdfPageSize) = _uiState.update { it.copy(imagePageSize = size) }
    fun setImageOrientation(ori: PageOrientation) = _uiState.update { it.copy(imageOrientation = ori) }
    fun setExcelLandscape(ls: Boolean) = _uiState.update { it.copy(isExcelLandscape = ls) }

    fun cancelOperation() {
        currentJob?.cancel()
        app.isProcessingPdf = false
        _uiState.update { it.copy(isProcessing = false, errorMessage = "Operation cancelled by user.") }
    }

    fun executeOperation() {
        val state = _uiState.value
        val files = state.selectedFiles
        if (files.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Please select at least one file to process.") }
            return
        }

        currentJob?.cancel()
        app.isProcessingPdf = true

        _uiState.update {
            it.copy(
                isProcessing = true,
                progressCurrent = 0,
                progressTotal = 1,
                progressPercent = 0f,
                progressStatusText = "Initializing...",
                errorMessage = null,
                result = null
            )
        }

        currentJob = viewModelScope.launch {
            try {
                val onProgress: (Int, Int) -> Unit = { cur, tot ->
                    _uiState.update {
                        it.copy(
                            progressCurrent = cur,
                            progressTotal = tot,
                            progressPercent = if (tot > 0) cur.toFloat() / tot.toFloat() else 0f,
                            progressStatusText = "Processing page $cur of $tot"
                        )
                    }
                }

                val primaryFile = files.first().localCachedFile ?: FileUtils.copyUriToTempFile(app, files.first().uri)

                val result: PdfOperationResult = when (state.toolId) {
                    "merge" -> {
                        val inputFiles = files.map { it.localCachedFile ?: FileUtils.copyUriToTempFile(app, it.uri) }
                        PdfProcessingEngine.mergePdfs(app, inputFiles, onProgress)
                    }
                    "split" -> {
                        PdfProcessingEngine.splitPdf(
                            app, primaryFile, state.splitMode, state.splitCustomRange,
                            state.splitSelectedPages, state.splitEveryN, onProgress
                        )
                    }
                    "compress" -> {
                        PdfProcessingEngine.compressPdf(
                            app, primaryFile, state.compressionLevel, state.customCompressionQuality, onProgress
                        )
                    }
                    "watermark" -> {
                        val config = WatermarkConfig(
                            text = state.watermarkText,
                            opacity = state.watermarkOpacity,
                            rotationDegrees = state.watermarkRotation,
                            position = state.watermarkPosition
                        )
                        PdfProcessingEngine.applyWatermark(app, primaryFile, config, onProgress)
                    }
                    "page_numbers" -> {
                        val config = PageNumberConfig(
                            startNumber = state.pageNumberStart,
                            format = state.pageNumberFormat,
                            position = state.pageNumberPosition
                        )
                        PdfProcessingEngine.addPageNumbers(app, primaryFile, config, onProgress)
                    }
                    "rotate" -> {
                        PdfProcessingEngine.rotatePdf(
                            app, primaryFile, state.rotateAngle, state.rotateSelectedPages, state.rotateAllPages, onProgress
                        )
                    }
                    "image_to_pdf" -> {
                        val config = ImageToPdfConfig(
                            pageSize = state.imagePageSize,
                            orientation = state.imageOrientation,
                            marginDp = state.imageMarginDp
                        )
                        PdfProcessingEngine.imagesToPdf(app, files.map { it.uri }, config, onProgress)
                    }
                    "pdf_to_jpg" -> {
                        val (res, images) = PdfProcessingEngine.pdfToJpg(app, primaryFile, 85, onProgress)
                        _uiState.update { it.copy(exportedImages = images) }
                        res
                    }
                    "word_to_pdf" -> DocumentConversionEngine.wordToPdf(app, primaryFile, onProgress)
                    "ppt_to_pdf" -> DocumentConversionEngine.pptToPdf(app, primaryFile, onProgress)
                    "excel_to_pdf" -> DocumentConversionEngine.excelToPdf(app, primaryFile, state.isExcelLandscape, onProgress)
                    "pdf_to_word" -> {
                        val conv = DocumentConversionEngine.pdfToWord(app, primaryFile, onProgress)
                        PdfOperationResult(
                            isSuccess = conv.isSuccess,
                            outputFile = conv.outputFile,
                            outputFileName = conv.outputFileName,
                            errorMessage = conv.errorMessage,
                            fileSizeBytes = conv.outputFile?.length() ?: 0L
                        )
                    }
                    "pdf_to_ppt" -> {
                        val conv = DocumentConversionEngine.pdfToPpt(app, primaryFile, onProgress)
                        PdfOperationResult(
                            isSuccess = conv.isSuccess,
                            outputFile = conv.outputFile,
                            outputFileName = conv.outputFileName,
                            errorMessage = conv.errorMessage,
                            fileSizeBytes = conv.outputFile?.length() ?: 0L
                        )
                    }
                    "pdf_to_excel" -> {
                        val conv = DocumentConversionEngine.pdfToExcel(app, primaryFile, onProgress)
                        PdfOperationResult(
                            isSuccess = conv.isSuccess,
                            outputFile = conv.outputFile,
                            outputFileName = conv.outputFileName,
                            errorMessage = conv.errorMessage,
                            fileSizeBytes = conv.outputFile?.length() ?: 0L
                        )
                    }
                    else -> PdfOperationResult(isSuccess = false, errorMessage = "Unsupported tool operation.")
                }

                app.isProcessingPdf = false
                _uiState.update { it.copy(isProcessing = false, result = result) }

                // Record history if successful
                if (result.isSuccess && result.outputFile != null) {
                    historyRepo.recordOperation(
                        operationName = state.toolId.replace("_", " ").replaceFirstChar { it.uppercase() },
                        operationType = state.toolId,
                        originalFileName = files.first().name,
                        outputFileName = result.outputFileName,
                        filePath = result.outputFile.absolutePath,
                        fileSizeBytes = result.fileSizeBytes,
                        pageCount = result.pageCount,
                        status = "Completed"
                    )
                }
            } catch (e: Exception) {
                app.isProcessingPdf = false
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = e.localizedMessage ?: "An unexpected error occurred during processing."
                    )
                }
            }
        }
    }
}
