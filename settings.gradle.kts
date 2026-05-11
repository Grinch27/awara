pluginManagement {
    includeBuild("build-logic")

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
        maven("https://jitpack.io")
    }
}
rootProject.name = "awara"
include(":app")
include(":core:model")
project(":core:model").projectDir = file("core/model")
include(":data")
include(":feature:player")
project(":feature:player").projectDir = file("feature/player")
include(":feature:search")
project(":feature:search").projectDir = file("feature/search")
