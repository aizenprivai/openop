package io.github.aizenprivai.openop.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/** ktlint with the pinned engine version; style rules come from .editorconfig. */
class KtlintConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with<Project, Unit>(target) {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")

        extensions.configure<KtlintExtension> {
            version.set(libs.version("ktlint-engine"))
            android.set(true)
            filter {
                // Generated code (e.g. UniFFI bindings) is not ours to format.
                exclude { it.file.path.contains("/build/generated/") }
            }
        }
    }
}
