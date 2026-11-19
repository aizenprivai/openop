pluginManagement {
    // OPENOP convention plugins (openop.android.*, openop.rust-bridge, ...).
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "openop"

include(":app")
include(":core-bridge")
include(":core-navigation")
include(":core-ui")
include(":feature:dashboard")
include(":feature:files")
include(":feature:notes")
include(":feature:device")
include(":feature:tools")
include(":feature:ai")
