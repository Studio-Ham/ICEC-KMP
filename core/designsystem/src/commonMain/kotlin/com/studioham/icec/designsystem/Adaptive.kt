package com.studioham.icec.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** #34: wide (Medium+, 600dp or more) or landscape phone (height under 480dp) uses the two-pane layout. */
fun isTwoPane(
    width: Dp,
    height: Dp,
): Boolean = width >= 600.dp || height < 480.dp

/** [isTwoPane] for the current window. */
@Composable
fun isTwoPane(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) { isTwoPane(size.width.toDp(), size.height.toDp()) }
}

/** #34: buttons and guide paragraphs stay centered at this width or narrower. */
val ContentMaxWidth = 480.dp

/** A separating hinge across an area: [vertical] splits left and right. [first] is the length before it, [gap] its width (px). */
data class HingeSplit(
    val vertical: Boolean,
    val first: Float,
    val gap: Float,
)

/** How the first of [hinges] (window px) that crosses [area] (window px) splits it, or null when none does. */
fun hingeSplit(
    area: Rect,
    hinges: List<Rect>,
): HingeSplit? {
    val hinge = hinges.firstOrNull { it.overlaps(area) } ?: return null
    return if (hinge.height >= hinge.width) {
        HingeSplit(true, (hinge.left - area.left).coerceIn(0f, area.width), hinge.width)
    } else {
        HingeSplit(false, (hinge.top - area.top).coerceIn(0f, area.height), hinge.height)
    }
}

/** Separating hinges (a half-opened fold or the gap between two screens) in window pixels; empty on iOS and flat screens. */
@Composable
expect fun separatingHinges(): List<Rect>
