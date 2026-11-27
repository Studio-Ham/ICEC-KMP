package com.studioham.icec.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect

/** iPhone and iPad have no hinge; Split View and Stage Manager only change the window size. */
@Composable
actual fun separatingHinges(): List<Rect> = emptyList()
