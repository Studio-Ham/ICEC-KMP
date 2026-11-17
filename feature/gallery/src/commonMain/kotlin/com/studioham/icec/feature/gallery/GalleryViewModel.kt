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
import kotlinx.coroutines.channels.Channel
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
    ) : PhotosState {
        val count = sections.sumOf { it.photos.size }
    }

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
                loadIfAllowed()
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

    private fun loadIfAllowed() {
        if (_access.value != PhotoAccess.Granted && _access.value != PhotoAccess.Limited) return
        if (_photos.value !is PhotosState.Loaded) _photos.value = PhotosState.Loading
        viewModelScope.launch {
            _photos.value =
                try {
                    PhotosState.Loaded(library.photos().groupByDay(TimeZone.currentSystemDefault()))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    PhotosState.LoadFailed
                }
        }
    }
}

/** Newest day first, newest photo first inside a day (Wiki Gallery UX: Content). */
internal fun List<Photo>.groupByDay(zone: TimeZone): List<PhotoSection> =
    sortedByDescending { it.takenAtMillis }
        .groupBy { Instant.fromEpochMilliseconds(it.takenAtMillis).toLocalDateTime(zone).date }
        .map { (date, photos) -> PhotoSection(date, photos) }
