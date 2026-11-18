import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

// Java 17 bytecode, compiled with whatever JDK runs Gradle (17+).
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    // compileOnly: the plugin versions come from the root build's plugins block.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "openop.android.application"
            implementationClass = "io.github.aizenprivai.openop.buildlogic.AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "openop.android.library"
            implementationClass = "io.github.aizenprivai.openop.buildlogic.AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "openop.android.compose"
            implementationClass = "io.github.aizenprivai.openop.buildlogic.AndroidComposeConventionPlugin"
        }
        register("kotlinJvm") {
            id = "openop.kotlin.jvm"
            implementationClass = "io.github.aizenprivai.openop.buildlogic.KotlinJvmConventionPlugin"
        }
        register("ktlint") {
            id = "openop.ktlint"
            implementationClass = "io.github.aizenprivai.openop.buildlogic.KtlintConventionPlugin"
        }
        register("rustBridge") {
            id = "openop.rust-bridge"
            implementationClass = "io.github.aizenprivai.openop.buildlogic.RustBridgeConventionPlugin"
        }
    }
}
