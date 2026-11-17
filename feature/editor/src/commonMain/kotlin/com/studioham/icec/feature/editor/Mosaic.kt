package com.studioham.icec.feature.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.studioham.icec.face.FaceBounds
import kotlin.math.roundToInt

/** Blocks across a face. Fixed mosaic strength: each block is 1/8 of the face width (user decision, 2026-11-18). */
internal const val MOSAIC_COLUMNS = 8

/** [face] in pixels of a [width] x [height] image, at least 1px. */
internal fun FaceBounds.toPixels(
    width: Int,
    height: Int,
): IntRect {
    val left = (left * width).roundToInt().coerceIn(0, width - 1)
    val top = (top * height).roundToInt().coerceIn(0, height - 1)
    return IntRect(
        left,
        top,
        (right * width).roundToInt().coerceIn(left + 1, width),
        (bottom * height).roundToInt().coerceIn(top + 1, height),
    )
}

/** Mosaic grid for a face box: [MOSAIC_COLUMNS] square-ish blocks across, as many rows as the height needs. */
internal fun mosaicGrid(face: IntRect): IntSize {
    val block = face.width.toFloat() / MOSAIC_COLUMNS
    return IntSize(MOSAIC_COLUMNS, (face.height / block).roundToInt().coerceAtLeast(1))
}

/**
 * The face region shrunk to one pixel per block. Drawn back over the face with [FilterQuality.None], it shows the
 * mosaic. ponytail: mipmapped downscale approximates the block average; #26 decides the saved renderer.
 */
internal fun ImageBitmap.mosaicBlocks(face: IntRect): ImageBitmap {
    val grid = mosaicGrid(face)
    val blocks = ImageBitmap(grid.width, grid.height)
    Canvas(blocks).drawImageRect(
        this,
        IntOffset(face.left, face.top),
        IntSize(face.width, face.height),
        IntOffset.Zero,
        grid,
        Paint().apply { filterQuality = FilterQuality.Medium },
    )
    return blocks
}

/** A square around the face with some margin, for the face list (UX-041). */
internal fun faceCrop(
    face: IntRect,
    width: Int,
    height: Int,
): IntRect {
    val side = (maxOf(face.width, face.height) * 1.4f).roundToInt().coerceAtMost(minOf(width, height))
    val center = face.center
    val left = (center.x - side / 2).coerceIn(0, width - side)
    val top = (center.y - side / 2).coerceIn(0, height - side)
    return IntRect(left, top, left + side, top + side)
}

/** Where an image of [image] size lands when fitted inside [container], centered. */
internal fun fitRect(
    image: IntSize,
    container: Size,
): Rect {
    val scale = minOf(container.width / image.width, container.height / image.height)
    val size = Size(image.width * scale, image.height * scale)
    return Rect(Offset((container.width - size.width) / 2, (container.height - size.height) / 2), size)
}

/** [face] pixels of an image [imageWidth] wide, mapped into this fitted image rect. */
internal fun Rect.map(
    face: IntRect,
    imageWidth: Int,
): Rect {
    val scale = width / imageWidth
    return Rect(left + face.left * scale, top + face.top * scale, left + face.right * scale, top + face.bottom * scale)
}
