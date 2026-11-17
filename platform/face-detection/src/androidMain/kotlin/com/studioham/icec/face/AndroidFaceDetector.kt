package com.studioham.icec.face

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore.Images.Media
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidFaceDetector(
    context: Context,
) : FaceDetector {
    private val resolver = context.applicationContext.contentResolver
    private val detector = FaceDetection.getClient()

    override suspend fun detect(photoId: String): List<FaceBounds> {
        val bitmap = withContext(Dispatchers.IO) { decode(photoId) }
        val faces =
            suspendCancellableCoroutine { continuation ->
                detector
                    .process(InputImage.fromBitmap(bitmap, 0))
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        return faces.map {
            val box = it.boundingBox
            bounds(box.left, box.top, box.right, box.bottom, bitmap.width, bitmap.height)
        }
    }

    /** Upright bitmap no larger than [MAX_SIDE]; the PRD allows downsampling for detection. */
    private fun decode(photoId: String): Bitmap {
        val uri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, photoId.toLong())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(resolver, uri)) { decoder, info, _ ->
                val scale = MAX_SIDE.toFloat() / maxOf(info.size.width, info.size.height)
                if (scale < 1) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        // ponytail: API 24-27 ignore EXIF rotation, so boxes on rotated JPEGs land sideways. Rotate with ExifInterface
        // if those devices matter.
        val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, size) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(maxOf(size.outWidth, size.outHeight)) }
        return resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Cannot decode photo $photoId")
    }

    private companion object {
        const val MAX_SIDE = 2048

        fun sampleSize(side: Int): Int {
            var sample = 1
            while (side / (sample * 2) >= MAX_SIDE) sample *= 2
            return sample
        }
    }
}
