package com.studioham.icec.feature.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.studioham.icec.face.FaceBounds
import kotlin.math.roundToInt

/** Crop shapes of the basic edit (Wiki: 자유, 1:1, 4:3, 16:9; no portrait ratios, D5). [width] 0 means free. */
enum class CropRatio(
    val width: Int,
    val height: Int,
) {
    Free(0, 0),
    Square(1, 1),
    Ratio4x3(4, 3),
    Ratio16x9(16, 9),
}

/** Which part of the crop box a drag moves. */
enum class CropHandle { TopLeft, TopRight, BottomLeft, BottomRight, Move }

/** The whole photo as crop fractions. */
val FullCrop = Rect(0f, 0f, 1f, 1f)

/**
 * A non-destructive basic edit, always applied to the upright original in one order (D6): turn [turns] quarter turns
 * counter-clockwise (D3), mirror left-right when [flipped], then crop to [crop] (fractions of the turned, mirrored photo).
 */
data class BasicEdit(
    val turns: Int = 0,
    val flipped: Boolean = false,
    val crop: Rect = FullCrop,
    val ratio: CropRatio = CropRatio.Free,
) {
    val isIdentity get() = turns == 0 && !flipped && crop == FullCrop

    /**
     * D3: turns what the screen shows a quarter counter-clockwise, mirrored or not. The screen is the mirror after [turns],
     * so on a mirrored photo that is one turn fewer. D6: resets the crop to the largest [ratio] box of the turned photo.
     */
    fun rotated(
        width: Int,
        height: Int,
    ): BasicEdit {
        val next = (turns + if (flipped) 3 else 1) % 4
        return copy(turns = next, crop = ratioCrop(ratio, turnedSize(width, height, next)))
    }

    /** D6: mirroring mirrors the crop box too, so the cropped content stays. */
    fun mirrored() = copy(flipped = !flipped, crop = Rect(1 - crop.right, crop.top, 1 - crop.left, crop.bottom))

    /** Locks [ratio] as the largest centered box; [CropRatio.Free] keeps the current box. */
    fun withRatio(
        ratio: CropRatio,
        width: Int,
        height: Int,
    ) = copy(ratio = ratio, crop = if (ratio == CropRatio.Free) crop else ratioCrop(ratio, turnedSize(width, height, turns)))
}

/** A [width] x [height] photo's size after [turns] quarter turns. */
internal fun turnedSize(
    width: Int,
    height: Int,
    turns: Int,
) = if (turns % 2 == 0) IntSize(width, height) else IntSize(height, width)

/** The largest centered [ratio] box in a [size] photo, as fractions. */
internal fun ratioCrop(
    ratio: CropRatio,
    size: IntSize,
): Rect {
    if (ratio == CropRatio.Free) return FullCrop
    val target = ratio.width.toFloat() / ratio.height
    val photo = size.width.toFloat() / size.height
    return if (photo > target) {
        val w = target / photo
        Rect((1 - w) / 2, 0f, (1 + w) / 2, 1f)
    } else {
        val h = photo / target
        Rect(0f, (1 - h) / 2, 1f, (1 + h) / 2)
    }
}

/**
 * This crop box after dragging [handle] by [drag] (fractions of a photo whose width / height is [aspect]). Corners keep
 * the opposite corner and the locked [ratio]; [CropHandle.Move] keeps the size. The box stays inside the photo and at least
 * [min] (fraction of the photo height) on each side.
 */
internal fun Rect.dragged(
    handle: CropHandle,
    drag: Offset,
    ratio: CropRatio,
    aspect: Float,
    min: Float,
): Rect {
    if (handle == CropHandle.Move) {
        // A box edge on the photo edge may sit 1 ULP outside after Float math; never let the range turn empty.
        val dx = drag.x.coerceIn(minOf(-left, 0f), maxOf(1 - right, 0f))
        val dy = drag.y.coerceIn(minOf(-top, 0f), maxOf(1 - bottom, 0f))
        return translate(dx, dy).inPhoto()
    }
    // Work in photo-shaped units (x times aspect) so a locked ratio keeps its on-screen shape.
    val fromLeft = handle == CropHandle.TopLeft || handle == CropHandle.BottomLeft
    val fromTop = handle == CropHandle.TopLeft || handle == CropHandle.TopRight
    val anchorX = (if (fromLeft) right else left) * aspect
    val anchorY = if (fromTop) bottom else top
    val pointerX = ((if (fromLeft) left else right) + drag.x) * aspect
    val pointerY = (if (fromTop) top else bottom) + drag.y
    val maxW = if (fromLeft) anchorX else aspect - anchorX
    val maxH = if (fromTop) anchorY else 1 - anchorY
    val rawW = if (fromLeft) anchorX - pointerX else pointerX - anchorX
    val rawH = if (fromTop) anchorY - pointerY else pointerY - anchorY
    val w: Float
    val h: Float
    if (ratio == CropRatio.Free) {
        w = rawW.coerceAtMost(maxW).coerceAtLeast(minOf(min, maxW))
        h = rawH.coerceAtMost(maxH).coerceAtLeast(minOf(min, maxH))
    } else {
        val r = ratio.width.toFloat() / ratio.height
        val hMax = minOf(maxH, maxW / r)
        h = minOf(rawH, rawW / r, hMax).coerceAtLeast(minOf(maxOf(min, min / r), hMax))
        w = h * r
    }
    val x = if (fromLeft) anchorX - w else anchorX
    val y = if (fromTop) anchorY - h else anchorY
    return Rect(x / aspect, y, (x + w) / aspect, y + h).inPhoto()
}

/** Float rounding (x times aspect, then divided back) can put an edge 1 ULP past the photo; snap it back. */
private fun Rect.inPhoto() = Rect(left.coerceIn(0f, 1f), top.coerceIn(0f, 1f), right.coerceIn(0f, 1f), bottom.coerceIn(0f, 1f))

/**
 * Moves this canvas so that drawing a [width] x [height] upright image lands turned and mirrored like [edit]. The preview
 * and the save path share it, so both show the same result (like the mosaic, ADR-0004).
 */
internal fun Canvas.orient(
    width: Int,
    height: Int,
    edit: BasicEdit,
) {
    if (edit.flipped) {
        translate(turnedSize(width, height, edit.turns).width.toFloat(), 0f)
        scale(-1f, 1f)
    }
    when (edit.turns) {
        1 -> {
            translate(0f, width.toFloat())
            rotate(-90f)
        }

        2 -> {
            translate(width.toFloat(), height.toFloat())
            rotate(180f)
        }

        3 -> {
            translate(height.toFloat(), 0f)
            rotate(90f)
        }
    }
}

/** The crop in pixels of a [size] photo, with the face boxes' rounding (each edge rounded, at least 1 px). */
internal fun Rect.toPixels(size: IntSize): IntRect = FaceBounds(left, top, right, bottom).toPixels(size.width, size.height)

/** This upright image turned, mirrored, then cropped by [edit], at this image's resolution. */
internal fun ImageBitmap.edited(edit: BasicEdit): ImageBitmap {
    val source = this
    val crop = edit.crop.toPixels(turnedSize(source.width, source.height, edit.turns))
    val result = ImageBitmap(crop.width, crop.height)
    Canvas(result).apply {
        translate(-crop.left.toFloat(), -crop.top.toFloat())
        orient(source.width, source.height, edit)
        drawImage(source, Offset.Zero, Paint().apply { filterQuality = FilterQuality.None })
    }
    return result
}

/** A copy no longer than [maxSide] px on its long side: the editing preview, made from the same pixels as the save. */
internal fun ImageBitmap.scaledToFit(maxSide: Int): ImageBitmap {
    val scale = maxSide.toFloat() / maxOf(width, height)
    if (scale >= 1f) return this
    val size = IntSize((width * scale).roundToInt().coerceAtLeast(1), (height * scale).roundToInt().coerceAtLeast(1))
    val result = ImageBitmap(size.width, size.height)
    Canvas(result).drawImageRect(this, dstSize = size, paint = Paint().apply { filterQuality = FilterQuality.High })
    return result
}
