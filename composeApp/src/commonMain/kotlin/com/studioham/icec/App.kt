package com.studioham.icec

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.studioham.icec.home.HomeScreen
import com.studioham.icec.resources.Res
import com.studioham.icec.resources.gallery_title
import org.jetbrains.compose.resources.stringResource

sealed interface Route {
    data object Home : Route

    data object Gallery : Route
}

// ponytail: in-memory back stack; a cold start begins at Home (UX-034). Android keeps it across
// rotation via configChanges in the manifest.
@Composable
fun App(modifier: Modifier = Modifier) {
    val backStack = remember { mutableStateListOf<Route>(Route.Home) }
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.removeLastOrNull() },
        entryProvider =
            entryProvider {
                entry<Route.Home> { HomeScreen(onLoadPhotoClick = { backStack.add(Route.Gallery) }) }
                // ponytail: placeholder until the Gallery feature lands.
                entry<Route.Gallery> {
                    Box(Modifier.fillMaxSize().background(Color.Black), Alignment.Center) {
                        BasicText(stringResource(Res.string.gallery_title), style = TextStyle(color = Color.White))
                    }
                }
            },
    )
}

@Preview
@Composable
private fun AppPreview() {
    App()
}
