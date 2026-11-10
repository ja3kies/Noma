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
}
