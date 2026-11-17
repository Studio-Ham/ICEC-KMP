package com.studioham.icec.face

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.CoreGraphics.CGSizeMake
import platform.Photos.PHAsset
import platform.Photos.PHImageContentModeAspectFit
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.UIKit.UIImage
import swiftPMImport.ICEC.platform.platform.face.detection.MLKFace
import swiftPMImport.ICEC.platform.platform.face.detection.MLKFaceDetector
import swiftPMImport.ICEC.platform.platform.face.detection.MLKFaceDetectorOptions
import swiftPMImport.ICEC.platform.platform.face.detection.MLKVisionImage
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosFaceDetector : FaceDetector {
    private val detector = MLKFaceDetector.faceDetectorWithOptions(MLKFaceDetectorOptions())

    override suspend fun detect(photoId: String): List<FaceBounds> = detect(load(photoId))

    /** ML Kit's synchronous API throws when called on the main thread. */
    internal suspend fun detect(image: UIImage): List<FaceBounds> =
        withContext(Dispatchers.Default) {
            val vision = MLKVisionImage(image).apply { orientation = image.imageOrientation }
            val faces = detector.resultsInImage(vision, null) ?: error("Face detection failed")
            image.size.useContents {
                faces.map {
                    (it as MLKFace).frame.useContents {
                        bounds(
                            origin.x,
                            origin.y,
                            origin.x + size.width,
                            origin.y + size.height,
                            width,
                            height,
                        )
                    }
                }
            }
        }

    /** Photo no larger than [MAX_SIDE], downloading from iCloud when needed. */
    private suspend fun load(photoId: String): UIImage {
        val asset = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(photoId), null).firstObject as? PHAsset ?: error("No photo $photoId")
        val options =
            PHImageRequestOptions().apply {
                deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
                networkAccessAllowed = true
            }
        return suspendCancellableCoroutine { continuation ->
            PHImageManager.defaultManager().requestImageForAsset(
                asset,
                CGSizeMake(MAX_SIDE, MAX_SIDE),
                PHImageContentModeAspectFit,
                options,
            ) { image, _ ->
                continuation.resume(image ?: error("Cannot load photo $photoId"))
            }
        }
    }

    private companion object {
        const val MAX_SIDE = 2048.0
    }
}
