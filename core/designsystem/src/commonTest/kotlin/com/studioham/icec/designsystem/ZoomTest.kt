package com.studioham.icec.designsystem

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals

class ZoomTest {
    private val viewport = Size(200f, 200f)
    private val fit = Rect(0f, 50f, 200f, 150f) // a 2:1 photo letterboxed in a square viewport

    @Test
    fun noPanAtOneX() {
        val zoom = ZoomState()
        zoom.onGesture(Offset(100f, 100f), pan = Offset(30f, 30f), zoom = 1f, fit, viewport)
        assertEquals(Offset.Zero, zoom.offset)
        assertEquals(fit, zoom.transform(fit, viewport))
    }

    @Test
    fun zoomStopsAtFourX() {
        val zoom = ZoomState()
        zoom.onGesture(Offset(100f, 100f), Offset.Zero, zoom = 10f, fit, viewport)
        assertEquals(ZoomState.MAX_SCALE, zoom.scale)
    }

    @Test
    fun panNeverUncoversEmptySpace() {
        // At 2x the photo is 400 x 200: 100 px of slack on each side horizontally, none vertically.
        assertEquals(Offset(100f, 0f), clampOffset(Offset(500f, 80f), 2f, fit.size, viewport))
        assertEquals(Offset(-100f, 0f), clampOffset(Offset(-500f, -80f), 2f, fit.size, viewport))
        val zoom = ZoomState(scale = 2f)
        zoom.onGesture(Offset(100f, 100f), pan = Offset(500f, 0f), zoom = 1f, fit, viewport)
        assertEquals(Rect(0f, 0f, 400f, 200f), zoom.transform(fit, viewport)) // left edge stays at the viewport edge
    }

    @Test
    fun zoomKeepsThePointUnderTheFingers() {
        val zoom = ZoomState()
        val finger = Offset(150f, 100f)
        val before = zoom.transform(fit, viewport)
        val fraction = (finger.x - before.left) / before.width
        zoom.onGesture(finger, Offset.Zero, zoom = 2f, fit, viewport)
        val after = zoom.transform(fit, viewport)
        assertEquals(finger.x, after.left + fraction * after.width, 0.01f)
    }
}
