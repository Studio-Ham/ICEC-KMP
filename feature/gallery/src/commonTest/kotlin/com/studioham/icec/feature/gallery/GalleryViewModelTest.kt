package com.studioham.icec.feature.gallery

import com.studioham.icec.media.PhotoAccess
import com.studioham.icec.media.PhotoPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GalleryViewModelTest {
    private class FakePermission(
        var current: PhotoAccess,
        val afterRequest: PhotoAccess,
    ) : PhotoPermission {
        var requests = 0
        var settingsOpened = 0

        override fun status() = current

        override suspend fun request(): PhotoAccess {
            requests++
            current = afterRequest
            return current
        }

        override fun openSettings() {
            settingsOpened++
        }
    }

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun allowAsksTheSystemWhileItCan() {
        val permission = FakePermission(PhotoAccess.NotDetermined, afterRequest = PhotoAccess.Denied)
        val viewModel = GalleryViewModel(permission)

        viewModel.onAllowClick()

        assertEquals(1, permission.requests)
        assertEquals(PhotoAccess.Denied, viewModel.access.value) // UX-021: same screen stays
    }

    @Test
    fun allowOpensSettingsWhenBlocked() {
        val permission = FakePermission(PhotoAccess.Blocked, afterRequest = PhotoAccess.Blocked)
        val viewModel = GalleryViewModel(permission)

        viewModel.onAllowClick()

        assertEquals(0, permission.requests)
        assertEquals(1, permission.settingsOpened)
    }
}
