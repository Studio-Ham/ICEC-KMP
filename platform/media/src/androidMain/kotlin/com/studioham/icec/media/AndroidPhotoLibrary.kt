package com.studioham.icec.media

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore.Images.Media
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class AndroidPhotoLibrary(
    context: Context,
) : PhotoLibrary {
    private val resolver = context.contentResolver

    override suspend fun photos(
        offset: Int,
        limit: Int,
    ): PhotoPage =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(Media._ID, Media.DATE_TAKEN, Media.DATE_ADDED)
            val selection = "${Media.MIME_TYPE} IN (?, ?, ?, ?)"
            val types = arrayOf("image/jpeg", "image/png", "image/heic", "image/heif")
            val order = "${Media.DATE_TAKEN} DESC, ${Media.DATE_ADDED} DESC, ${Media._ID} DESC"
            val cursor = resolver.query(Media.EXTERNAL_CONTENT_URI, projection, selection, types, order) ?: error("MediaStore query failed")
            cursor.use {
                val total = it.count
                val end = offset + minOf(limit, (total - offset).coerceAtLeast(0))
                val photos =
                    buildList {
                        if (offset < end && it.moveToPosition(offset)) {
                            do {
                                val taken = it.getLong(1).takeIf { millis -> millis > 0 } ?: (it.getLong(2) * 1000)
                                add(Photo(it.getLong(0).toString(), taken))
                            } while (it.position + 1 < end && it.moveToNext())
                        }
                    }
                PhotoPage(photos, end.takeIf { it < total }, total)
            }
        }

    override suspend fun isReadable(id: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                resolver.openInputStream(ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id.toLong()))?.use { it.read() >= 0 }
            }.getOrNull() == true
        }

    override suspend fun thumbnail(
        id: String,
        sizePx: Int,
    ): ImageBitmap? =
        withContext(Dispatchers.IO) {
            runCatching {
                val uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id.toLong())
                if (Build.VERSION.SDK_INT >= 29) {
                    resolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
                } else {
                    // API 24-28: MINI_KIND ignores sizePx (512 x 384) and may not apply EXIF; decode sampled down and upright.
                    decodeUpright(uri, sizePx)
                }
            }.getOrNull()?.asImageBitmap()
        }

    override suspend fun original(id: String): ImageBitmap =
        withContext(Dispatchers.IO) {
            val uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id.toLong())
            // ponytail: full-resolution ARGB in memory (about 48 MB for 12 MP); tile the render if large photos run out of memory.
            if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                decodeUpright(uri)
            }.asImageBitmap()
        }

    /**
     * API 24-27 originals and API 24-28 thumbnails: BitmapFactory ignores EXIF, so turn the pixels upright like ImageDecoder
     * does (and face detection). [minSide] above 0 samples down by powers of two while the short side stays at least that.
     * ponytail: a 2048px thumbnail of a 12 MP photo still decodes at full size; use ImageDecoder.setTargetSize on API 28 if
     * old devices run out of memory.
     */
    private fun decodeUpright(
        uri: Uri,
        minSide: Int = 0,
    ): Bitmap {
        var sample = 1
        if (minSide > 0) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            val short = minOf(bounds.outWidth, bounds.outHeight)
            while (short / (sample * 2) >= minSide) sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) } ?: error("Cannot decode $uri")
        // A broken or unsupported EXIF block must not lose a photo that decoded; a missing permission already failed above.
        val orientation = runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it).orientation() } }.getOrNull()
        return bitmap.upright(orientation ?: ExifInterface.ORIENTATION_NORMAL)
    }

    override suspend fun save(image: ImageBitmap): String =
        withContext(Dispatchers.IO) {
            val values =
                ContentValues().apply {
                    put(Media.DISPLAY_NAME, "ICEC_${System.currentTimeMillis()}.jpg")
                    put(Media.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= 29) {
                        put(Media.RELATIVE_PATH, "Pictures/ICEC")
                        put(Media.IS_PENDING, 1)
                    }
                }
            val uri =
                try {
                    resolver.insert(Media.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore insert failed")
                } catch (e: SecurityException) {
                    throw SaveException.NoPermission(e)
                }
            try {
                resolver.openOutputStream(uri).use { out ->
                    check(
                        out != null && image.asAndroidBitmap().compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out),
                    ) { "JPEG encode failed" }
                }
                if (Build.VERSION.SDK_INT >= 29) resolver.update(uri, ContentValues().apply { put(Media.IS_PENDING, 0) }, null, null)
                ContentUris.parseId(uri).toString()
            } catch (e: Exception) {
                // Clean up the pending entry without hiding the real cause.
                runCatching { resolver.delete(uri, null, null) }
                throw when {
                    e is IOException && e.message.orEmpty().contains("ENOSPC") -> SaveException.NoSpace(e)
                    e is SecurityException -> SaveException.NoPermission(e)
                    else -> e
                }
            }
        }

    private companion object {
        const val JPEG_QUALITY = 95
    }
}

private fun ExifInterface.orientation() = getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

/**
 * This bitmap turned upright for an EXIF [orientation] (1-8). ponytail: briefly holds two full bitmaps on API 24-27;
 * decode with inSampleSize first if old devices run out of memory. Same matrix as AndroidFaceDetector.
 */
internal fun Bitmap.upright(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> matrix.apply { setRotate(90f) }.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> matrix.apply { setRotate(-90f) }.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return this
    }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true).also { if (it !== this) recycle() }
}
