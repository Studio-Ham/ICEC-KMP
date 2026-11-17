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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@Inject
class GalleryViewModel(
    private val permission: PhotoPermission,
    private val library: PhotoLibrary,
) : ViewModel() {
    private val _access = MutableStateFlow(permission.status())
    val access: StateFlow<PhotoAccess> = _access.asStateFlow()

    private val _photos = MutableStateFlow<PhotosState>(PhotosState.Loading)
    val photos: StateFlow<PhotosState> = _photos.asStateFlow()

    init {
        loadIfAllowed()
    }

    /** Re-reads the permission, e.g. after returning from the OS settings. */
    fun refresh() {
        _access.value = permission.status()
        if (_photos.value !is PhotosState.Loaded) loadIfAllowed()
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
        _photos.value = PhotosState.Loading
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
