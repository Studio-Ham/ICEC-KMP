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

/**
 * [isTwoPane] with room for a side column next to the photo: a narrow split window (under 580dp wide, under 480dp
 * tall) stays in one column (Wiki 적응형 배치: 좁은 분할 창은 아래).
 */
fun isSideBySide(
    width: Dp,
    height: Dp,
): Boolean = isTwoPane(width, height) && width >= 580.dp

/** [isSideBySide] for the current window. */
@Composable
fun isSideBySide(): Boolean {
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) { isSideBySide(size.width.toDp(), size.height.toDp()) }
}

/** #34: buttons and guide paragraphs stay centered at this width or narrower. */
val ContentMaxWidth = 480.dp

/** A separating hinge across an area: [vertical] splits left and right. [first] is the length before it, [gap] its width (px). */
data class HingeSplit(
    val vertical: Boolean,
    val first: Float,
    val gap: Float,
)

/**
 * How the first of [hinges] (window px) that cuts [area] (window px) in two splits it, or null when none does. The axis
 * is the hinge's own (taller than wide is vertical). A hinge counts only when it runs across the whole area and leaves at
 * least [minPane] px on both sides; a hinge that only touches an edge or a corner leaves the normal layout in place.
 */
fun hingeSplit(
    area: Rect,
    hinges: List<Rect>,
    minPane: Float = 0f,
): HingeSplit? =
    hinges.firstNotNullOfOrNull { hinge ->
        val vertical = hinge.height >= hinge.width
        val across =
            if (vertical) {
                hinge.top <= area.top && hinge.bottom >= area.bottom
            } else {
                hinge.left <= area.left &&
                    hinge.right >= area.right
            }
        val first = if (vertical) hinge.left - area.left else hinge.top - area.top
        val gap = if (vertical) hinge.width else hinge.height
        val second = (if (vertical) area.width else area.height) - first - gap
        HingeSplit(vertical, first, gap).takeIf { across && first > 0 && second > 0 && minOf(first, second) >= minPane }
    }

/** Separating hinges (a half-opened fold or the gap between two screens) in window pixels; empty on iOS and flat screens. */
@Composable
expect fun separatingHinges(): List<Rect>
