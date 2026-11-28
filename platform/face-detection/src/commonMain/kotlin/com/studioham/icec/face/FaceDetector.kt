package com.studioham.icec.face

/** A face box as fractions (0..1) of the upright photo, so callers can map it onto any rendered size. */
data class FaceBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)

/** Minimum detectable face width is 3% of the input image width. */
internal const val MIN_FACE_SIZE = 0.03f

fun interface FaceDetector {
    /**
     * Faces in the photo with platform id [photoId] (MediaStore id, PHAsset localIdentifier). An empty list means
     * the analysis worked and found no face. Throws when the photo cannot be read or the detector fails.
     */
    suspend fun detect(photoId: String): List<FaceBounds>
}
