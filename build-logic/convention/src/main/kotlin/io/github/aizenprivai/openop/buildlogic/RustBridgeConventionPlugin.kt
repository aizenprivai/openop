package io.github.aizenprivai.openop.buildlogic

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register

/**
 * Builds the Rust core (rust/) into the module's jniLibs with cargo-ndk and adds
 * the UniFFI-generated Kotlin bindings as a source directory (decision D4).
 * Apply after openop.android.library.
 */
class RustBridgeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with<Project, Unit>(target) {
        pluginManager.apply("openop.android.library")

        val rustDir = rootProject.layout.projectDirectory.dir("rust")
        fun rustInputs() = fileTree(rustDir) {
            include("Cargo.toml", "Cargo.lock", "rust-toolchain.toml", "crates/**", "uniffi-bindgen/**")
            exclude("**/target/**")
        }

        // Version from [workspace.package] in rust/Cargo.toml, for instrumented tests.
        // Read through providers so the configuration cache tracks the file.
        val cargoToml = providers.fileContents(rustDir.file("Cargo.toml")).asText.get()
        // Only look inside the [workspace.package] section (up to the next section header).
        val workspacePackage =
            Regex("""(?ms)^\[workspace\.package]\s*$(.*?)(?=^\[|\z)""").find(cargoToml)?.groupValues?.get(1)
                ?: error("rust/Cargo.toml has no [workspace.package] section")
        val rustCoreVersion =
            Regex("""(?m)^\s*version\s*=\s*"([^"]+)"""").find(workspacePackage)?.groupValues?.get(1)
                ?: error("rust/Cargo.toml [workspace.package] has no version")

        val minSdkLevel = libs.version("min-sdk").toInt()
        val ndk = libs.version("ndk")

        extensions.configure<LibraryExtension> {
            ndkVersion = ndk
            buildFeatures.buildConfig = true
            defaultConfig {
                buildConfigField("String", "EXPECTED_CORE_VERSION", "\"$rustCoreVersion\"")
            }
        }

        val generateUniffiBindings =
            tasks.register<UniffiBindgenTask>("generateUniffiBindings") {
                group = "rust"
                description = "Generates Kotlin bindings for openop-ffi with UniFFI."
                rustSources.from(rustInputs())
                this.rustDir.set(rustDir)
                outputDir.set(layout.buildDirectory.dir("generated/uniffi/kotlin"))
            }

        val androidComponents = extensions.getByType<LibraryAndroidComponentsExtension>()
        val ndkDirectory = androidComponents.sdkComponents.ndkDirectory
        androidComponents.onVariants { variant ->
            val isRelease = variant.buildType == "release"
            val name = variant.name.replaceFirstChar { it.uppercase() }
            val cargoBuild =
                tasks.register<CargoNdkBuildTask>("cargoBuild$name") {
                    group = "rust"
                    description = "Builds openop-ffi for Android (${variant.name})."
                    rustSources.from(rustInputs())
                    this.rustDir.set(rustDir)
                    // Decision D3: x86_64 only in debug builds (emulators); release is arm64 only.
                    abis.set(if (isRelease) listOf("arm64-v8a") else listOf("arm64-v8a", "x86_64"))
                    release.set(isRelease)
                    minSdk.set(minSdkLevel)
                    ndkVersion.set(ndk)
                    ndkDir.set(ndkDirectory)
                    outputDir.set(layout.buildDirectory.dir("rustJniLibs/${variant.name}"))
                }
            variant.sources.jniLibs?.addGeneratedSourceDirectory(cargoBuild, CargoNdkBuildTask::outputDir)
            variant.sources.kotlin?.addGeneratedSourceDirectory(generateUniffiBindings, UniffiBindgenTask::outputDir)
        }
    }
}
