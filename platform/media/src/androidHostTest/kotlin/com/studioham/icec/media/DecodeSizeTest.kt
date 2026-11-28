package com.studioham.icec.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PR #64 re-review 3.1: the preview's long side never passes its cap, and a Gallery tile never decodes a whole panorama. */
class DecodeSizeTest {
    private fun ceilDiv(
        side: Int,
        sample: Int,
    ) = (side + sample - 1) / sample

    @Test
    fun api24to27PreviewSamplesTheLongSideUnderTheCap() {
        // width x height -> expected sample for a 2048px preview
        val cases =
            mapOf(
                (4032 to 3024) to 2,
                (8000 to 6000) to 4,
                (16000 to 3000) to 8,
                (10000 to 2000) to 8,
                (4097 to 100) to 4,
                (1000 to 750) to 1,
            )
        for ((size, expected) in cases) {
            val (width, height) = size
            val sample = sampleSize(width, height, 2048, 2048)
            assertEquals(expected, sample, "$width x $height")
            assertTrue(ceilDiv(maxOf(width, height), sample) <= 2048, "$width x $height")
        }
    }

    @Test
    fun api24to27TileKeepsTheShortSideButCapsAPanorama() {
        assertEquals(4, sampleSize(4032, 3024, 390, 390 * 4)) // 1008 x 756: short side still at least 390
        assertEquals(8, sampleSize(10000, 700, 390, 390 * 4)) // 1250 x 88 instead of 10000 x 700 (26.7 MiB)
    }

    @Test
    fun api28AndLaterScaleTheLongSideToTheCapExactly() {
        assertTrue((4032 * targetScale(4032, 3024, 2048, 2048)).toInt() in 2047..2048)
        assertTrue((16000 * targetScale(16000, 3000, 2048, 2048)).toInt() in 2047..2048)
        assertEquals(1f, targetScale(1000, 750, 2048, 2048)) // never scales up
        assertEquals(1f, targetScale(4032, 3024, Int.MAX_VALUE, Int.MAX_VALUE)) // original()
        assertTrue((3024 * targetScale(4032, 3024, 390, 1560)).toInt() in 389..390) // tile: short side 390
        assertTrue((10000 * targetScale(10000, 700, 390, 1560)).toInt() in 1559..1560) // panorama tile: long side capped
    }
}
