package dev.juuri.noma.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/** Regression checks for the six user-selectable Noma color palettes. */
class NomaAccentTest {
    @Test fun missingOrUnknownPreferenceUsesForest() {
        assertEquals(NomaAccent.FOREST, NomaAccent.fromStorage(null))
        assertEquals(NomaAccent.FOREST, NomaAccent.fromStorage(""))
        assertEquals(NomaAccent.FOREST, NomaAccent.fromStorage("UNRECOGNIZED"))
    }

    @Test fun everyAccentRoundTripsThroughStoredEnumName() {
        NomaAccent.entries.forEach { accent ->
            assertEquals(accent, NomaAccent.fromStorage(accent.name))
        }
    }

    @Test fun everyAccentHasItsOwnPrimaryColor() {
        assertEquals(6, NomaAccent.entries.size)
        assertEquals(6, NomaAccent.entries.map { it.lightPrimary }.distinct().size)
        assertEquals(6, NomaAccent.entries.map { it.darkPrimary }.distinct().size)
    }

    @Test fun selectedForegroundColorPassesBodyTextContrast() {
        NomaAccent.entries.forEach { accent ->
            val light = contrast(accent.lightPrimary, Color.White)
            val dark = contrast(accent.darkPrimary, Color(0xFF17271C))
            assertTrue("${accent.name} light primary contrast is $light", light >= 4.5)
            assertTrue("${accent.name} dark primary contrast is $dark", dark >= 4.5)
        }
    }

    @Test fun semanticSurfacesRemainReadableForEveryThemeAndAccent() {
        NomaAccent.entries.forEach { accent ->
            listOf(false, true).forEach { dark ->
                val colors = nomaColorScheme(dark, accent)
                listOf(
                    colors.onBackground to colors.background,
                    colors.onSurface to colors.surfaceContainerLow,
                    colors.onSurfaceVariant to colors.surfaceContainerHighest,
                    colors.onPrimaryContainer to colors.primaryContainer,
                    colors.onSecondaryContainer to colors.secondaryContainer
                ).forEach { (ink, paper) ->
                    assertTrue("$accent dark=$dark contrast", contrast(ink, paper) >= 4.5)
                }
                assertTrue("error container $accent dark=$dark", contrast(colors.onErrorContainer, colors.errorContainer) >= 4.5)
                assertEquals(colors.primary, colors.surfaceTint)
            }
        }
    }

    @Test fun themeFamilyMigrationDefaultsUnknownValuesAndRoundTrips() {
        assertEquals(NomaThemeFamily.ACCENT_WHITE, NomaThemeFamily.fromStorage(null))
        assertEquals(NomaThemeFamily.ACCENT_WHITE, NomaThemeFamily.fromStorage("old-value"))
        NomaThemeFamily.entries.forEach { assertEquals(it, NomaThemeFamily.fromStorage(it.name)) }
    }

    @Test fun monochromeFamiliesKeepSurfacesGrayAndDifferByAccentRole() {
        listOf(false, true).forEach { dark ->
            val pure = nomaColorScheme(dark, NomaAccent.CORAL, NomaThemeFamily.BW_PURE)
            val accented = nomaColorScheme(dark, NomaAccent.CORAL, NomaThemeFamily.BW_ACCENT)
            assertEquals(pure.background, accented.background)
            assertEquals(pure.surface, accented.surface)
            monochromeRoles(pure).forEach { color ->
                assertEquals("red/green in $color", color.red, color.green, 0f)
                assertEquals("green/blue in $color", color.green, color.blue, 0f)
            }
            monochromeRoles(accented).forEach { color ->
                assertEquals("BW_ACCENT red/green in $color", color.red, color.green, 0f)
                assertEquals("BW_ACCENT green/blue in $color", color.green, color.blue, 0f)
            }
            assertEquals(NomaAccent.CORAL.let { if (dark) it.darkPrimary else it.lightPrimary }, accented.primary)
            assertTrue(contrast(accented.primary, accented.onPrimary) >= 4.5)
            assertTrue(contrast(pure.onSurface, pure.surface) >= 4.5)
        }
    }

    @Test fun everyAccentChoosesReadableForegroundInBothModesAndFamilies() {
        NomaAccent.entries.forEach { accent ->
            listOf(false, true).forEach { dark ->
                listOf(NomaThemeFamily.BW_ACCENT, NomaThemeFamily.ACCENT_WHITE).forEach { family ->
                    val scheme = nomaColorScheme(dark, accent, family)
                    assertTrue("$family $accent dark=$dark", contrast(scheme.primary, scheme.onPrimary) >= 4.5)
                }
            }
        }
    }

    private fun monochromeRoles(scheme: androidx.compose.material3.ColorScheme) = listOf(
        scheme.onPrimary, scheme.primaryContainer, scheme.onPrimaryContainer,
        scheme.secondary, scheme.onSecondary, scheme.secondaryContainer, scheme.onSecondaryContainer,
        scheme.tertiary, scheme.onTertiary, scheme.tertiaryContainer, scheme.onTertiaryContainer,
        scheme.background, scheme.onBackground, scheme.surface, scheme.onSurface, scheme.surfaceVariant,
        scheme.onSurfaceVariant, scheme.surfaceTint, scheme.inverseSurface, scheme.inverseOnSurface,
        scheme.error, scheme.onError, scheme.errorContainer, scheme.onErrorContainer, scheme.outline,
        scheme.outlineVariant, scheme.scrim, scheme.surfaceBright, scheme.surfaceDim, scheme.surfaceContainer,
        scheme.surfaceContainerHigh, scheme.surfaceContainerHighest, scheme.surfaceContainerLow,
        scheme.surfaceContainerLowest
    )

    private fun contrast(first: Color, second: Color): Double {
        val a = luminance(first)
        val b = luminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private fun luminance(color: Color): Double {
        fun linear(component: Float): Double {
            val v = component.toDouble()
            return if (v <= 0.04045) v / 12.92
            else ((v + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) +
            0.7152 * linear(color.green) +
            0.0722 * linear(color.blue)
    }
}
