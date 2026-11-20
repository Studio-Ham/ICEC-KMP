package com.studioham.icec.face

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.test.runTest
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextCreateImage
import platform.CoreGraphics.CGColorSpaceCreateWithName
import platform.CoreGraphics.CGColorSpaceRelease
import platform.CoreGraphics.CGContextRelease
import platform.CoreGraphics.CGImageAlphaInfo
import platform.CoreGraphics.CGImageGetAlphaInfo
import platform.CoreGraphics.CGImageGetBitmapInfo
import platform.CoreGraphics.CGImageGetHeight
import platform.CoreGraphics.CGImageGetWidth
import platform.CoreGraphics.CGImageRelease
import platform.CoreGraphics.kCGBitmapByteOrder32Little
import platform.CoreGraphics.kCGBitmapByteOrderDefault
import platform.CoreGraphics.kCGBitmapByteOrderMask
import platform.CoreGraphics.kCGColorSpaceSRGB
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosFaceDetectorTest {
    @Test
    fun findsEveryFaceInTheFixture() =
        runTest {
            val dir = NSProcessInfo.processInfo.environment["ICEC_FIXTURES"] as String
            val image = UIImage.imageWithContentsOfFile("$dir/three-faces.jpg")!!

            val faces = IosFaceDetector().detect(image)

            // Zero faces here usually means the model bundle was not copied next to the binary.
            assertEquals(3, faces.size)
            assertTrue(faces.all { it.left < it.right && it.top < it.bottom && it.right <= 1f && it.bottom <= 1f })
        }

    @OptIn(ExperimentalForeignApi::class)
    @Test
    fun convertsBgrxToRgbxAtTheSamePixelSize() {
        val colorSpace = assertNotNull(CGColorSpaceCreateWithName(kCGColorSpaceSRGB))
        val bitmapInfo = CGImageAlphaInfo.kCGImageAlphaNoneSkipFirst.value or kCGBitmapByteOrder32Little
        val context = CGBitmapContextCreate(null, 12u, 8u, 8u, 0u, colorSpace, bitmapInfo)
        CGColorSpaceRelease(colorSpace)
        assertNotNull(context)

        val source = CGBitmapContextCreateImage(context)
        CGContextRelease(context)
        assertNotNull(source)
        val input =
            try {
                assertEquals(bitmapInfo, CGImageGetBitmapInfo(source))
                UIImage.imageWithCGImage(source)
            } finally {
                CGImageRelease(source)
            }

        val result = input.rgbx()
        val output = assertNotNull(result.CGImage)
        assertEquals(CGImageAlphaInfo.kCGImageAlphaNoneSkipLast, CGImageGetAlphaInfo(output))
        assertEquals(kCGBitmapByteOrderDefault, CGImageGetBitmapInfo(output) and kCGBitmapByteOrderMask)
        assertEquals(12uL, CGImageGetWidth(output))
        assertEquals(8uL, CGImageGetHeight(output))
    }
}
