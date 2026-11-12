package dev.juuri.noma.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.lerp
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

enum class NomaAppearance {
    SYSTEM, LIGHT, DARK;

    companion object {
        fun fromStorage(value: String?): NomaAppearance =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

/**
 * Optional accent palettes from the Noma Reborn design exploration.
 * Every light primary is dark enough for white text; every dark primary uses
 * dark on-primary ink. Shared semantic surfaces remain independent of accent.
 */
enum class NomaAccent(
    val lightPrimary: Color,
    val lightContainer: Color,
    val darkPrimary: Color,
    val darkContainer: Color
) {
    FOREST(Color(0xFF456C52), Color(0xFFDCEACF), Color(0xFFB1D6B6), Color(0xFF31523C)),
    SAGE(Color(0xFF5E6D45), Color(0xFFE3E9D5), Color(0xFFCAD8AB), Color(0xFF3B4932)),
    HONEY(Color(0xFF80601B), Color(0xFFF9EDC2), Color(0xFFEFCB79), Color(0xFF655023)),
    CORAL(Color(0xFF8D533F), Color(0xFFFBE5DB), Color(0xFFE8B6A3), Color(0xFF674338)),
    LILAC(Color(0xFF755681), Color(0xFFEFE4F2), Color(0xFFD7B9E2), Color(0xFF533C5C)),
    SKY(Color(0xFF4E6687), Color(0xFFE0EAF8), Color(0xFFB6CEF1), Color(0xFF364B66));

    companion object {
        fun fromStorage(value: String?): NomaAccent =
            entries.firstOrNull { it.name == value } ?: FOREST
    }
}

/** Consistent spacing and motion roles shared across all Noma surfaces. */
object NomaSpacing {
    val tiny = 4.dp
    val small = 8.dp
    val medium = 12.dp
    val comfortable = 16.dp
    val section = 24.dp
    val page = 16.dp
    val listPaneWidth = 400.dp
    val editorMaxWidth = 700.dp
    val touchTarget = 48.dp
}

object NomaMotion {
    const val quick = 160
    const val standard = 230
}

// Noma Reborn: botanical surfaces and warm paper, not a replica of another brand.
// High-contrast semantic roles also keep previews, errors and TalkBack readable.
private val LightColors = lightColorScheme(
    primary = Color(0xFF456C52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCEACF),
    onPrimaryContainer = Color(0xFF193622),
    secondary = Color(0xFF516A53),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7EFE4),
    onSecondaryContainer = Color(0xFF273C2B),
    tertiary = Color(0xFF835B3E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8D5),
    onTertiaryContainer = Color(0xFF50331F),
    background = Color(0xFFF6F7F2),
    onBackground = Color(0xFF1D2B23),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D2B23),
    surfaceVariant = Color(0xFFE5EAE2),
    onSurfaceVariant = Color(0xFF536257),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFCFDF9),
    surfaceContainer = Color(0xFFF0F3EC),
    surfaceContainerHigh = Color(0xFFE7EDE4),
    surfaceContainerHighest = Color(0xFFDCE4DA),
    outline = Color(0xFF637565),
    outlineVariant = Color(0xFFD8E0D6),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB1D6B6),
    onPrimary = Color(0xFF193A26),
    primaryContainer = Color(0xFF31523C),
    onPrimaryContainer = Color(0xFFD2F0D5),
    secondary = Color(0xFFB7D1BA),
    onSecondary = Color(0xFF263C2C),
    secondaryContainer = Color(0xFF3A5140),
    onSecondaryContainer = Color(0xFFD8EBDD),
    tertiary = Color(0xFFF0BE91),
    onTertiary = Color(0xFF4D2F17),
    tertiaryContainer = Color(0xFF67452A),
    onTertiaryContainer = Color(0xFFFFE0C6),
    background = Color(0xFF121D16),
    onBackground = Color(0xFFE5EFE6),
    surface = Color(0xFF1B2B21),
    onSurface = Color(0xFFE5EFE6),
    surfaceVariant = Color(0xFF35473A),
    onSurfaceVariant = Color(0xFFCAD8CC),
    surfaceContainerLowest = Color(0xFF0F1913),
    surfaceContainerLow = Color(0xFF1B2920),
    surfaceContainer = Color(0xFF213428),
    surfaceContainerHigh = Color(0xFF2B3D31),
    surfaceContainerHighest = Color(0xFF36493A),
    outline = Color(0xFFA0B1A2),
    outlineVariant = Color(0xFF425547),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

private val NomaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val NomaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.7).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp
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
        lineHeight = 22.sp
    )
)

@Composable
fun isNomaDark(appearance: NomaAppearance): Boolean {
    val systemIsDark = isSystemInDarkTheme()
    return when (appearance) {
        NomaAppearance.SYSTEM -> systemIsDark
        NomaAppearance.LIGHT -> false
        NomaAppearance.DARK -> true
    }
}

/** Shared by the live application and appearance previews. */
internal fun nomaColorScheme(dark: Boolean, accent: NomaAccent): ColorScheme {
    val base = if (dark) DarkColors else LightColors
    val primary = if (dark) accent.darkPrimary else accent.lightPrimary
    val container = if (dark) accent.darkContainer else accent.lightContainer
    val ink = if (dark) Color(0xFFE5EFE6) else Color(0xFF193622)
    return base.copy(
        primary = primary,
        onPrimary = if (dark) Color(0xFF17271C) else Color.White,
        primaryContainer = container,
        onPrimaryContainer = ink,
        secondary = primary,
        onSecondary = if (dark) Color(0xFF17271C) else Color.White,
        secondaryContainer = container,
        onSecondaryContainer = ink,
        surfaceTint = primary,
        inversePrimary = if (dark) accent.lightPrimary else accent.darkPrimary,
        background = lerp(base.background, container, 0.08f),
        surfaceContainerLow = lerp(base.surfaceContainerLow, container, 0.08f),
        surfaceContainer = lerp(base.surfaceContainer, container, 0.12f),
        surfaceContainerHigh = lerp(base.surfaceContainerHigh, container, 0.16f),
        surfaceContainerHighest = lerp(base.surfaceContainerHighest, container, 0.20f)
    )
}

@Composable
fun NomaTheme(
    appearance: NomaAppearance = NomaAppearance.SYSTEM,
    accent: NomaAccent = NomaAccent.FOREST,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = nomaColorScheme(isNomaDark(appearance), accent),
        typography = NomaTypography,
        shapes = NomaShapes,
        content = content
    )
}
