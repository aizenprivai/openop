package io.github.aizenprivai.openop.buildlogic

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

/** Builds libopenop_ffi.so for the given Android ABIs with cargo-ndk. */
abstract class CargoNdkBuildTask : DefaultTask() {
    @get:Inject abstract val execOps: ExecOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rustSources: ConfigurableFileCollection

    @get:Internal abstract val rustDir: DirectoryProperty

    @get:Input abstract val abis: ListProperty<String>

    @get:Input abstract val release: Property<Boolean>

    @get:Input abstract val minSdk: Property<Int>

    @get:Input abstract val ndkVersion: Property<String>

    @get:Internal abstract val ndkDir: DirectoryProperty

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun build() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        val rust = rustDir.get().asFile
        val cargoHome = System.getenv("CARGO_HOME") ?: "${System.getProperty("user.home")}/.cargo"

        // Strip machine-specific paths from the binary (reproducible builds).
        // Passed as a TOML array via --config (not a space-separated env var),
        // so paths containing spaces work. TOML literal strings need no escaping
        // of Windows backslashes.
        val rustflags =
            listOf(
                "--remap-path-prefix=${rust.parentFile.absolutePath}=/openop",
                "--remap-path-prefix=$cargoHome=/cargo"
            ).onEach { require('\'' !in it) { "Path contains a single quote: $it" } }
                .joinToString(", ", "[", "]") { "'$it'" }

        val cargoArgs = mutableListOf("ndk", "--platform", minSdk.get().toString())
        abis.get().forEach { cargoArgs += listOf("-t", it) }
        cargoArgs += listOf("-o", out.absolutePath, "build", "-p", "openop-ffi", "--locked")
        abis.get().forEach { abi ->
            val triple = RUST_TARGETS[abi] ?: error("No Rust target for ABI $abi")
            cargoArgs += listOf("--config", "target.$triple.rustflags=$rustflags")
        }
        if (release.get()) cargoArgs += "--release"

        execOps.exec {
            workingDir = rust
            executable = "cargo"
            args(cargoArgs)
            environment("ANDROID_NDK_HOME", ndkDir.get().asFile.absolutePath)
        }
    }

    private companion object {
        val RUST_TARGETS = mapOf("arm64-v8a" to "aarch64-linux-android", "x86_64" to "x86_64-linux-android")
    }
}
