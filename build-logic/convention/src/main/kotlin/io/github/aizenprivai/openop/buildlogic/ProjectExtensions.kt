package io.github.aizenprivai.openop.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

/** The `libs` version catalog (gradle/libs.versions.toml). */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(alias: String): String = findVersion(alias).orElseThrow {
    IllegalStateException("Version '$alias' missing from gradle/libs.versions.toml")
}.requiredVersion

internal fun VersionCatalog.library(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow {
        IllegalStateException("Library '$alias' missing from gradle/libs.versions.toml")
    }

/** Settings shared by every OPENOP Android module. */
internal object OpenopAndroid {
    const val TEST_RUNNER = "androidx.test.runner.AndroidJUnitRunner"
}
