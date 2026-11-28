package com.studioham.icec.feature.gallery

import androidx.compose.ui.graphics.ImageBitmap
import com.studioham.icec.media.Photo
import com.studioham.icec.media.PhotoAccess
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.media.PhotoPage
import com.studioham.icec.media.PhotoPermission
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
        var readable = true
        val loads = mutableListOf<Pair<Int, Int>>()
        var beforeLoad: suspend (Int) -> Unit = {}

        override suspend fun photos(
            offset: Int,
            limit: Int,
        ): PhotoPage {
            loads += offset to limit
            beforeLoad(offset)
            val photos = result()
            val end = minOf(offset + limit, photos.size)
            return PhotoPage(photos.drop(offset).take(limit), end.takeIf { it < photos.size }, photos.size)
        }

        override suspend fun isReadable(id: String) = readable

        override suspend fun thumbnail(
            id: String,
            sizePx: Int,
        ) = null

        override suspend fun preview(
            id: String,
            maxSidePx: Int,
        ) = null

        override suspend fun original(id: String): ImageBitmap = error("not used")

        override suspend fun save(image: ImageBitmap): String = error("not used")
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
        viewModel.refresh()
        assertEquals(PhotosState.LoadFailed, viewModel.photos.value)

        library.result = { listOf(Photo("1", 0)) }
        viewModel.onRetryClick()

        assertEquals(1, (viewModel.photos.value as PhotosState.Loaded).total)
    }

    @Test
    fun readablePhotoOpens() =
        runTest {
            val photo = Photo("1", 0)
            val viewModel = GalleryViewModel(FakePermission(PhotoAccess.Granted, PhotoAccess.Granted), FakeLibrary { listOf(photo) })

            viewModel.onPhotoClick(photo)

            assertEquals(GalleryEffect.OpenPhoto(photo), viewModel.effects.first())
        }

    @Test
    fun unreadablePhotoWarnsAndReloads() =
        runTest {
            val library = FakeLibrary { emptyList() }.apply { readable = false }
            val viewModel = GalleryViewModel(FakePermission(PhotoAccess.Granted, PhotoAccess.Granted), library)

            viewModel.onPhotoClick(Photo("gone", 0))

            assertEquals(GalleryEffect.PhotoUnavailable, viewModel.effects.first())
            assertEquals(listOf(0 to 60), library.loads) // UX-030: the deleted photo drops out of the list
        }

    @Test
    fun firstPageLoadsWithoutReadingMore() {
        val library = pagedLibrary()
        val viewModel = gallery(library)

        viewModel.refresh()

        assertEquals(listOf(0 to 60), library.loads)
        assertEquals(60, viewModel.loadedPhotos().size)
        assertEquals(125, (viewModel.photos.value as PhotosState.Loaded).total)
    }

    @Test
    fun loadMoreAppendsTheNextPage() {
        val library = pagedLibrary()
        val viewModel = gallery(library)
        viewModel.refresh()

        viewModel.onLoadMore()

        assertEquals(listOf(0 to 60, 60 to 60), library.loads)
        assertEquals(library.result().take(120), viewModel.loadedPhotos())
    }

    @Test
    fun loadMoreStopsAtTheEnd() {
        val library = pagedLibrary(61)
        val viewModel = gallery(library)
        viewModel.refresh()

        viewModel.onLoadMore()
        viewModel.onLoadMore()

        assertEquals(listOf(0 to 60, 60 to 60), library.loads)
        assertEquals(library.result(), viewModel.loadedPhotos())
    }

    @Test
    fun concurrentLoadMoreReadsThePageOnlyOnce() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val library = pagedLibrary().apply { beforeLoad = { offset -> if (offset == 60) gate.await() } }
            val viewModel = gallery(library)
            viewModel.refresh()

            viewModel.onLoadMore()
            viewModel.onLoadMore()

            assertEquals(listOf(0 to 60, 60 to 60), library.loads)
            gate.complete(Unit)
            assertEquals(library.result().take(120), viewModel.loadedPhotos())
        }

    @Test
    fun refreshRereadsTheLoadedRange() {
        val library = pagedLibrary()
        val viewModel = gallery(library)
        viewModel.refresh()
        viewModel.onLoadMore()
        val updated = listOf(Photo("new", 126)) + library.result()
        library.result = { updated }

        viewModel.refresh()

        assertEquals(listOf(0 to 60, 60 to 60, 0 to 120), library.loads)
        assertEquals(updated.take(120), viewModel.loadedPhotos())
    }

    @Test
    fun loadMoreFailureKeepsTheListAndCanBeRetried() {
        val library = pagedLibrary().apply { beforeLoad = { offset -> if (offset == 60) error("MediaStore down") } }
        val viewModel = gallery(library)
        viewModel.refresh()
        val loaded = viewModel.photos.value

        viewModel.onLoadMore()
        assertEquals(loaded, viewModel.photos.value)
        library.beforeLoad = {}
        viewModel.onLoadMore()

        assertEquals(listOf(0 to 60, 60 to 60, 60 to 60), library.loads)
        assertEquals(library.result().take(120), viewModel.loadedPhotos())
    }

    private fun pagedLibrary(count: Int = 125) = FakeLibrary { (count downTo 1).map { Photo(it.toString(), it.toLong()) } }

    private fun gallery(library: FakeLibrary) = GalleryViewModel(FakePermission(PhotoAccess.Granted, PhotoAccess.Granted), library)

    private fun GalleryViewModel.loadedPhotos() = (photos.value as PhotosState.Loaded).sections.flatMap { it.photos }

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
