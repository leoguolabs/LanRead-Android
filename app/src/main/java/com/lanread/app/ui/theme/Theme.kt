package com.lanread.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// iOS system blue
val AccentBlue = Color(0xFF007AFF)
val ReaderLightBg = Color(0xFFFAFAFA)
val ReaderDarkBg = Color(0xFF0D0D12)
val CardGrayLight = Color(0xFFF2F2F7)
val CardGrayDark = Color(0xFF1C1C22)

private val LightColors = lightColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = CardGrayLight,
    onPrimaryContainer = Color(0xFF007AFF),
    secondary = AccentBlue,
    onSecondary = Color.White,
    secondaryContainer = CardGrayLight,
    onSecondaryContainer = Color(0xFF000000),
    background = Color.White,
    onBackground = Color(0xFF000000),
    surface = Color.White,
    onSurface = Color(0xFF000000),
    surfaceVariant = CardGrayLight,
    onSurfaceVariant = Color(0xFF3C3C43),
    outline = Color(0xFFC7C7CC),
    outlineVariant = Color(0xFFE5E5EA)
)

private val DarkColors = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color.White,
    primaryContainer = CardGrayDark,
    onPrimaryContainer = AccentBlue,
    secondary = AccentBlue,
    onSecondary = Color.White,
    secondaryContainer = CardGrayDark,
    onSecondaryContainer = Color.White,
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = CardGrayDark,
    onSurfaceVariant = Color(0xFFEBEBF5),
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF2C2C2E)
)

val LanReadTypography = Typography(
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 17.sp),
    bodyMedium = TextStyle(fontSize = 15.sp),
    bodySmall = TextStyle(fontSize = 12.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun LanReadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        typography = LanReadTypography,
        content = {
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.material3.LocalContentColor provides colors.onBackground
            ) {
                androidx.compose.material3.Surface(
                    color = colors.background,
                    contentColor = colors.onBackground
                ) {
                    content()
                }
            }
        }
    )
}
