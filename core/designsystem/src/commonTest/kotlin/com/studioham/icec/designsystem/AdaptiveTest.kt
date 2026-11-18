package com.studioham.icec.designsystem

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveTest {
    @Test
    fun twoPaneOnlyForWideOrShortWindows() {
        assertFalse(isTwoPane(390.dp, 844.dp)) // portrait phone
        assertFalse(isTwoPane(599.dp, 480.dp))
        assertTrue(isTwoPane(844.dp, 390.dp)) // landscape phone
        assertTrue(isTwoPane(600.dp, 960.dp)) // portrait tablet / unfolded foldable
        assertTrue(isTwoPane(500.dp, 479.dp))
    }
}
