// TODO(user): Decide whether search should stay self-contained here or later shed shared cards/chrome into a dedicated :ui module.
// TODO(agent): If another feature needs the same filter chrome, extract those widgets once the API and visuals settle instead of forking them again.

plugins {
    id("awara.android.library")
    id("awara.android.compose")
}

android {
    namespace = "me.rerere.awara.feature.search"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":data"))

    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.compose.ui:ui:1.4.2")
    implementation("androidx.compose.ui:ui-graphics:1.4.2")
    implementation("androidx.compose.ui:ui-util:1.4.2")
    implementation("androidx.compose.material3:material3:1.1.0-rc01")
    implementation("androidx.compose.material:material-icons-extended:1.4.2")
    implementation("io.coil-kt:coil-compose:2.3.0")
    implementation("com.github.re-ovo:compose-setting:1.1")
    implementation("io.insert-koin:koin-androidx-compose:3.4.4")
}