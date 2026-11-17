package com.studioham.icec.media

import androidx.compose.ui.graphics.ImageBitmap

/** A still image the editor can open. [id] is the platform identifier (MediaStore id, PHAsset localIdentifier). */
data class Photo(
    val id: String,
    val takenAtMillis: Long,
)

interface PhotoLibrary {
    /** Editable still images (JPEG, PNG, HEIC/HEIF), newest first. Throws when the library cannot be read. */
    suspend fun photos(): List<Photo>

    /** True when the original can be read now; false when it was deleted or cannot be opened. */
    suspend fun isReadable(id: String): Boolean

    /** A square-ish thumbnail at least [sizePx] wide, or null when the photo cannot be read. */
    suspend fun thumbnail(
        id: String,
        sizePx: Int,
    ): ImageBitmap?
}
