package io.github.aizenprivai.openop.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Jetpack Compose with Material 3. Apply together with openop.android.application or .library. */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with<Project, Unit>(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { buildFeatures.compose = true }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { buildFeatures.compose = true }
        }

        val bom = dependencies.platform(libs.library("compose-bom"))
        dependencies.add("implementation", bom)
        dependencies.add("implementation", libs.library("compose-ui"))
        dependencies.add("implementation", libs.library("compose-ui-tooling-preview"))
        dependencies.add("implementation", libs.library("compose-material3"))
        dependencies.add("debugImplementation", libs.library("compose-ui-tooling"))
        dependencies.add("androidTestImplementation", bom)
        // Same lifecycle version in every module (see libs.versions.toml).
        dependencies.constraints.add("implementation", libs.library("androidx-lifecycle-runtime"))
    }
}
