package io.github.aizenprivai.openop.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/** Android library defaults: SDK levels, Java 17, strict lint, ktlint. Each module sets its own namespace. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with<Project, Unit>(target) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("openop.ktlint")

        extensions.configure<LibraryExtension> {
            compileSdk = libs.version("compile-sdk").toInt()
            defaultConfig {
                minSdk = libs.version("min-sdk").toInt()
                testInstrumentationRunner = OpenopAndroid.TEST_RUNNER
            }
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            lint {
                warningsAsErrors = true
                abortOnError = true
            }
        }

        dependencies.add("testImplementation", libs.library("junit4"))
    }
}
