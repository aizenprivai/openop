package io.github.aizenprivai.openop.buildlogic

import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

/** Generates the Kotlin bindings from a host build of openop-ffi (decision D8: not committed). */
abstract class UniffiBindgenTask : DefaultTask() {
    @get:Inject abstract val execOps: ExecOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rustSources: ConfigurableFileCollection

    @get:Internal abstract val rustDir: DirectoryProperty

    @get:OutputDirectory abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()
        val rust = rustDir.get().asFile

        execOps.exec {
            workingDir = rust
            executable = "cargo"
            args("build", "-p", "openop-ffi", "--locked")
        }

        // Respect CARGO_TARGET_DIR and .cargo/config: ask Cargo where it builds.
        val metadata = ByteArrayOutputStream()
        execOps.exec {
            workingDir = rust
            executable = "cargo"
            args("metadata", "--format-version", "1", "--no-deps", "--locked")
            standardOutput = metadata
        }
        val targetDir =
            (JsonSlurper().parseText(metadata.toString(Charsets.UTF_8)) as Map<*, *>)["target_directory"] as String

        val os = System.getProperty("os.name").lowercase()
        val libName =
            when {
                os.contains("windows") -> "openop_ffi.dll"
                os.contains("mac") -> "libopenop_ffi.dylib"
                else -> "libopenop_ffi.so"
            }

        execOps.exec {
            workingDir = rust
            executable = "cargo"
            args(
                "run", "-q", "-p", "uniffi-bindgen", "--locked", "--",
                "generate",
                "--library", File(targetDir, "debug/$libName").absolutePath,
                "--language", "kotlin",
                "--no-format",
                "--out-dir", out.absolutePath
            )
        }
    }
}
