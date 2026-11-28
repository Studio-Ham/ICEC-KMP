package com.studioham.icec.designsystem

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdaptiveTest {
    @Test
    fun twoPaneOnlyForWideOrShortWindows() {
        assertFalse(isTwoPane(390.dp, 844.dp)) // portrait phone
        assertFalse(isTwoPane(599.dp, 480.dp))
        assertTrue(isTwoPane(844.dp, 390.dp)) // landscape phone
        assertTrue(isTwoPane(600.dp, 960.dp)) // portrait tablet / unfolded foldable
        assertTrue(isTwoPane(500.dp, 479.dp))
    }

    @Test
    fun sideBySideNeedsRoomForTheSideColumn() {
        assertTrue(isSideBySide(844.dp, 390.dp)) // landscape phone
        assertTrue(isSideBySide(600.dp, 960.dp)) // portrait tablet / unfolded foldable
        assertFalse(isSideBySide(411.dp, 400.dp)) // narrow split window stays one column
        assertFalse(isSideBySide(390.dp, 844.dp)) // portrait phone
    }

    @Test
    fun hingeSplitsOnlyTheAreaItCrosses() {
        val area = Rect(0f, 100f, 1000f, 800f)
        // Book posture or dual screen: a 26px vertical hinge at x=480.
        assertEquals(HingeSplit(vertical = true, first = 480f, gap = 26f), hingeSplit(area, listOf(Rect(480f, 0f, 506f, 900f))))
        // Tabletop or flip: a zero-width horizontal fold line at y=450.
        assertEquals(HingeSplit(vertical = false, first = 350f, gap = 0f), hingeSplit(area, listOf(Rect(0f, 450f, 1000f, 450f))))
        // A hinge outside the area (for example under the top bar) does not split it.
        assertNull(hingeSplit(area, listOf(Rect(0f, 40f, 1000f, 60f))))
        assertNull(hingeSplit(area, emptyList()))
    }

    @Test
    fun hingeOnAnEdgeOrCornerKeepsTheNormalLayout() {
        val area = Rect(0f, 100f, 1000f, 800f)
        // Across the right edge: the palette side would get 0px (PR #64 re-review 3.3).
        assertNull(hingeSplit(area, listOf(Rect(990f, 0f, 1016f, 900f))))
        // Across the top edge: the photo side would get 0px.
        assertNull(hingeSplit(area, listOf(Rect(0f, 80f, 1000f, 106f))))
        // A vertical hinge that only reaches 10px into the area stays vertical and does not cut the area in two.
        assertNull(hingeSplit(area, listOf(Rect(480f, 0f, 506f, 110f))))
        // A pane smaller than minPane is not usable.
        assertNull(hingeSplit(area, listOf(Rect(100f, 0f, 126f, 900f)), minPane = 200f))
        assertEquals(
            HingeSplit(vertical = true, first = 480f, gap = 26f),
            hingeSplit(area, listOf(Rect(480f, 0f, 506f, 900f)), minPane = 200f),
        )
    }
}
