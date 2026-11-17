package com.studioham.icec.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSSortDescriptor
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAsset
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetResource
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageContentModeDefault
import platform.Photos.PHImageManager
import platform.Photos.PHImageManagerMaximumSize
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeFast
import platform.Photos.PHPhotoLibrary
import platform.Photos.PHPhotosErrorAccessRestricted
import platform.Photos.PHPhotosErrorAccessUserDenied
import platform.Photos.PHPhotosErrorNotEnoughSpace
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
class IosPhotoLibrary : PhotoLibrary {
    override suspend fun photos(): List<Photo> =
        withContext(Dispatchers.Default) {
            val options = PHFetchOptions().apply { sortDescriptors = listOf(NSSortDescriptor(key = "creationDate", ascending = false)) }
            val result = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options)
            (0 until result.count.toInt()).mapNotNull { index ->
                val asset = result.objectAtIndex(index.toULong()) as PHAsset
                if (asset.isUnsupported()) return@mapNotNull null
                Photo(asset.localIdentifier, ((asset.creationDate?.timeIntervalSince1970 ?: 0.0) * 1000).toLong())
            }
        }

    override suspend fun isReadable(id: String): Boolean {
        val asset = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(id), null).firstObject as? PHAsset ?: return false
        val options = PHImageRequestOptions().apply { networkAccessAllowed = true }
        return suspendCancellableCoroutine { continuation ->
            PHImageManager.defaultManager().requestImageDataAndOrientationForAsset(asset, options) { data, _, _, _ ->
                continuation.resume(data != null)
            }
        }
    }

    override suspend fun thumbnail(
        id: String,
        sizePx: Int,
    ): ImageBitmap? {
        val asset = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(id), null).firstObject as? PHAsset ?: return null
        val options =
            PHImageRequestOptions().apply {
                deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
                resizeMode = PHImageRequestOptionsResizeModeFast
            }
        val image =
            suspendCancellableCoroutine { continuation ->
                PHImageManager.defaultManager().requestImageForAsset(
                    asset,
                    CGSizeMake(sizePx.toDouble(), sizePx.toDouble()),
                    PHImageContentModeAspectFill,
                    options,
                ) { image, _ -> continuation.resume(image) }
            }
        return image?.toImageBitmap()
    }

    override suspend fun original(id: String): ImageBitmap {
        val asset = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(id), null).firstObject as? PHAsset ?: error("No photo $id")
        val options =
            PHImageRequestOptions().apply {
                deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
                networkAccessAllowed = true
            }
        val image =
            suspendCancellableCoroutine<UIImage?> { continuation ->
                PHImageManager.defaultManager().requestImageForAsset(
                    asset,
                    PHImageManagerMaximumSize.readValue(),
                    PHImageContentModeDefault,
                    options,
                ) { image, _ ->
                    continuation.resume(image)
                }
            } ?: error("Cannot load photo $id")
        return withContext(Dispatchers.Default) { image.upright().toImageBitmap(quality = 1.0) ?: error("Cannot decode photo $id") }
    }

    /** UX-014: asks iOS for background time so leaving the app does not stop a running save. */
    override suspend fun save(image: ImageBitmap): String {
        val data =
            withContext(Dispatchers.Default) {
                val bytes =
                    Image.makeFromBitmap(image.asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, JPEG_QUALITY)?.bytes
                        ?: error("JPEG encode failed")
                bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
            }
        val app = UIApplication.sharedApplication
        val task = app.beginBackgroundTaskWithExpirationHandler(null)
        try {
            return suspendCancellableCoroutine { continuation ->
                var created: String? = null
                PHPhotoLibrary.sharedPhotoLibrary().performChanges({
                    val request = PHAssetCreationRequest.creationRequestForAsset()
                    request.addResourceWithType(PHAssetResourceTypePhoto, data, null)
                    created = request.placeholderForCreatedAsset?.localIdentifier
                }) { success, error ->
                    val id = created
                    if (success && id != null) {
                        continuation.resume(id)
                    } else {
                        continuation.resumeWithException(error.toSaveException())
                    }
                }
            }
        } finally {
            app.endBackgroundTask(task)
        }
    }

    private companion object {
        const val JPEG_QUALITY = 95
    }
}

private fun NSError?.toSaveException(): Exception =
    when (this?.code) {
        PHPhotosErrorNotEnoughSpace -> SaveException.NoSpace()
        PHPhotosErrorAccessUserDenied, PHPhotosErrorAccessRestricted -> SaveException.NoPermission()
        else -> IllegalStateException(this?.localizedDescription ?: "Saving failed")
    }

/** Draws the image with its orientation applied, one pixel per point, so pixels match the upright face boxes. */
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.upright(): UIImage {
    val format = UIGraphicsImageRendererFormat.preferredFormat().apply { scale = 1.0 }
    val pixels = size.useContents { CGSizeMake(width * scale, height * scale) }
    return UIGraphicsImageRenderer(pixels, format).imageWithActions { _ ->
        pixels.useContents { drawInRect(CGRectMake(0.0, 0.0, width, height)) }
    }
}

// ponytail: GIF and RAW are found by the first resource's UTI; one lookup per asset, cache it if large libraries list slowly.
private fun PHAsset.isUnsupported(): Boolean {
    val uti = (PHAssetResource.assetResourcesForAsset(this).firstOrNull() as? PHAssetResource)?.uniformTypeIdentifier ?: return false
    return uti == "com.compuserve.gif" || uti.contains("raw", ignoreCase = true)
}

// ponytail: JPEG round trip to reach Skia; draw from the CGImage directly if thumbnails get slow.
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toImageBitmap(quality: Double = 0.9): ImageBitmap? {
    val data = UIImageJPEGRepresentation(this, quality) ?: return null
    val bytes = ByteArray(data.length.toInt())
    bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
