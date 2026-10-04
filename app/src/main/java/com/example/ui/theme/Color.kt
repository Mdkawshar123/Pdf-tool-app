package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Light Theme Colors - Modern Crimson & Executive Slate
val CrimsonPrimaryLight = Color(0xFFD32F2F)
val CrimsonOnPrimaryLight = Color(0xFFFFFFFF)
val CrimsonPrimaryContainerLight = Color(0xFFFFEBEE)
val CrimsonOnPrimaryContainerLight = Color(0xFF5C000B)

val SlateSecondaryLight = Color(0xFF334155)
val SlateOnSecondaryLight = Color(0xFFFFFFFF)
val SlateSecondaryContainerLight = Color(0xFFF1F5F9)
val SlateOnSecondaryContainerLight = Color(0xFF0F172A)

val AmberTertiaryLight = Color(0xFFD97706)
val AmberOnTertiaryLight = Color(0xFFFFFFFF)
val AmberTertiaryContainerLight = Color(0xFFFEF3C7)
val AmberOnTertiaryContainerLight = Color(0xFF78350F)

val NeutralBackgroundLight = Color(0xFFF8FAFC)
val NeutralOnBackgroundLight = Color(0xFF0F172A)
val NeutralSurfaceLight = Color(0xFFFFFFFF)
val NeutralOnSurfaceLight = Color(0xFF0F172A)
val NeutralSurfaceVariantLight = Color(0xFFF1F5F9)
val NeutralOnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight = Color(0xFFE2E8F0)
val OutlineVariantLight = Color(0xFFF1F5F9)

// Dark Theme Colors - Deep Obsidian & Glowing Crimson
val CrimsonPrimaryDark = Color(0xFFFF5252)
val CrimsonOnPrimaryDark = Color(0xFF4D0008)
val CrimsonPrimaryContainerDark = Color(0xFF8B0014)
val CrimsonOnPrimaryContainerDark = Color(0xFFFFCDD2)

val SlateSecondaryDark = Color(0xFF94A3B8)
val SlateOnSecondaryDark = Color(0xFF0F172A)
val SlateSecondaryContainerDark = Color(0xFF1E293B)
val SlateOnSecondaryContainerDark = Color(0xFFF8FAFC)

val AmberTertiaryDark = Color(0xFFFBBF24)
val AmberOnTertiaryDark = Color(0xFF451A03)
val AmberTertiaryContainerDark = Color(0xFF78350F)
val AmberOnTertiaryContainerDark = Color(0xFFFEF3C7)

val NeutralBackgroundDark = Color(0xFF0B0F19)
val NeutralOnBackgroundDark = Color(0xFFF8FAFC)
val NeutralSurfaceDark = Color(0xFF111827)
val NeutralOnSurfaceDark = Color(0xFFF8FAFC)
val NeutralSurfaceVariantDark = Color(0xFF1F2937)
val NeutralOnSurfaceVariantDark = Color(0xFF9CA3AF)
val OutlineDark = Color(0xFF374151)
val OutlineVariantDark = Color(0xFF1F2937)

// Status & Semantic Colors
val SuccessGreen = Color(0xFF10B981)
val SuccessContainerLight = Color(0xFFDCFCE7)
val SuccessContainerDark = Color(0xFF064E3B)
val WarningAmber = Color(0xFFF59E0B)
val ErrorRed = Color(0xFFEF4444)
val InfoBlue = Color(0xFF3B82F6)

// Professional Gradient Brushes
val PrimaryCrimsonGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C))
)

val DarkMeshGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF111827), Color(0xFF0B0F19))
)

val SurfaceGlassGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFFFFFFFF).copy(alpha = 0.95f), Color(0xFFF8FAFC).copy(alpha = 0.85f))
)
