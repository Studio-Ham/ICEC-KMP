package com.studioham.icec

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.studioham.icec.feature.gallery.GalleryScreen
import com.studioham.icec.home.HomeScreen

sealed interface Route {
    data object Home : Route

    data object Gallery : Route
}

// ponytail: in-memory back stack; a cold start begins at Home (UX-034). Android keeps it across
// rotation via configChanges in the manifest.
@Composable
fun App(
    graph: AppGraph,
    modifier: Modifier = Modifier,
) {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Route.Home> { HomeScreen(onLoadPhotoClick = { backStack.add(Route.Gallery) }) }
                entry<Route.Gallery> {
                    // ponytail: ViewModel is scoped to the host, not the entry; add the nav3 ViewModel decorator when a second
                    // ViewModel screen needs its own scope.
                    GalleryScreen(viewModel { graph.galleryViewModel }, onBack = { backStack.removeLastOrNull() })
                }
            },
    )
}
