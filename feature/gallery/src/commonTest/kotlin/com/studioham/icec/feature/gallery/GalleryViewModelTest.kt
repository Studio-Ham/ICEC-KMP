package com.studioham.icec.feature.gallery

import com.studioham.icec.media.Photo
import com.studioham.icec.media.PhotoAccess
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.media.PhotoPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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

    private class FakeLibrary(
        var result: () -> List<Photo>,
    ) : PhotoLibrary {
        override suspend fun photos() = result()

        override suspend fun thumbnail(
            id: String,
            sizePx: Int,
        ) = null
    }

    private val noPhotos = FakeLibrary { emptyList() }

    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest fun tearDown() = Dispatchers.resetMain()

    @Test
    fun allowAsksTheSystemWhileItCan() {
        val permission = FakePermission(PhotoAccess.NotDetermined, afterRequest = PhotoAccess.Denied)
        val viewModel = GalleryViewModel(permission, noPhotos)

        viewModel.onAllowClick()

        assertEquals(1, permission.requests)
        assertEquals(PhotoAccess.Denied, viewModel.access.value) // UX-021: same screen stays
    }

    @Test
    fun allowOpensSettingsWhenBlocked() {
        val permission = FakePermission(PhotoAccess.Blocked, afterRequest = PhotoAccess.Blocked)
        val viewModel = GalleryViewModel(permission, noPhotos)

        viewModel.onAllowClick()

        assertEquals(0, permission.requests)
        assertEquals(1, permission.settingsOpened)
    }

    @Test
    fun loadFailureCanBeRetried() {
        val library = FakeLibrary { error("MediaStore down") }
        val viewModel = GalleryViewModel(FakePermission(PhotoAccess.Granted, PhotoAccess.Granted), library)
        assertEquals(PhotosState.LoadFailed, viewModel.photos.value)

        library.result = { listOf(Photo("1", 0)) }
        viewModel.onRetryClick()

        assertEquals(1, (viewModel.photos.value as PhotosState.Loaded).count)
    }

    @Test
    fun photosAreGroupedByDayNewestFirst() {
        val day = 86_400_000L
        val sections = listOf(Photo("old", 0), Photo("new", day + 10), Photo("newer", day + 20)).groupByDay(TimeZone.UTC)

        assertEquals(listOf(LocalDate(1970, 1, 2), LocalDate(1970, 1, 1)), sections.map { it.date })
        assertEquals(listOf("newer", "new"), sections[0].photos.map { it.id })
    }

    @Test
    fun countsUseThousandsSeparators() {
        assertEquals(listOf("0", "999", "1,240", "1,000,000"), listOf(0, 999, 1240, 1_000_000).map { it.withThousands() })
    }
}
