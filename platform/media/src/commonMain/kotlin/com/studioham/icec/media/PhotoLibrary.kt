package com.studioham.icec.media

import androidx.compose.ui.graphics.ImageBitmap

/** A still image the editor can open. [id] is the platform identifier (MediaStore id, PHAsset localIdentifier). */
data class Photo(
    val id: String,
    val takenAtMillis: Long,
)

/** [photos] contains editable images in newest-first order. [next] is the next source offset, or null at the end. */
data class PhotoPage(
    val photos: List<Photo>,
    val next: Int?,
    val total: Int,
)

interface PhotoLibrary {
    /**
     * Reads up to [limit] source entries from [offset]. Unsupported entries can make the page shorter.
     * The page total is the source list size. Throws when the library cannot be read.
     */
    suspend fun photos(
        offset: Int,
        limit: Int,
    ): PhotoPage

    /** True when the original can be read now; false when it was deleted or cannot be opened. */
    suspend fun isReadable(id: String): Boolean

    /**
     * A Gallery tile: upright, short side about [sizePx] or more (the tile may crop it), from local data only on iOS.
     * Null when the photo cannot be read.
     */
    suspend fun thumbnail(
        id: String,
        sizePx: Int,
    ): ImageBitmap?

    /**
     * The whole photo for a screen that shows the chosen photo: upright, original aspect ratio, long side at most
     * [maxSidePx]. iOS may download it from iCloud. Null when the photo cannot be read.
     */
    suspend fun preview(
        id: String,
        maxSidePx: Int,
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
