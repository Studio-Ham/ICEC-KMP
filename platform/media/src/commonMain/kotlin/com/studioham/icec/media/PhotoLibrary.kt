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

    /** The photo upright at its original pixel size. Throws when it cannot be read. */
    suspend fun original(id: String): ImageBitmap

    /**
     * Saves [image] as a new JPEG photo without metadata and returns its id. The original stays unchanged.
     * Throws [SaveException] for a full disk or a missing permission, other exceptions for any other failure.
     */
    suspend fun save(image: ImageBitmap): String
}

sealed class SaveException(
    cause: Throwable?,
) : Exception(cause) {
    class NoSpace(
        cause: Throwable? = null,
    ) : SaveException(cause)

    class NoPermission(
        cause: Throwable? = null,
    ) : SaveException(cause)
}
