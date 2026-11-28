package com.studioham.icec.face

/** Converts a pixel box in a [width] x [height] image to fractions, clamped to the image. */
internal fun bounds(
    left: Number,
    top: Number,
    right: Number,
    bottom: Number,
    width: Number,
    height: Number,
): FaceBounds {
    val w = width.toFloat()
    val h = height.toFloat()
    return FaceBounds(
        (left.toFloat() / w).coerceIn(0f, 1f),
        (top.toFloat() / h).coerceIn(0f, 1f),
        (right.toFloat() / w).coerceIn(0f, 1f),
        (bottom.toFloat() / h).coerceIn(0f, 1f),
    )
}
