package com.example.presentation.viewer

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class PdfViewerUiState(
    val filePath: String = "",
    val fileName: String = "",
    val pageCount: Int = 0,
    val currentPage: Int = 0,
    val currentBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val zoomScale: Float = 1.0f,
    val rotationDegrees: Int = 0,
    val isBookmarked: Boolean = false,
    val isFullscreen: Boolean = false,
    val errorMessage: String? = null
)

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PdfViewerUiState())
    val uiState: StateFlow<PdfViewerUiState> = _uiState.asStateFlow()

    private var fileDescriptor: ParcelFileDescriptor? = null
    private var pdfRenderer: PdfRenderer? = null
    private var activePage: PdfRenderer.Page? = null

    fun loadPdf(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, filePath = path, fileName = File(path).name) }
            try {
                withContext(Dispatchers.IO) {
                    val file = File(path)
                    if (!file.exists()) {
                        _uiState.update { it.copy(isLoading = false, errorMessage = "File not found: ${file.name}") }
                        return@withContext
                    }
                    closeRenderer()

                    fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    pdfRenderer = PdfRenderer(fileDescriptor!!)
                    val count = pdfRenderer?.pageCount ?: 0

                    _uiState.update {
                        it.copy(
                            pageCount = count,
                            currentPage = 0,
                            isLoading = false
                        )
                    }
                    renderCurrentPage(0)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not open PDF: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun nextPage() {
        val next = _uiState.value.currentPage + 1
        if (next < _uiState.value.pageCount) {
            renderCurrentPage(next)
        }
    }

    fun previousPage() {
        val prev = _uiState.value.currentPage - 1
        if (prev >= 0) {
            renderCurrentPage(prev)
        }
    }

    fun jumpToPage(pageIndex: Int) {
        if (pageIndex in 0 until _uiState.value.pageCount) {
            renderCurrentPage(pageIndex)
        }
    }

    fun rotate() {
        val next = (_uiState.value.rotationDegrees + 90) % 360
        _uiState.update { it.copy(rotationDegrees = next) }
    }

    fun zoomIn() {
        _uiState.update { it.copy(zoomScale = (it.zoomScale + 0.25f).coerceAtMost(4.0f)) }
    }

    fun zoomOut() {
        _uiState.update { it.copy(zoomScale = (it.zoomScale - 0.25f).coerceAtLeast(1.0f)) }
    }

    fun resetZoom() {
        _uiState.update { it.copy(zoomScale = 1.0f) }
    }

    fun toggleBookmark() {
        _uiState.update { it.copy(isBookmarked = !it.isBookmarked) }
    }

    fun toggleFullscreen() {
        _uiState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    private fun renderCurrentPage(index: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val renderer = pdfRenderer ?: return@launch
            try {
                activePage?.close()
                val page = renderer.openPage(index)
                activePage = page

                val renderWidth = (page.width * 2).coerceIn(720, 2160)
                val renderHeight = (page.height * 2).coerceIn(960, 2880)

                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                _uiState.update {
                    it.copy(
                        currentPage = index,
                        currentBitmap = bitmap,
                        zoomScale = 1.0f
                    )
                }
            } catch (e: Exception) {
                // Ignore rendering error
            }
        }
    }

    private fun closeRenderer() {
        activePage?.close()
        activePage = null
        pdfRenderer?.close()
        pdfRenderer = null
        fileDescriptor?.close()
        fileDescriptor = null
    }

    override fun onCleared() {
        super.onCleared()
        closeRenderer()
    }
}
