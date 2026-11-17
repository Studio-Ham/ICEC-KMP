package com.studioham.icec.media

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore.Images.Media
import android.provider.MediaStore.Images.Thumbnails
import android.util.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
}
