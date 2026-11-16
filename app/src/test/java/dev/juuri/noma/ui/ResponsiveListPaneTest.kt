package dev.juuri.noma.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure layout checks: no device or persistence required. */
class ResponsiveListPaneTest {
    @Test fun familyPreviewGridAvoidsCrampedLabelsOnSmallScreens() {
        assertEquals(1, nomaThemeFamilyColumns(320.dp, 1f))
        assertEquals(1, nomaThemeFamilyColumns(360.dp, 1f))
        assertEquals(1, nomaThemeFamilyColumns(393.dp, 1.5f))
        assertEquals(1, nomaThemeFamilyColumns(560.dp, 2f))
        assertEquals(2, nomaThemeFamilyColumns(393.dp, 1f))
        assertEquals(2, nomaThemeFamilyColumns(560.dp, 1.2f))
    }

    @Test fun themeModeOptionsStayReadableAtCompactWidthsAndLargeText() {
        assertEquals(1, nomaAppearanceColumns(320.dp, 1f))
        assertEquals(1, nomaAppearanceColumns(393.dp, 1f))
        assertEquals(1, nomaAppearanceColumns(560.dp, 2f))
        assertEquals(2, nomaAppearanceColumns(480.dp, 1f))
        assertEquals(3, nomaAppearanceColumns(560.dp, 1f))
    }

    @Test fun accentSwatchesWrapOnPhoneAndInLargeText() {
        assertEquals(3, nomaAccentColumns(320.dp, 1f))
        assertEquals(3, nomaAccentColumns(393.dp, 1f))
        assertEquals(3, nomaAccentColumns(560.dp, 2f))
        assertEquals(6, nomaAccentColumns(560.dp, 1f))
    }

    @Test fun minimumListWidthAtCompactTabletBreakpoint() {
        assertEquals(320.dp, responsiveListPaneWidth(840.dp))
    }

    @Test fun proportionalListWidthForMediumTablet() {
        assertEquals(360.dp, responsiveListPaneWidth(1000.dp))
    }

    @Test fun maximumListWidthKeepsWritingPaneWide() {
        assertEquals(420.dp, responsiveListPaneWidth(1600.dp))
    }

    @Test fun listWidthNeverExceedsMaximumForLargeDesktopWindow() {
        assertEquals(420.dp, responsiveListPaneWidth(3200.dp))
    }

    @Test fun phoneWindowUsesOnePaneUntilBreakpoint() {
        assertEquals(false, shouldShowNomaTwoPane(839.dp))
    }

    @Test fun tabletWindowUsesTwoPanesAtBreakpoint() {
        assertEquals(true, shouldShowNomaTwoPane(840.dp))
    }

    @Test fun wideWindowUsesTwoPanes() {
        assertEquals(true, shouldShowNomaTwoPane(1600.dp))
    }

    @Test fun compactTabletUsesSmallPaneSpacing() {
        assertEquals(8.dp, nomaPaneGutter(1000.dp))
        assertEquals(8.dp, nomaTabletMargin(1000.dp))
        assertEquals(22.dp, nomaPaneCornerRadius(1000.dp))
    }

    @Test fun wideTabletUsesComfortablePaneSpacing() {
        assertEquals(16.dp, nomaPaneGutter(1200.dp))
        assertEquals(16.dp, nomaTabletMargin(1200.dp))
        assertEquals(28.dp, nomaPaneCornerRadius(1200.dp))
    }
}
