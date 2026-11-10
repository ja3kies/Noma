package dev.juuri.noma.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure layout checks: no device or persistence required. */
class ResponsiveListPaneTest {
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
