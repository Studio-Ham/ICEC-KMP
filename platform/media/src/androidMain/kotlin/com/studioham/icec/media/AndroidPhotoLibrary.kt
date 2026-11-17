package com.studioham.icec.media

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.Images.Thumbnails
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

    override suspend fun photos(): List<Photo> =
        withContext(Dispatchers.IO) {
            val projection = arrayOf(Media._ID, Media.DATE_TAKEN, Media.DATE_ADDED)
            val selection = "${Media.MIME_TYPE} IN (?, ?, ?, ?)"
            val types = arrayOf("image/jpeg", "image/png", "image/heic", "image/heif")
            val cursor = resolver.query(Media.EXTERNAL_CONTENT_URI, projection, selection, types, null) ?: error("MediaStore query failed")
            cursor
                .use {
                    buildList {
                        while (it.moveToNext()) {
                            val taken = it.getLong(1).takeIf { millis -> millis > 0 } ?: (it.getLong(2) * 1000)
                            add(Photo(it.getLong(0).toString(), taken))
                        }
                    }
                }.sortedByDescending { it.takenAtMillis }
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
                if (Build.VERSION.SDK_INT >= 29) {
                    resolver.loadThumbnail(ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id.toLong()), Size(sizePx, sizePx), null)
                } else {
                    @Suppress("DEPRECATION")
                    Thumbnails.getThumbnail(resolver, id.toLong(), Thumbnails.MINI_KIND, null)
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
                // ponytail: API 24-27 ignore EXIF rotation, like face detection on those versions.
                resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it) } ?: error("Cannot decode photo $id")
            }.asImageBitmap()
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
                resolver.delete(uri, null, null)
                throw if (e is IOException && e.message.orEmpty().contains("ENOSPC")) SaveException.NoSpace(e) else e
            }
        }

    private companion object {
        const val JPEG_QUALITY = 95
    }
}
