// Requirements: shared compact Material 3 presentation, independent of app routing.
// Open decisions: spacing is checked on actual windows and fonts.
// Verify: app/search use one thumbnail policy and grid calculation.
plugins {
    id("awara.android.library")
    id("awara.android.compose")
}
android { namespace = "me.rerere.awara.core.ui" }
dependencies {
    implementation(platform("androidx.compose:compose-bom:+"))
    implementation(project(":core:model"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("io.coil-kt.coil3:coil-compose:+")
    implementation("com.github.re-ovo:compose-setting:+")
}
