import groovy.json.JsonSlurper
import java.io.ByteArrayOutputStream
import java.io.File
import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ktlint)
}

// --- Rust build integration (decision D4: plain tasks + cargo-ndk) ---------

val rustDir: File = rootDir.resolve("rust")

/** Rust sources that affect the native library and bindings. */
fun rustInputs() = fileTree(rustDir) {
    include("Cargo.toml", "Cargo.lock", "rust-toolchain.toml", "crates/**", "uniffi-bindgen/**")
    exclude("**/target/**")
}

/** Version from [workspace.package] in rust/Cargo.toml, used by instrumented tests. */
// Read through providers so the configuration cache tracks the file.
val rustCoreVersion: String =
    Regex("""(?s)\[workspace\.package].*?\bversion\s*=\s*"([^"]+)"""")
        .find(providers.fileContents(rootProject.layout.projectDirectory.file("rust/Cargo.toml")).asText.get())
        ?.groupValues
        ?.get(1)
        ?: error("Could not read workspace version from rust/Cargo.toml")

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
        val triples = mapOf("arm64-v8a" to "aarch64-linux-android", "x86_64" to "x86_64-linux-android")

        val cargoArgs = mutableListOf("ndk", "--platform", minSdk.get().toString())
        abis.get().forEach { cargoArgs += listOf("-t", it) }
        cargoArgs += listOf("-o", out.absolutePath, "build", "-p", "openop-ffi", "--locked")
        abis.get().forEach { abi ->
            val triple = triples[abi] ?: error("No Rust target for ABI $abi")
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
}

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
        @Suppress("UNCHECKED_CAST")
        val targetDir =
            (JsonSlurper().parseText(metadata.toString(Charsets.UTF_8)) as Map<String, Any?>)[
                "target_directory"
            ] as String

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

val generateUniffiBindings =
    tasks.register<UniffiBindgenTask>("generateUniffiBindings") {
        group = "rust"
        description = "Generates Kotlin bindings for openop-ffi with UniFFI."
        rustSources.from(rustInputs())
        rustDir.set(rootProject.layout.projectDirectory.dir("rust"))
        outputDir.set(layout.buildDirectory.dir("generated/uniffi/kotlin"))
    }

android {
    namespace = "io.github.aizenprivai.openop.core"
    compileSdk = libs.versions.compile.sdk.get().toInt()
    ndkVersion = libs.versions.ndk.get()

    defaultConfig {
        minSdk = libs.versions.min.sdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        // Lets instrumented tests check the version that came through the bridge.
        buildConfigField("String", "EXPECTED_CORE_VERSION", "\"$rustCoreVersion\"")
    }

    buildFeatures {
        buildConfig = true
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

androidComponents {
    val ndkDirectory = sdkComponents.ndkDirectory
    onVariants { variant ->
        val isRelease = variant.buildType == "release"
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val cargoBuild =
            tasks.register<CargoNdkBuildTask>("cargoBuild$name") {
                group = "rust"
                description = "Builds openop-ffi for Android (${variant.name})."
                rustSources.from(rustInputs())
                rustDir.set(rootProject.layout.projectDirectory.dir("rust"))
                // Decision D3: x86_64 only in debug builds (emulators); release is arm64 only.
                abis.set(if (isRelease) listOf("arm64-v8a") else listOf("arm64-v8a", "x86_64"))
                release.set(isRelease)
                minSdk.set(libs.versions.min.sdk.get().toInt())
                ndkVersion.set(libs.versions.ndk.get())
                ndkDir.set(ndkDirectory)
                outputDir.set(layout.buildDirectory.dir("rustJniLibs/${variant.name}"))
            }
        variant.sources.jniLibs?.addGeneratedSourceDirectory(cargoBuild, CargoNdkBuildTask::outputDir)
        variant.sources.kotlin?.addGeneratedSourceDirectory(generateUniffiBindings, UniffiBindgenTask::outputDir)
    }
}

ktlint {
    version.set(libs.versions.ktlint.engine.get())
    android.set(true)
    filter {
        // Generated UniFFI code is not ours to format.
        exclude { it.file.path.contains("/build/generated/") }
    }
}

dependencies {
    // UniFFI's Kotlin bindings call native code through JNA (decision D7).
    implementation(libs.jna) {
        artifact { type = "aar" }
    }

    testImplementation(libs.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
