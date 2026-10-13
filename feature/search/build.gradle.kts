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
    // Stable BOM: Compose 1.6.8 / Material3 1.2.1, within the current compileSdk 34 lane.
    implementation(platform("androidx.compose:compose-bom:+"))
    implementation(project(":core:model"))
    implementation(project(":data"))

    implementation("androidx.core:core-ktx:+")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt:coil-compose:+")
    implementation("com.github.re-ovo:compose-setting:+")
    implementation("io.insert-koin:koin-androidx-compose:+")
}