package io.github.aizenprivai.openop.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Android application defaults: SDK levels, Java 17, strict lint, ktlint.
 * App-specific settings (ID, version, build types, signing) stay in app/build.gradle.kts.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with<Project, Unit>(target) {
        pluginManager.apply("com.android.application")
        pluginManager.apply("openop.ktlint")

        extensions.configure<ApplicationExtension> {
            compileSdk = libs.version("compile-sdk").toInt()
            // Lets AGP strip debug symbols from bundled native libraries.
            ndkVersion = libs.version("ndk")
            defaultConfig {
                minSdk = libs.version("min-sdk").toInt()
                targetSdk = libs.version("target-sdk").toInt()
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
    }
}
