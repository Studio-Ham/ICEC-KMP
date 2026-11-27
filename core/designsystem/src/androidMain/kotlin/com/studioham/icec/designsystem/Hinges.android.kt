package com.studioham.icec.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

@Composable
actual fun separatingHinges(): List<Rect> {
    val context = LocalContext.current
    val layout by remember(context) { WindowInfoTracker.getOrCreate(context).windowLayoutInfo(context) }.collectAsState(null)
    return layout
        ?.displayFeatures
        .orEmpty()
        .filterIsInstance<FoldingFeature>()
        .filter { it.isSeparating }
        .map { it.bounds.toComposeRect() }
}
