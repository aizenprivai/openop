import java.util.Properties

plugins {
    id("openop.android.application")
    id("openop.android.compose")
}

// Release signing comes from an untracked keystore.properties at the repo root
// (keys: storeFile, storePassword, keyAlias, keyPassword). Without it, the
// release APK is built unsigned. Keys and passwords are never committed.
// Read through providers so the configuration cache tracks the file.
val keystoreProperties: Properties? =
    providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
        .asText
        .orNull
        ?.let { text -> Properties().apply { load(text.reader()) } }

fun Properties.required(key: String): String =
    requireNotNull(getProperty(key)?.takeIf { it.isNotBlank() }) { "keystore.properties is missing '$key'" }

android {
    namespace = "io.github.aizenprivai.openop"

    defaultConfig {
        applicationId = "io.github.aizenprivai.openop"
        versionCode = 1
        versionName = "0.1.0-dev"
    }

    signingConfigs {
        if (keystoreProperties != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.required("storeFile"))
                storePassword = keystoreProperties.required("storePassword")
                keyAlias = keystoreProperties.required("keyAlias")
                keyPassword = keystoreProperties.required("keyPassword")
            }
        }
    }

    buildTypes {
        // ABI filters (decision D3) also drop ABIs bundled in dependencies such as JNA.
        debug {
            applicationIdSuffix = ".debug"
            ndk {
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                abiFilters += listOf("arm64-v8a")
            }
            signingConfig = signingConfigs.findByName("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        // App icon is an open branding decision (CLAUDE.md §2.1).
        // Re-enable this check when the icon is approved.
        disable += "MissingApplicationIcon"
        // Release is arm64-v8a only by approved decision D3 (docs/phase-1-plan.md).
        // ChromeOS x86_64 support would need a new ABI decision.
        disable += "ChromeOsAbiSupport"
    }
}

dependencies {
    implementation(project(":core-bridge"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    testImplementation(libs.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
