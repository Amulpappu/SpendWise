package com.example.smartexpensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Premium Fintech Palette (Emerald, Deep Slate, Indigo & Coral Accents)
val PrimaryEmerald = Color(0xFF10B981)
val PrimaryEmeraldLight = Color(0xFF34D399)
val PrimaryEmeraldDark = Color(0xFF059669)

val SecondaryTeal = Color(0xFF06B6D4)
val AccentIndigo = Color(0xFF6366F1)
val AccentPurple = Color(0xFF8B5CF6)
val AccentPink = Color(0xFFF43F5E)
val AccentOrange = Color(0xFFFB923C)
val DangerRed = Color(0xFFF87171)
val SuccessGreen = Color(0xFF10B981)
val WarningYellow = Color(0xFFFBBF24)

val BackgroundDark = Color(0xFF0B0F19)
val SurfaceDark = Color(0xFF161E2E)
val SurfaceVariantDark = Color(0xFF222F43)
val CardBackgroundDark = Color(0xFF1C2638)

val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFF1F5F9)
val CardBackgroundLight = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryEmeraldLight,
    onPrimary = Color(0xFF0B0F19),
    primaryContainer = PrimaryEmeraldDark.copy(alpha = 0.3f),
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    tertiary = AccentIndigo,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryEmeraldDark,
    onPrimary = Color.White,
    primaryContainer = PrimaryEmerald.copy(alpha = 0.15f),
    secondary = SecondaryTeal,
    onSecondary = Color.White,
    tertiary = AccentIndigo,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF1E293B)
)

val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp
    )
)

@Composable
fun SmartExpenseTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
