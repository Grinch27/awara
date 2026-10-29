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
    // Latest stable BOM follows the shared dependency policy; CI records resolved versions.
    implementation(platform("androidx.compose:compose-bom:+"))
    implementation("androidx.core:core-ktx:+")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    val media3Version = "+"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")
    implementation("com.squareup.okhttp3:okhttp:+")
    implementation("net.mm2d.mmupnp:mmupnp:+")
    implementation("com.github.re-ovo:compose-setting:+")
}