package com.example.presentation.editor

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.PdfMasterApplication
import com.example.core.storage.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EditorToolType {
    PEN,
    HIGHLIGHTER,
    TEXT,
    SIGNATURE,
    DATE_STAMP,
    CHECKBOX
}

data class DrawPath(
    val points: List<Offset>,
    val color: Int,
    val strokeWidth: Float,
    val isHighlighter: Boolean = false,
    val text: String? = null,
    val isStamp: Boolean = false
)

data class PdfEditorUiState(
    val filePath: String = "",
    val fileName: String = "",
    val currentPageIndex: Int = 0,
    val pageCount: Int = 0,
    val currentBitmap: Bitmap? = null,
    val activeTool: EditorToolType = EditorToolType.PEN,
    val selectedColor: Int = Color.RED,
    val strokeWidth: Float = 6f,
    val paths: List<DrawPath> = emptyList(),
    val undoStack: List<List<DrawPath>> = emptyList(),
    val redoStack: List<List<DrawPath>> = emptyList(),
    val isSaving: Boolean = false,
    val savedFilePath: String? = null
)

class PdfEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PdfMasterApplication
    private val historyRepo = app.historyRepository

    private val _uiState = MutableStateFlow(PdfEditorUiState())
    val uiState: StateFlow<PdfEditorUiState> = _uiState.asStateFlow()

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var activePage: PdfRenderer.Page? = null

    fun loadPdf(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(filePath = path, fileName = File(path).name) }
            withContext(Dispatchers.IO) {
                try {
                    val file = File(path)
                    fileDescriptor?.close()
                    fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    pdfRenderer = PdfRenderer(fileDescriptor!!)
                    val count = pdfRenderer?.pageCount ?: 0
                    _uiState.update { it.copy(pageCount = count, currentPageIndex = 0) }
                    renderPage(0)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun renderPage(index: Int) {
        val renderer = pdfRenderer ?: return
        try {
            activePage?.close()
            val page = renderer.openPage(index)
            activePage = page

            val bmp = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            _uiState.update {
                it.copy(
                    currentPageIndex = index,
                    currentBitmap = bmp,
                    paths = emptyList(),
                    undoStack = emptyList(),
                    redoStack = emptyList()
                )
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun setTool(tool: EditorToolType) {
        _uiState.update {
            val color = when (tool) {
                EditorToolType.HIGHLIGHTER -> Color.argb(120, 255, 235, 59)
                EditorToolType.PEN -> Color.RED
                EditorToolType.SIGNATURE -> Color.BLUE
                else -> it.selectedColor
            }
            val width = if (tool == EditorToolType.HIGHLIGHTER) 24f else 6f
            it.copy(activeTool = tool, selectedColor = color, strokeWidth = width)
        }
    }

    fun setColor(color: Int) = _uiState.update { it.copy(selectedColor = color) }

    fun addPath(path: DrawPath) {
        _uiState.update { current ->
            val newUndo = current.undoStack + listOf(current.paths)
            current.copy(
                paths = current.paths + path,
                undoStack = newUndo,
                redoStack = emptyList()
            )
        }
    }

    fun addStamp(text: String, position: Offset) {
        val path = DrawPath(
            points = listOf(position),
            color = _uiState.value.selectedColor,
            strokeWidth = 2f,
            text = text,
            isStamp = true
        )
        addPath(path)
    }

    fun addDateStamp(position: Offset) {
        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        addStamp(dateStr, position)
    }

    fun undo() {
        val current = _uiState.value
        if (current.undoStack.isNotEmpty()) {
            val previousPaths = current.undoStack.last()
            val newUndo = current.undoStack.dropLast(1)
            val newRedo = current.redoStack + listOf(current.paths)
            _uiState.update {
                it.copy(paths = previousPaths, undoStack = newUndo, redoStack = newRedo)
            }
        }
    }

    fun redo() {
        val current = _uiState.value
        if (current.redoStack.isNotEmpty()) {
            val nextPaths = current.redoStack.last()
            val newRedo = current.redoStack.dropLast(1)
            val newUndo = current.undoStack + listOf(current.paths)
            _uiState.update {
                it.copy(paths = nextPaths, undoStack = newUndo, redoStack = newRedo)
            }
        }
    }

    fun clearAllAnnotations() {
        _uiState.update {
            it.copy(
                paths = emptyList(),
                undoStack = it.undoStack + listOf(it.paths),
                redoStack = emptyList()
            )
        }
    }

    fun saveAnnotatedPdf(onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val state = _uiState.value
            val baseBmp = state.currentBitmap ?: return@launch

            withContext(Dispatchers.IO) {
                try {
                    val outputFile = FileUtils.createOutputPdfFile(app, "edited_document")
                    val doc = PdfDocument()

                    val pageInfo = PdfDocument.PageInfo.Builder(baseBmp.width, baseBmp.height, 1).create()
                    val page = doc.startPage(pageInfo)
                    val canvas = page.canvas

                    // Draw base rendered page
                    canvas.drawBitmap(baseBmp, 0f, 0f, null)

                    // Draw user annotations
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }

                    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        textSize = 24f
                        style = Paint.Style.FILL
                        color = Color.DKGRAY
                    }

                    for (item in state.paths) {
                        if (item.isStamp && item.text != null && item.points.isNotEmpty()) {
                            val pt = item.points.first()
                            canvas.drawText(item.text, pt.x, pt.y, textPaint)
                        } else if (item.points.size > 1) {
                            paint.color = item.color
                            paint.strokeWidth = item.strokeWidth
                            val path = Path()
                            path.moveTo(item.points[0].x, item.points[0].y)
                            for (i in 1 until item.points.size) {
                                path.lineTo(item.points[i].x, item.points[i].y)
                            }
                            canvas.drawPath(path, paint)
                        }
                    }

                    doc.finishPage(page)
                    FileOutputStream(outputFile).use { doc.writeTo(it) }
                    doc.close()

                    historyRepo.recordOperation(
                        operationName = "Edit PDF",
                        operationType = "edit_pdf",
                        originalFileName = state.fileName,
                        outputFileName = outputFile.name,
                        filePath = outputFile.absolutePath,
                        fileSizeBytes = outputFile.length(),
                        pageCount = 1,
                        status = "Completed"
                    )

                    _uiState.update { it.copy(isSaving = false, savedFilePath = outputFile.absolutePath) }
                    onSuccess(outputFile.absolutePath)
                } catch (e: Exception) {
                    _uiState.update { it.copy(isSaving = false) }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activePage?.close()
        pdfRenderer?.close()
        fileDescriptor?.close()
    }
}
