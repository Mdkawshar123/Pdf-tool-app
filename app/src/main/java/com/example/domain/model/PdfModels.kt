package com.example.domain.model

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File

enum class ToolCategory(@StringRes val titleRes: Int) {
    POPULAR(com.example.R.string.category_popular),
    CONVERT(com.example.R.string.category_convert),
    EDIT(com.example.R.string.category_edit),
    ORGANIZE(com.example.R.string.category_organize)
}

data class PdfTool(
    val id: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val icon: ImageVector,
    val category: ToolCategory,
    val color: Color
)

data class PdfFileItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val pageCount: Int = 0,
    val localCachedFile: File? = null
)

data class PdfOperationResult(
    val isSuccess: Boolean,
    val outputFile: File? = null,
    val outputUri: Uri? = null,
    val outputFileName: String = "",
    val errorMessage: String? = null,
    val pageCount: Int = 1,
    val fileSizeBytes: Long = 0L
)

enum class CompressionLevel(val scaleFactor: Float, val jpegQuality: Int, val label: String) {
    MAXIMUM(0.5f, 40, "Maximum Compression (Smallest Size)"),
    RECOMMENDED(0.75f, 65, "Recommended (Good Balance)"),
    HIGH_QUALITY(0.9f, 85, "High Quality (Best Visuals)"),
    CUSTOM(0.7f, 60, "Custom Compression")
}

enum class WatermarkPosition(val label: String) {
    TOP_LEFT("Top Left"),
    TOP_CENTER("Top Center"),
    TOP_RIGHT("Top Right"),
    CENTER("Center"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right")
}

data class WatermarkConfig(
    val text: String = "CONFIDENTIAL",
    val fontSize: Float = 48f,
    val textColor: Int = android.graphics.Color.RED,
    val opacity: Float = 0.35f,
    val rotationDegrees: Float = -45f,
    val position: WatermarkPosition = WatermarkPosition.CENTER,
    val isAllPages: Boolean = true,
    val imageUri: Uri? = null
)

enum class PageNumberPosition(val label: String) {
    TOP_LEFT("Top Left"),
    TOP_CENTER("Top Center"),
    TOP_RIGHT("Top Right"),
    BOTTOM_LEFT("Bottom Left"),
    BOTTOM_CENTER("Bottom Center"),
    BOTTOM_RIGHT("Bottom Right")
}

enum class PageNumberFormat(val label: String) {
    NUMBER_ONLY("1"),
    NUMBER_SLASH_TOTAL("1 / 10"),
    PAGE_NUMBER("Page 1"),
    PAGE_NUMBER_OF_TOTAL("Page 1 of 10")
}

data class PageNumberConfig(
    val startNumber: Int = 1,
    val fontSize: Float = 14f,
    val position: PageNumberPosition = PageNumberPosition.BOTTOM_CENTER,
    val format: PageNumberFormat = PageNumberFormat.PAGE_NUMBER_OF_TOTAL,
    val isAllPages: Boolean = true
)

enum class SplitMode(val label: String) {
    ALL_PAGES("Extract All Pages Individually"),
    CUSTOM_RANGE("Custom Page Range (e.g. 1-3, 5)"),
    EXTRACT_SELECTED("Extract Selected Pages"),
    EVERY_N_PAGES("Split Every N Pages")
}

enum class PageRotationAngle(val degrees: Int) {
    DEGREES_90(90),
    DEGREES_180(180),
    DEGREES_270(270)
}

enum class PdfPageSize(val widthPoints: Int, val heightPoints: Int, val label: String) {
    A4(595, 842, "A4 (210 x 297 mm)"),
    A5(420, 595, "A5 (148 x 210 mm)"),
    LETTER(612, 792, "US Letter (8.5 x 11 in)"),
    LEGAL(612, 1008, "US Legal (8.5 x 14 in)"),
    ORIGINAL(0, 0, "Match Image Aspect Ratio")
}

enum class PageOrientation {
    AUTO,
    PORTRAIT,
    LANDSCAPE
}

data class ImageToPdfConfig(
    val pageSize: PdfPageSize = PdfPageSize.A4,
    val orientation: PageOrientation = PageOrientation.AUTO,
    val marginDp: Int = 16,
    val compressQuality: Int = 85
)
