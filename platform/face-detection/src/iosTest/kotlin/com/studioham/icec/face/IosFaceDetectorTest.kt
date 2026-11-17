package com.studioham.icec.face

import kotlinx.coroutines.test.runTest
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIImage
import kotlin.test.Test
import kotlin.test.assertEquals
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
}
