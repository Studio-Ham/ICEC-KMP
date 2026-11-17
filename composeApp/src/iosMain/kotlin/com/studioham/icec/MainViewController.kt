package com.studioham.icec

import androidx.compose.ui.window.ComposeUIViewController
import com.studioham.icec.face.IosFaceDetector
import com.studioham.icec.media.IosPhotoLibrary
import com.studioham.icec.media.IosPhotoPermission
import platform.UIKit.UIViewController

@Suppress("ktlint:standard:function-naming", "FunctionName")
fun MainViewController(): UIViewController {
    val graph = createAppGraph(IosPhotoPermission(), IosPhotoLibrary(), IosFaceDetector())
    return ComposeUIViewController { App(graph) }
}
