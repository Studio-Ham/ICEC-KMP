package com.studioham.icec

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.studioham.icec.resources.Res
import com.studioham.icec.resources.app_name
import org.jetbrains.compose.resources.stringResource

// ponytail: placeholder Home until feature/home lands with the design system.
@Composable
fun App(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(stringResource(Res.string.app_name), style = TextStyle(color = Color.White))
    }
}

@Preview
@Composable
private fun AppPreview() {
    App()
}
