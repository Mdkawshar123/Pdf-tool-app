package com.example.domain.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.ui.graphics.Color
import com.example.R

object PdfToolsList {

    val allTools: List<PdfTool> = listOf(
        // Popular Tools
        PdfTool(
            id = "merge",
            titleRes = R.string.tool_merge,
            descRes = R.string.tool_merge_desc,
            icon = Icons.Default.MergeType,
            category = ToolCategory.POPULAR,
            color = Color(0xFFEF4444) // Vibrant Red
        ),
        PdfTool(
            id = "split",
            titleRes = R.string.tool_split,
            descRes = R.string.tool_split_desc,
            icon = Icons.Default.CallSplit,
            category = ToolCategory.POPULAR,
            color = Color(0xFFF97316) // Vibrant Orange
        ),
        PdfTool(
            id = "compress",
            titleRes = R.string.tool_compress,
            descRes = R.string.tool_compress_desc,
            icon = Icons.Default.Compress,
            category = ToolCategory.POPULAR,
            color = Color(0xFF0EA5E9) // Sky Blue
        ),
        PdfTool(
            id = "image_to_pdf",
            titleRes = R.string.tool_image_to_pdf,
            descRes = R.string.tool_image_to_pdf_desc,
            icon = Icons.Default.Image,
            category = ToolCategory.POPULAR,
            color = Color(0xFF10B981) // Emerald
        ),
        PdfTool(
            id = "pdf_to_jpg",
            titleRes = R.string.tool_pdf_to_jpg,
            descRes = R.string.tool_pdf_to_jpg_desc,
            icon = Icons.Default.PictureAsPdf,
            category = ToolCategory.POPULAR,
            color = Color(0xFF8B5CF6) // Purple
        ),
        PdfTool(
            id = "edit_pdf",
            titleRes = R.string.tool_edit_pdf,
            descRes = R.string.tool_edit_pdf_desc,
            icon = Icons.Default.Edit,
            category = ToolCategory.EDIT,
            color = Color(0xFFEC4899) // Pink
        ),

        // Convert Tools
        PdfTool(
            id = "word_to_pdf",
            titleRes = R.string.tool_word_to_pdf,
            descRes = R.string.tool_word_to_pdf_desc,
            icon = Icons.Default.Description,
            category = ToolCategory.CONVERT,
            color = Color(0xFF2563EB) // Royal Blue
        ),
        PdfTool(
            id = "ppt_to_pdf",
            titleRes = R.string.tool_ppt_to_pdf,
            descRes = R.string.tool_ppt_to_pdf_desc,
            icon = Icons.Default.Slideshow,
            category = ToolCategory.CONVERT,
            color = Color(0xFFEA580C) // Rust
        ),
        PdfTool(
            id = "excel_to_pdf",
            titleRes = R.string.tool_excel_to_pdf,
            descRes = R.string.tool_excel_to_pdf_desc,
            icon = Icons.Default.TableChart,
            category = ToolCategory.CONVERT,
            color = Color(0xFF059669) // Green
        ),
        PdfTool(
            id = "pdf_to_word",
            titleRes = R.string.tool_pdf_to_word,
            descRes = R.string.tool_pdf_to_word_desc,
            icon = Icons.Default.Description,
            category = ToolCategory.CONVERT,
            color = Color(0xFF3B82F6) // Blue
        ),
        PdfTool(
            id = "pdf_to_ppt",
            titleRes = R.string.tool_pdf_to_ppt,
            descRes = R.string.tool_pdf_to_ppt_desc,
            icon = Icons.Default.Slideshow,
            category = ToolCategory.CONVERT,
            color = Color(0xFFF59E0B) // Amber
        ),
        PdfTool(
            id = "pdf_to_excel",
            titleRes = R.string.tool_pdf_to_excel,
            descRes = R.string.tool_pdf_to_excel_desc,
            icon = Icons.Default.TableChart,
            category = ToolCategory.CONVERT,
            color = Color(0xFF14B8A6) // Teal
        ),

        // Organize & Edit Tools
        PdfTool(
            id = "watermark",
            titleRes = R.string.tool_watermark,
            descRes = R.string.tool_watermark_desc,
            icon = Icons.Default.WaterDamage,
            category = ToolCategory.ORGANIZE,
            color = Color(0xFF6366F1) // Indigo
        ),
        PdfTool(
            id = "page_numbers",
            titleRes = R.string.tool_page_numbers,
            descRes = R.string.tool_page_numbers_desc,
            icon = Icons.Default.FormatListNumbered,
            category = ToolCategory.ORGANIZE,
            color = Color(0xFF0D9488) // Dark Teal
        ),
        PdfTool(
            id = "rotate",
            titleRes = R.string.tool_rotate_pdf,
            descRes = R.string.tool_rotate_pdf_desc,
            icon = Icons.Default.RotateRight,
            category = ToolCategory.ORGANIZE,
            color = Color(0xFFD97706) // Ochre
        ),
        PdfTool(
            id = "viewer",
            titleRes = R.string.tool_viewer,
            descRes = R.string.tool_viewer_desc,
            icon = Icons.Default.Visibility,
            category = ToolCategory.POPULAR,
            color = Color(0xFF475569) // Slate
        )
    )

    fun getToolById(id: String): PdfTool? = allTools.find { it.id == id }
}
