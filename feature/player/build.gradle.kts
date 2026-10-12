// TODO(user): Decide whether video detail UI should move into :feature:player next, or whether this module should stay limited to shared playback widgets.
// TODO(agent): If search extraction starts pulling shared chrome next, lift common Compose helpers into :ui before duplicating them across feature modules.

plugins {
    id("awara.android.library")
    id("awara.android.compose")
}

android {
    namespace = "me.rerere.awara.feature.player"
}

dependencies {
    // Stable BOM: Compose 1.6.8 / Material3 1.2.1, within the current compileSdk 34 lane.
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    val media3Version = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("net.mm2d.mmupnp:mmupnp:3.1.6")
    implementation("com.github.re-ovo:compose-setting:1.1")
}