package com.studioham.icec.designsystem

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.toSize

/**
 * Pinch zoom and pan for a photo fitted in a viewport (Wiki Editor and Result UX): up to 4x, no pan at 1x, and the
 * photo never uncovers empty space while zoomed. Callers draw through [transform], so the photo, face guides and
 * mosaic share one transform while stroke widths stay in screen pixels.
 */
@Stable
class ZoomState(
    scale: Float = 1f,
    offset: Offset = Offset.Zero,
) {
    var scale by mutableFloatStateOf(scale)
        private set
    var offset by mutableStateOf(offset)
        private set

    /**
     * Where [fit] (the photo at 1x, inside a [viewport]-sized box) is drawn now. The offset is clamped to this [fit] and
     * [viewport], so a turned photo or a smaller window never uncovers empty space.
     */
    fun transform(
        fit: Rect,
        viewport: Size,
    ): Rect {
        val center = Offset(viewport.width / 2, viewport.height / 2)
        val size = fit.size * scale
        val topLeft = center + (fit.topLeft - center) * scale + clampOffset(offset, scale, fit.size, viewport)
        return Rect(topLeft, size)
    }

    /** Where an [image]-sized photo is drawn now in a [viewport]: [transform] of its fitted rect. Screens draw and hit-test with it. */
    fun photoRect(
        image: Size,
        viewport: Size,
    ): Rect = transform(fitRect(image, viewport), viewport)

    /** Applies one gesture step: [zoom] around [centroid], then [pan], clamped to the photo. */
    fun onGesture(
        centroid: Offset,
        pan: Offset,
        zoom: Float,
        fit: Rect,
        viewport: Size,
    ) {
        val newScale = (scale * zoom).coerceIn(1f, MAX_SCALE)
        val center = Offset(viewport.width / 2, viewport.height / 2)
        val anchor = centroid - center
        // Start from where the photo is drawn now (see transform), then keep the point under the fingers still while scaling.
        val current = clampOffset(offset, scale, fit.size, viewport)
        val zoomed = anchor - (anchor - current) * (newScale / scale)
        scale = newScale
        // At 1x one finger is for selecting faces, so ignore pan (Wiki Editor UX).
        offset = clampOffset(if (newScale > 1f) zoomed + pan else Offset.Zero, newScale, fit.size, viewport)
    }

    companion object {
        const val MAX_SCALE = 4f

        val Saver: Saver<ZoomState, List<Float>> =
            Saver({ listOf(it.scale, it.offset.x, it.offset.y) }, { ZoomState(it[0], Offset(it[1], it[2])) })
    }
}

/** Where an [image]-sized photo lands when fitted inside [viewport], centered: its 1x rect for [ZoomState]. */
fun fitRect(
    image: Size,
    viewport: Size,
): Rect {
    val scale = minOf(viewport.width / image.width, viewport.height / image.height)
    val size = image * scale
    return Rect(Offset((viewport.width - size.width) / 2, (viewport.height - size.height) / 2), size)
}

/** Keeps the scaled photo covering the viewport on each axis where it is larger, and centered where it is smaller. */
fun clampOffset(
    offset: Offset,
    scale: Float,
    photo: Size,
    viewport: Size,
): Offset {
    val maxX = ((photo.width * scale - viewport.width) / 2).coerceAtLeast(0f)
    val maxY = ((photo.height * scale - viewport.height) / 2).coerceAtLeast(0f)
    // + 0f turns -0f into 0f; Offset compares raw float bits.
    return Offset(offset.x.coerceIn(-maxX, maxX) + 0f, offset.y.coerceIn(-maxY, maxY) + 0f)
}

/** Saved across navigation and configuration changes, so returning from Result keeps the Editor zoom. */
@Composable
fun rememberZoomState(): ZoomState = rememberSaveable(saver = ZoomState.Saver) { ZoomState() }

/**
 * Pinch and drag on this element drive [state]; [fit] gives the photo's 1x rect for the element size. Pass the photo
 * size as [key] when it can change (a turned photo), so [fit] is not the first composition's. With
 * [singleFingerPan] false only two or more fingers zoom and pan, and one-finger drags stay with the content (the basic
 * edit crop box). Put it after the content's own pointerInput so a pinch cancels that drag.
 */
fun Modifier.zoomable(
    state: ZoomState,
    singleFingerPan: Boolean = true,
    key: Any? = null,
    fit: (Size) -> Rect?,
): Modifier =
    pointerInput(state, singleFingerPan, key) {
        if (singleFingerPan) {
            detectTransformGestures { centroid, pan, zoom, _ ->
                val viewport = size.toSize()
                fit(viewport)?.let { state.onGesture(centroid, pan, zoom, it, viewport) }
            }
        } else {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                do {
                    val event = awaitPointerEvent()
                    val viewport = size.toSize()
                    val photo = fit(viewport)
                    if (photo != null && event.changes.count { it.pressed } >= 2) {
                        state.onGesture(event.calculateCentroid(), event.calculatePan(), event.calculateZoom(), photo, viewport)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                } while (event.changes.any { it.pressed })
            }
        }
    }
