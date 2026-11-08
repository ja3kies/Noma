package dev.juuri.noma.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * Noma's M3/M3E semantic palette. Surfaces, type scale and shape roles are
 * centralized so feature composables never need literal colors.
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF425F80),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE7F4),
    onPrimaryContainer = Color(0xFF203A57),
    secondary = Color(0xFF6B6260),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF2E9E4),
    onSecondaryContainer = Color(0xFF4E4543),
    tertiary = Color(0xFF83562C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE3C4),
    onTertiaryContainer = Color(0xFF603A12),
    background = Color(0xFFF9F8F5),
    onBackground = Color(0xFF23292E),
    surface = Color(0xFFFFFEFB),
    onSurface = Color(0xFF23292E),
    surfaceVariant = Color(0xFFF0F0ED),
    onSurfaceVariant = Color(0xFF5E666A),
    outline = Color(0xFF899098),
    outlineVariant = Color(0xFFDDE0E0),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB2CBE9),
    onPrimary = Color(0xFF19334F),
    primaryContainer = Color(0xFF2E4865),
    onPrimaryContainer = Color(0xFFD6E6FA),
    secondary = Color(0xFFD8C6BE),
    onSecondary = Color(0xFF43342F),
    secondaryContainer = Color(0xFF524844),
    onSecondaryContainer = Color(0xFFF5E6DF),
    tertiary = Color(0xFFE8C29B),
    onTertiary = Color(0xFF482B0E),
    tertiaryContainer = Color(0xFF664522),
    onTertiaryContainer = Color(0xFFFFE2C1),
    background = Color(0xFF14191E),
    onBackground = Color(0xFFE6E9ED),
    surface = Color(0xFF1E242A),
    onSurface = Color(0xFFE6E9ED),
    surfaceVariant = Color(0xFF303840),
    onSurfaceVariant = Color(0xFFC0C7CE),
    outline = Color(0xFF929CA5),
    outlineVariant = Color(0xFF3D464E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val NomaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

private val NomaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 31.sp,
        lineHeight = 39.sp,
        letterSpacing = (-0.7).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
)

@Composable
fun NomaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = NomaTypography,
        shapes = NomaShapes,
        content = content
    )
}
