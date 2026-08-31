package com.lorbit.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian & Neon Dark Palette
val ObsidianBg = Color(0xFF0B0F19)
val ObsidianSurface = Color(0xFF131B2E)
val GlassDarkSurface = Color(0x331E293B) // 20% opacity translucent
val GlassDarkBorder = Color(0x40FFFFFF) // 25% specular light border

// Accent Neon Colors
val NeonCyan = Color(0xFF00F0FF)
val NeonPurple = Color(0xFFB026FF)
val NeonAmber = Color(0xFFFFB800)
val NeonEmerald = Color(0xFF00FFA3)
val NeonRose = Color(0xFFFF2E93)
val NeonBlue = Color(0xFF3B82F6)

// Text Colors
val TextPrimary = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

// Glass Gradients
val LiquidMeshGradient = Brush.radialGradient(
    colors = listOf(
        Color(0x30B026FF),
        Color(0x2000F0FF),
        Color(0x000B0F19)
    ),
    radius = 1200f
)

val CyanPurpleGradient = Brush.horizontalGradient(
    colors = listOf(NeonCyan, NeonPurple)
)

val GlassCardBorderGradient = Brush.linearGradient(
    colors = listOf(
        Color.White.copy(alpha = 0.40f),
        Color.White.copy(alpha = 0.08f),
        Color.White.copy(alpha = 0.20f)
    )
)