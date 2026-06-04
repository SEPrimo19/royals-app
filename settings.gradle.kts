pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Supabase + Ktor snapshots / GitHub-hosted artifacts resolve from Maven Central,
        // but JitPack is kept as a fallback for transitive community libraries.
        maven { url = uri("https://jitpack.io") }
    }
    // Version catalog "libs" is auto-loaded from gradle/libs.versions.toml
}

rootProject.name = "GRACE"
include(":app")
