// TODO(user): Decide whether build-logic should remain a local included build or evolve into a shared conventions repository later.
// TODO(agent): If version catalogs land next, move toolchain coordinates there instead of scattering them across convention sources.

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}