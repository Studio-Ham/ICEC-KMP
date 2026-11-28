import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.studioham.icec.designsystem"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.ui)
            implementation(libs.compose.runtime)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.foundation)
        }
        androidMain.dependencies {
            // Already on the app classpath through Compose UI; declared for fold and hinge features.
            implementation(libs.androidx.window)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.studioham.icec.designsystem.resources"
}
