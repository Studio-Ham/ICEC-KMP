package com.studioham.icec.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSNumber
import platform.Foundation.NSSortDescriptor
import platform.Foundation.create
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAsset
import platform.Photos.PHAssetCreationRequest
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetPlaybackStyleImageAnimated
import platform.Photos.PHAssetResource
import platform.Photos.PHAssetResourceTypePhoto
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageContentMode
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageContentModeAspectFit
import platform.Photos.PHImageContentModeDefault
import platform.Photos.PHImageManager
import platform.Photos.PHImageManagerMaximumSize
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeExact
import platform.Photos.PHImageRequestOptionsResizeModeFast
import platform.Photos.PHImageResultIsDegradedKey
import platform.Photos.PHPhotoLibrary
import platform.Photos.PHPhotosErrorAccessRestricted
import platform.Photos.PHPhotosErrorAccessUserDenied
import platform.Photos.PHPhotosErrorNotEnoughSpace
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImageOrientation
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalForeignApi::class)
class IosPhotoLibrary : PhotoLibrary {
    override suspend fun photos(
        offset: Int,
        limit: Int,
    ): PhotoPage =
        withContext(Dispatchers.Default) {
            val options = PHFetchOptions().apply { sortDescriptors = listOf(NSSortDescriptor(key = "creationDate", ascending = false)) }
            val result = PHAsset.fetchAssetsWithMediaType(PHAssetMediaTypeImage, options)
            val total = result.count.toInt()
            val end = offset + minOf(limit, (total - offset).coerceAtLeast(0))
            val photos =
                (offset until end).mapNotNull { index ->
                    ensureActive()
                    val asset = result.objectAtIndex(index.toULong()) as PHAsset
                    if (asset.isUnsupported()) return@mapNotNull null
                    Photo(asset.localIdentifier, ((asset.creationDate?.timeIntervalSince1970 ?: 0.0) * 1000).toLong())
                }
            // ponytail: total includes hidden GIF and RAW assets. Count eligible assets separately if an exact total is needed.
            PhotoPage(photos, end.takeIf { it < total }, total)
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
        // Local only, as before PR #64: scrolling the Gallery must not download iCloud photos. A tile crops to a square anyway.
        val options =
            PHImageRequestOptions().apply {
                deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
                resizeMode = PHImageRequestOptionsResizeModeFast
            }
        val image = requestImage(id, CGSizeMake(sizePx.toDouble(), sizePx.toDouble()), PHImageContentModeAspectFill, options) ?: return null
        return withContext(Dispatchers.Default) { image.uprightIfNeeded().toImageBitmap() }
    }

    override suspend fun preview(
        id: String,
        maxSidePx: Int,
    ): ImageBitmap? {
        // AspectFit never crops; Exact makes the long side the asked size, so it never exceeds maxSidePx. The chosen photo may
        // be iCloud-only, like isReadable() and original().
        val options =
            PHImageRequestOptions().apply {
                deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
                resizeMode = PHImageRequestOptionsResizeModeExact
                networkAccessAllowed = true
            }
        val size = CGSizeMake(maxSidePx.toDouble(), maxSidePx.toDouble())
        val image = requestImage(id, size, PHImageContentModeAspectFit, options) ?: return null
        return withContext(Dispatchers.Default) { image.uprightIfNeeded().toImageBitmap() }
    }

    override suspend fun original(id: String): ImageBitmap {
        val options =
            PHImageRequestOptions().apply {
                deliveryMode = PHImageRequestOptionsDeliveryModeHighQualityFormat
                networkAccessAllowed = true
            }
        val image =
            requestImage(id, PHImageManagerMaximumSize.readValue(), PHImageContentModeDefault, options) ?: error("Cannot load photo $id")
        return withContext(Dispatchers.Default) { image.upright().toImageBitmap(quality = 1.0) ?: error("Cannot decode photo $id") }
    }

    /**
     * The final image of an asynchronous PhotoKit request, or null when the photo is missing or fails. PhotoKit may call
     * back more than once: degraded images come before the final one, so they are skipped, and only the first final
     * result resumes. Cancelling the coroutine cancels the request.
     */
    private suspend fun requestImage(
        id: String,
        size: CValue<CGSize>,
        mode: PHImageContentMode,
        options: PHImageRequestOptions,
    ): UIImage? {
        val asset = PHAsset.fetchAssetsWithLocalIdentifiers(listOf(id), null).firstObject as? PHAsset ?: return null
        val manager = PHImageManager.defaultManager()
        return suspendCancellableCoroutine { continuation ->
            val request =
                manager.requestImageForAsset(asset, size, mode, options) { image, info ->
                    // Callbacks arrive on the main thread one at a time, so this check and resume do not race each other.
                    val degraded = (info?.get(PHImageResultIsDegradedKey) as? NSNumber)?.boolValue == true
                    if (!degraded && continuation.isActive) continuation.resume(image)
                }
            continuation.invokeOnCancellation { manager.cancelImageRequest(request) }
        }
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

/** This image, or a copy drawn upright when PhotoKit hands it over with an orientation. */
private fun UIImage.uprightIfNeeded(): UIImage = if (imageOrientation == UIImageOrientation.UIImageOrientationUp) this else upright()

/** Draws the image with its orientation applied, one pixel per point, so pixels match the upright face boxes. */
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.upright(): UIImage {
    val format = UIGraphicsImageRendererFormat.preferredFormat().apply { scale = 1.0 }
    val pixels = size.useContents { CGSizeMake(width * scale, height * scale) }
    return UIGraphicsImageRenderer(pixels, format).imageWithActions { _ ->
        pixels.useContents { drawInRect(CGRectMake(0.0, 0.0, width, height)) }
    }
}

private fun PHAsset.isUnsupported(): Boolean {
    if (playbackStyle == PHAssetPlaybackStyleImageAnimated) return true
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
