pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Cashfree's SDK resolves from mavenCentral() above; jitpack is
        // listed as a fallback in Cashfree's own troubleshooting docs for
        // some transitive dependencies — harmless to keep even if unused.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PizzaTown"

include(":admin-app")
include(":delivery-app")
include(":customer-app")
