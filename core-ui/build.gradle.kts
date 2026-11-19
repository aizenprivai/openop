// OPENOP design system: theme, tokens, shared components (Phase 2).
plugins {
    id("openop.android.library")
    id("openop.android.compose")
}

android {
    namespace = "io.github.aizenprivai.openop.designsystem"
}

dependencies {
    // Pure Kotlin destination model; lets one mapping hold labels, icons and planned phases.
    api(project(":core-navigation"))
}
