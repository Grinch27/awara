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
    implementation("androidx.core:core-ktx:1.10.0")
    implementation("androidx.compose.ui:ui:1.4.2")
    implementation("androidx.compose.ui:ui-graphics:1.4.2")
    implementation("androidx.compose.ui:ui-util:1.4.2")
    implementation("androidx.compose.material3:material3:1.1.0-rc01")
    implementation("androidx.compose.material:material-icons-extended:1.4.2")
    implementation("com.google.android.exoplayer:exoplayer:2.18.6")
    implementation("net.mm2d.mmupnp:mmupnp:3.1.6")
    implementation("com.github.re-ovo:compose-setting:1.1")
}