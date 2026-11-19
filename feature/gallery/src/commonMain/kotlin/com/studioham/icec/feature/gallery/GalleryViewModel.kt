package com.studioham.icec.feature.gallery

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studioham.icec.media.Photo
import com.studioham.icec.media.PhotoAccess
import com.studioham.icec.media.PhotoLibrary
import com.studioham.icec.media.PhotoPermission
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class PhotoSection(
    val date: LocalDate,
    val photos: List<Photo>,
)

sealed interface PhotosState {
    data object Loading : PhotosState

    data class Loaded(
        val sections: List<PhotoSection>,
        val total: Int,
        val next: Int? = null,
        val appendFailed: Boolean = false,
    ) : PhotosState

    data object LoadFailed : PhotosState
}

sealed interface GalleryEffect {
    data class OpenPhoto(
        val photo: Photo,
    ) : GalleryEffect

    /** UX-030: the tapped photo could not be read; the list stays. */
    data object PhotoUnavailable : GalleryEffect
}

@Inject
class GalleryViewModel(
    private val permission: PhotoPermission,
    private val library: PhotoLibrary,
) : ViewModel() {
    private val _access = MutableStateFlow(permission.status())
    val access: StateFlow<PhotoAccess> = _access.asStateFlow()

    private val _photos = MutableStateFlow<PhotosState>(PhotosState.Loading)
    val photos: StateFlow<PhotosState> = _photos.asStateFlow()

    private val _effects = Channel<GalleryEffect>(Channel.BUFFERED)
    val effects: Flow<GalleryEffect> = _effects.receiveAsFlow()

    private var loadJob: Job? = null

    /** UX-031: on every resume re-read the permission and the list; the grid keeps its anchor by photo id. */
    fun refresh() {
        _access.value = permission.status()
        loadIfAllowed()
    }

    /** UX-030: open only a readable photo; otherwise stay, tell the user and drop deleted photos from the list. */
    fun onPhotoClick(photo: Photo) {
        viewModelScope.launch {
            if (library.isReadable(photo.id)) {
                _effects.send(GalleryEffect.OpenPhoto(photo))
            } else {
                _effects.send(GalleryEffect.PhotoUnavailable)
                refresh()
            }
        }
    }

    /** UX-020~022: ask while the system can ask, otherwise send the user to the OS settings. */
    fun onAllowClick() {
        if (_access.value == PhotoAccess.Blocked) {
            permission.openSettings()
        } else {
            viewModelScope.launch {
                _access.value = permission.request()
                loadIfAllowed()
            }
        }
    }

    /** UX-029: LoadFailed retry. */
    fun onRetryClick() = loadIfAllowed()

    suspend fun thumbnail(
        photo: Photo,
        sizePx: Int,
    ): ImageBitmap? = library.thumbnail(photo.id, sizePx)

    fun onLoadMore() {
        val loaded = _photos.value as? PhotosState.Loaded ?: return
        val next = loaded.next ?: return
        if (!hasAccess() || loadJob?.isActive == true) return
        load(next, PAGE_SIZE, loaded)
    }

    private fun loadIfAllowed() {
        loadJob?.cancel()
        if (!hasAccess()) return
        val loaded = _photos.value as? PhotosState.Loaded
        val loadedRange = loaded?.let { it.next ?: it.total } ?: 0
        if (loaded == null) _photos.value = PhotosState.Loading
        load(0, maxOf(PAGE_SIZE, loadedRange))
    }

    private fun hasAccess(): Boolean = _access.value == PhotoAccess.Granted || _access.value == PhotoAccess.Limited

    private fun load(
        offset: Int,
        limit: Int,
        previous: PhotosState.Loaded? = null,
    ) {
        loadJob =
            viewModelScope.launch {
                try {
                    val page = library.photos(offset, limit)
                    ensureActive()
                    val combined = previous?.sections.orEmpty().flatMap { it.photos } + page.photos
                    _photos.value =
                        PhotosState.Loaded(
                            combined.distinctBy { it.id }.groupByDay(TimeZone.currentSystemDefault()),
                            page.total,
                            page.next,
                        )
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    ensureActive()
                    _photos.value =
                        when {
                            previous == null -> PhotosState.LoadFailed
                            previous.sections.isEmpty() -> previous.copy(appendFailed = true)
                            else -> previous
                        }
                }
            }
    }

    private companion object {
        const val PAGE_SIZE = 60
    }
}

/** Newest day first, newest photo first inside a day (Wiki Gallery UX: Content). */
internal fun List<Photo>.groupByDay(zone: TimeZone): List<PhotoSection> =
    sortedByDescending { it.takenAtMillis }
        .groupBy { Instant.fromEpochMilliseconds(it.takenAtMillis).toLocalDateTime(zone).date }
        .map { (date, photos) -> PhotoSection(date, photos) }
