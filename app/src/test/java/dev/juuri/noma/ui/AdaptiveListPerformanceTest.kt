package dev.juuri.noma.ui

import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Test

/** Independent guards for large notebook animations and responsive tablet geometry. */
class AdaptiveListPerformanceTest {
    @Test fun zeroItemsEnableSimpleAnimation() {
        assertTrue(shouldAnimateNomaListItems(0))
    }
    @Test fun singleItemEnablesAnimation() {
        assertTrue(shouldAnimateNomaListItems(1))
    }
    @Test fun mediumNotebookEnablesAnimation() {
        assertTrue(shouldAnimateNomaListItems(40))
    }
    @Test fun justBelowAnimationCapEnablesAnimation() {
        assertTrue(shouldAnimateNomaListItems(79))
    }
    @Test fun animationCapIsInclusive() {
        assertTrue(shouldAnimateNomaListItems(80))
    }
    @Test fun firstLargeNotebookDisablesAnimation() {
        assertFalse(shouldAnimateNomaListItems(81))
    }
    @Test fun largeNotebookDisablesAnimation() {
        assertFalse(shouldAnimateNomaListItems(500))
    }
    @Test fun extremelyLargeNotebookDisablesAnimation() {
        assertFalse(shouldAnimateNomaListItems(100_000))
    }
    @Test fun invalidNegativeCountDoesNotAnimate() {
        assertFalse(shouldAnimateNomaListItems(-1))
    }
    @Test fun firstTabletWidthEnablesTwoPanes() {
        assertTrue(shouldShowNomaTwoPane(840.dp))
    }
    @Test fun justBelowBreakpointKeepsSinglePane() {
        assertFalse(shouldShowNomaTwoPane(839.dp))
    }
    @Test fun midPhoneStaysSinglePane() {
        assertFalse(shouldShowNomaTwoPane(480.dp))
    }
    @Test fun firstTabletWidthKeepsMinimumListSize() {
        assertEquals(320.dp, responsiveListPaneWidth(840.dp))
    }
    @Test fun mediumTabletScalesListProportionally() {
        assertEquals(360.dp, responsiveListPaneWidth(1000.dp))
    }
    @Test fun wideTabletBoundsListWidth() {
        assertEquals(420.dp, responsiveListPaneWidth(1400.dp))
    }
    @Test fun largeDesktopBoundsListWidth() {
        assertEquals(420.dp, responsiveListPaneWidth(3000.dp))
    }
    @Test fun compactGutterStaysSmallBelowWideBreakpoint() {
        assertEquals(8.dp, nomaPaneGutter(1199.dp))
    }
    @Test fun wideGutterBeginsAtBreakpoint() {
        assertEquals(16.dp, nomaPaneGutter(1200.dp))
    }
    @Test fun compactMarginStaysSmallBelowWideBreakpoint() {
        assertEquals(8.dp, nomaTabletMargin(1199.dp))
    }
    @Test fun wideMarginBeginsAtBreakpoint() {
        assertEquals(16.dp, nomaTabletMargin(1200.dp))
    }
    @Test fun compactCornersStayRounded() {
        assertEquals(22.dp, nomaPaneCornerRadius(1199.dp))
    }
    @Test fun wideCornersBecomeLarger() {
        assertEquals(28.dp, nomaPaneCornerRadius(1200.dp))
    }
}
