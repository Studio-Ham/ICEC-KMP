package com.studioham.icec.media

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSSortDescriptor
import platform.Foundation.timeIntervalSince1970
import platform.Photos.PHAsset
import platform.Photos.PHAssetMediaTypeImage
import platform.Photos.PHAssetResource
import platform.Photos.PHFetchOptions
import platform.Photos.PHImageContentModeAspectFill
import platform.Photos.PHImageManager
import platform.Photos.PHImageRequestOptions
import platform.Photos.PHImageRequestOptionsDeliveryModeHighQualityFormat
import platform.Photos.PHImageRequestOptionsResizeModeFast
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.coroutines.resume

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
}

// ponytail: GIF and RAW are found by the first resource's UTI; one lookup per asset, cache it if large libraries list slowly.
private fun PHAsset.isUnsupported(): Boolean {
    val uti = (PHAssetResource.assetResourcesForAsset(this).firstOrNull() as? PHAssetResource)?.uniformTypeIdentifier ?: return false
    return uti == "com.compuserve.gif" || uti.contains("raw", ignoreCase = true)
}

// ponytail: JPEG round trip to reach Skia; draw from the CGImage directly if thumbnails get slow.
@OptIn(ExperimentalForeignApi::class)
private fun UIImage.toImageBitmap(): ImageBitmap? {
    val data = UIImageJPEGRepresentation(this, 0.9) ?: return null
    val bytes = ByteArray(data.length.toInt())
    bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
