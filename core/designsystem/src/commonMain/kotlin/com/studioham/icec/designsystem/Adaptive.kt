package com.studioham.icec.designsystem

import androidx.compose.runtime.Composable
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
