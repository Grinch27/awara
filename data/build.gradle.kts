// TODO(user): Decide when the rest of repositories should move from :app into :data so this module becomes the real data boundary.
// TODO(agent): If more shared mapping logic lands here, add dedicated tests instead of relying only on app-level workflow builds.

plugins {
    id("com.android.library")
}

android {
    namespace = "me.rerere.awara.data.module"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
}