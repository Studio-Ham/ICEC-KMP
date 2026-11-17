import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import java.net.URI
import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

// ML Kit iOS through the community SwiftPM package (ADR-0003). Bump the package and the model bundle together.
val mlKitSwiftPmVersion = "9.0.3"
val faceModelSha256 = "a81acdd21b76228981f6332b0e7f2166e297528d756fb0099d98930d58eb51c3"

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        // ML Kit registers Objective-C categories that static linking drops without -ObjC.
        it.binaries.all { linkerOpts("-ObjC") }
    }

    swiftPMDependencies {
        swiftPackage(
            url = url("https://github.com/d-date/google-mlkit-swiftpm.git"),
            version = exact(mlKitSwiftPmVersion),
            products = listOf(product("MLKitFaceDetection")),
        )
    }

    android {
        namespace = "com.studioham.icec.platform.facedetection"
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
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.mlkit.faceDetection)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

/** The iOS face model ML Kit loads from the app bundle. Without it detection silently finds no faces. */
val faceModelZip = layout.buildDirectory.file("mlkit/GoogleMVFaceDetectorResources.bundle.zip")
val downloadFaceModel by tasks.registering {
    val url =
        "https://github.com/d-date/google-mlkit-swiftpm/releases/download/" +
            "$mlKitSwiftPmVersion/GoogleMVFaceDetectorResources.bundle.zip"
    val sha256 = faceModelSha256
    val zip = faceModelZip
    inputs.property("sha256", sha256)
    outputs.file(zip)
    doLast {
        val file = zip.get().asFile
        file.parentFile.mkdirs()
        URI(url).toURL().openStream().use { input -> file.outputStream().use { input.copyTo(it) } }
        val actual = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        check(actual == sha256) { "Face model checksum mismatch: $actual" }
    }
}

val faceModel by tasks.registering(Sync::class) {
    dependsOn(downloadFaceModel)
    from(zipTree(faceModelZip))
    into(layout.buildDirectory.dir("mlkit/bundle"))
}

tasks.withType<KotlinNativeTest>().configureEach {
    // simctl forwards only SIMCTL_CHILD_* variables, without the prefix, to simulator processes.
    environment(
        "SIMCTL_CHILD_ICEC_FIXTURES",
        layout.projectDirectory
            .dir("fixtures")
            .asFile.absolutePath,
    )
}

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    binaries.withType<TestExecutable>().configureEach {
        val copy =
            tasks.register<Copy>("copyFaceModel${name.replaceFirstChar(Char::uppercase)}${target.name.replaceFirstChar(Char::uppercase)}") {
                from(faceModel)
                into(outputDirectory)
            }
        linkTaskProvider.configure { finalizedBy(copy) }
        tasks.withType<KotlinNativeTest>().matching { it.name.startsWith(target.name) }.configureEach { dependsOn(copy) }
    }
}
