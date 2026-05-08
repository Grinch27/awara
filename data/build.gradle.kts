// TODO(user): Decide when the rest of repositories should move from :app into :data so this module becomes the real data boundary.
// TODO(agent): If more shared mapping logic lands here, add dedicated tests instead of relying only on app-level workflow builds.

plugins {
    id("awara.android.library")
}

android {
    namespace = "me.rerere.awara.data.module"
}

dependencies {
    implementation(project(":core:model"))
}