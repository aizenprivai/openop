plugins {
    id("openop.android.library")
    id("openop.rust-bridge")
}

android {
    namespace = "io.github.aizenprivai.openop.core"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    // UniFFI's Kotlin bindings call native code through JNA (decision D7).
    implementation(libs.jna) {
        artifact { type = "aar" }
    }

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
