plugins {
    id("openop.android.library")
    id("openop.android.compose")
}

android {
    namespace = "io.github.aizenprivai.openop.feature.device"
}

dependencies {
    implementation(project(":core-ui"))
}
