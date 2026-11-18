package com.studioham.icec

import android.graphics.Color
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.studioham.icec.face.AndroidFaceDetector
import com.studioham.icec.media.AndroidPhotoLibrary
import com.studioham.icec.media.AndroidPhotoPermission

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val graph = createAppGraph(AndroidPhotoPermission(this), AndroidPhotoLibrary(this), AndroidFaceDetector(this))
        setContent { App(graph) }
    }
}
