package com.studioham.icec.feature.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studioham.icec.designsystem.IcecColors
import com.studioham.icec.designsystem.Pretendard
import com.studioham.icec.feature.gallery.resources.Res
import com.studioham.icec.feature.gallery.resources.gallery_back
import com.studioham.icec.feature.gallery.resources.gallery_count
import com.studioham.icec.feature.gallery.resources.gallery_date_other_year
import com.studioham.icec.feature.gallery.resources.gallery_date_this_year
import com.studioham.icec.feature.gallery.resources.gallery_date_today
import com.studioham.icec.feature.gallery.resources.gallery_date_yesterday
import com.studioham.icec.feature.gallery.resources.gallery_empty_body
import com.studioham.icec.feature.gallery.resources.gallery_empty_title
import com.studioham.icec.feature.gallery.resources.gallery_load_failed_body
import com.studioham.icec.feature.gallery.resources.gallery_load_failed_title
import com.studioham.icec.feature.gallery.resources.gallery_months
import com.studioham.icec.feature.gallery.resources.gallery_permission_allow
import com.studioham.icec.feature.gallery.resources.gallery_permission_body
import com.studioham.icec.feature.gallery.resources.gallery_permission_title
import com.studioham.icec.feature.gallery.resources.gallery_retry
import com.studioham.icec.feature.gallery.resources.gallery_scroll_top
import com.studioham.icec.feature.gallery.resources.gallery_title
import com.studioham.icec.feature.gallery.resources.ic_arrow_up
import com.studioham.icec.feature.gallery.resources.ic_back
import com.studioham.icec.feature.gallery.resources.ic_chevron
import com.studioham.icec.feature.gallery.resources.ic_gear
import com.studioham.icec.feature.gallery.resources.ic_photo_large
import com.studioham.icec.feature.gallery.resources.ic_refresh
import com.studioham.icec.media.Photo
import com.studioham.icec.media.PhotoAccess
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import kotlin.time.Clock

private val TileBackground = Color(0xFF121212)

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val access by viewModel.access.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    GalleryContent(access, photos, onBack, viewModel::onAllowClick, viewModel::onRetryClick, viewModel::thumbnail, modifier)
}

@Composable
private fun GalleryContent(
    access: PhotoAccess,
    photos: PhotosState,
    onBack: () -> Unit,
    onAllowClick: () -> Unit,
    onRetryClick: () -> Unit,
    thumbnail: suspend (Photo, Int) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    val allowed = access == PhotoAccess.Granted || access == PhotoAccess.Limited
    val count = (photos as? PhotosState.Loaded)?.count?.takeIf { allowed }
    Column(modifier.fillMaxSize().background(Color.Black).windowInsetsPadding(WindowInsets.safeDrawing)) {
        TopBar(count, onBack)
        Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
            when {
                !allowed -> {
                    Message(Res.drawable.ic_photo_large, Res.string.gallery_permission_title, Res.string.gallery_permission_body) {
                        TextAction(Res.drawable.ic_gear, Res.string.gallery_permission_allow, chevron = true, onAllowClick)
                    }
                }

                photos is PhotosState.LoadFailed -> {
                    Message(Res.drawable.ic_photo_large, Res.string.gallery_load_failed_title, Res.string.gallery_load_failed_body) {
                        TextAction(Res.drawable.ic_refresh, Res.string.gallery_retry, chevron = false, onRetryClick)
                    }
                }

                photos is PhotosState.Loaded && photos.sections.isEmpty() -> {
                    Message(Res.drawable.ic_photo_large, Res.string.gallery_empty_title, Res.string.gallery_empty_body)
                }

                photos is PhotosState.Loaded -> {
                    PhotoGrid(photos.sections, thumbnail)
                }

                else -> {
                    Unit
                }
            }
        }
    }
}

/** Figma 316:2418 */
@Composable
private fun TopBar(
    count: Int?,
    onBack: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(IcecColors.StudioSlate)
            .drawBehind { drawLine(IcecColors.Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx()) },
    ) {
        Box(
            Modifier
                .padding(start = 16.dp)
                .size(40.dp)
                .align(Alignment.CenterStart)
                .clickable(role = Role.Button, onClick = onBack),
            Alignment.Center,
        ) { Image(painterResource(Res.drawable.ic_back), stringResource(Res.string.gallery_back)) }
        BasicText(
            stringResource(Res.string.gallery_title),
            Modifier.align(Alignment.Center),
            TextStyle(
                Color.White,
                15.sp,
                FontWeight.SemiBold,
                fontFamily = Pretendard(),
                letterSpacing = (-0.375).sp,
                lineHeight = 22.5.sp,
            ),
        )
        if (count != null) {
            BasicText(
                countText(count),
                Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                TextStyle(IcecColors.MutedSlate, 14.sp, fontFamily = Pretendard(), letterSpacing = (-0.35).sp, lineHeight = 21.sp),
            )
        }
    }
}

/** Figma 316:2412: date sections, 3 columns, 2dp gaps, scroll-to-top button. */
@Composable
private fun PhotoGrid(
    sections: List<PhotoSection>,
    thumbnail: suspend (Photo, Int) -> ImageBitmap?,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    Box(Modifier.fillMaxSize()) {
        // ponytail: fixed 3 columns per the phone design; Adaptive columns are a design follow-up.
        LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.fillMaxSize(),
            gridState,
            PaddingValues(top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            sections.forEachIndexed { index, section ->
                item(key = section.date.toString(), span = { GridItemSpan(maxLineSpan) }) {
                    SectionHeader(section, today, Modifier.padding(top = if (index == 0) 0.dp else 22.dp))
                }
                items(section.photos, key = { it.id }) { PhotoTile(it, thumbnail) }
            }
        }
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 24.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(IcecColors.StudioSlate)
                .border(BorderStroke(1.dp, IcecColors.NeonLime), CircleShape)
                .clickable(role = Role.Button) { scope.launch { gridState.animateScrollToItem(0) } },
            Alignment.Center,
        ) { Image(painterResource(Res.drawable.ic_arrow_up), stringResource(Res.string.gallery_scroll_top)) }
    }
}

@Composable
private fun SectionHeader(
    section: PhotoSection,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        Arrangement.SpaceBetween,
        Alignment.CenterVertically,
    ) {
        BasicText(
            dateLabel(section.date, today),
            style =
                TextStyle(
                    Color.White,
                    14.sp,
                    FontWeight.Medium,
                    fontFamily = Pretendard(),
                    letterSpacing = (-0.35).sp,
                    lineHeight = 21.sp,
                ),
        )
        BasicText(
            countText(section.photos.size),
            style = TextStyle(IcecColors.MutedSlate, 13.sp, fontFamily = Pretendard(), lineHeight = 19.5.sp),
        )
    }
}

@Composable
private fun PhotoTile(
    photo: Photo,
    thumbnail: suspend (Photo, Int) -> ImageBitmap?,
) {
    Box(Modifier.aspectRatio(1f).background(TileBackground)) {
        // ponytail: one request per visible tile, no cache; add an LRU cache if scrolling back reloads visibly.
        val sizePx = with(LocalDensity.current) { 130.dp.toPx().roundToInt() }
        val image by produceState<ImageBitmap?>(null, photo.id) { value = thumbnail(photo, sizePx) }
        image?.let { Image(BitmapPainter(it), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
    }
}

/** PermissionRequired (316:2477), Empty (316:2506), LoadFailed (340:294). */
@Composable
private fun Message(
    icon: DrawableResource,
    title: StringResource,
    body: StringResource,
    action: @Composable () -> Unit = {},
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(icon), null)
        Spacer(Modifier.height(16.dp))
        BasicText(
            stringResource(title),
            style =
                TextStyle(
                    Color.White,
                    18.sp,
                    FontWeight.Bold,
                    fontFamily = Pretendard(),
                    letterSpacing = (-0.45).sp,
                    lineHeight = 27.sp,
                ),
        )
        BasicText(
            stringResource(body),
            style = TextStyle(IcecColors.MutedSlate, 14.sp, fontFamily = Pretendard(), lineHeight = 22.75.sp, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(12.dp))
        action()
    }
}

@Composable
private fun TextAction(
    icon: DrawableResource,
    label: StringResource,
    chevron: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        Arrangement.spacedBy(6.dp),
        Alignment.CenterVertically,
    ) {
        Image(painterResource(icon), null)
        BasicText(
            stringResource(label),
            style = TextStyle(IcecColors.NeonLime, 14.sp, FontWeight.Medium, fontFamily = Pretendard(), lineHeight = 21.sp),
        )
        if (chevron) Image(painterResource(Res.drawable.ic_chevron), null)
    }
}

@Composable
private fun countText(count: Int): String = pluralStringResource(Res.plurals.gallery_count, count, count.withThousands())

@Composable
private fun dateLabel(
    date: LocalDate,
    today: LocalDate,
): String {
    val month = stringArrayResource(Res.array.gallery_months)[date.month.ordinal]
    val day = date.day.toString()
    return when {
        date == today -> stringResource(Res.string.gallery_date_today, month, day)
        date == today.minus(1, DateTimeUnit.DAY) -> stringResource(Res.string.gallery_date_yesterday, month, day)
        date.year == today.year -> stringResource(Res.string.gallery_date_this_year, month, day)
        else -> stringResource(Res.string.gallery_date_other_year, month, day, date.year.toString())
    }
}

internal fun Int.withThousands(): String =
    toString()
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()

@Preview
@Composable
private fun PermissionRequiredPreview() {
    GalleryContent(PhotoAccess.NotDetermined, PhotosState.Loading, {}, {}, {}, { _, _ -> null })
}

@Preview
@Composable
private fun EmptyPreview() {
    GalleryContent(PhotoAccess.Granted, PhotosState.Loaded(emptyList()), {}, {}, {}, { _, _ -> null })
}

@Preview
@Composable
private fun LoadFailedPreview() {
    GalleryContent(PhotoAccess.Granted, PhotosState.LoadFailed, {}, {}, {}, { _, _ -> null })
}
