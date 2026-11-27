package com.studioham.icec.face

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.provider.MediaStore.Images.Media
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidFaceDetector(
    context: Context,
) : FaceDetector {
    private val resolver = context.applicationContext.contentResolver
    private val detector =
        FaceDetection.getClient(
            FaceDetectorOptions
                .Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setMinFaceSize(MIN_FACE_SIZE)
                .build(),
        )

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
        // API 24-27: BitmapFactory ignores EXIF, so turn the pixels upright to match PhotoLibrary.original().
        val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, size) }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(maxOf(size.outWidth, size.outHeight)) }
        val bitmap =
            resolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                ?: error("Cannot decode photo $photoId")
        val orientation =
            resolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
        return bitmap.upright(orientation)
    }

    /**
     * This bitmap turned upright for an EXIF [orientation] (1-8). ponytail: copy of AndroidPhotoLibrary's matrix because
     * platform modules do not depend on each other; both must stay identical.
     */
    private fun Bitmap.upright(orientation: Int): Bitmap {
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

    private companion object {
        const val MAX_SIDE = 2048

        fun sampleSize(side: Int): Int {
            var sample = 1
            while (side / (sample * 2) >= MAX_SIDE) sample *= 2
            return sample
        }
    }
}
