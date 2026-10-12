// TODO: If the Gradle 9 build still fails after this pass, the next likely upgrade surface is the Compose/Accompanist dependency set rather than app business code.
// TODO: If memory leak analysis is needed again later, prefer an explicit opt-in debug flavor over shipping LeakCanary in the default debug install.
plugins {
    id("awara.android.application")
    id("awara.android.compose")
    id("awara.android.room")
    id("awara.kotlin.serialization")
}

android {
    namespace = "me.rerere.awara"

    defaultConfig {
        applicationId = "me.rerere.awara"
        versionCode = 9
        versionName = "1.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters.addAll(listOf(
                "arm64-v8a",
                "x86_64",
            ))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        optIn.addAll(
            "androidx.compose.material3.ExperimentalMaterial3Api",
            "androidx.compose.material.ExperimentalMaterialApi",
            "androidx.compose.animation.ExperimentalAnimationApi",
            "androidx.compose.foundation.ExperimentalFoundationApi",
            "com.google.accompanist.pager.ExperimentalPagerApi",
            "coil.annotation.ExperimentalCoilApi",
            "androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi"
        )
    }
}

dependencies {
    // Stable BOM: Compose 1.6.8 / Material3 1.2.1, within the current compileSdk 34 lane.
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    val roomVersion = "2.8.4"

    implementation(project(":core:model"))
    implementation(project(":data"))
    implementation(project(":feature:player"))
    implementation(project(":feature:search"))

    // Android KTX
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // MDC (仅用于提供动态取色算法，用于在视频页面动态取色)
    implementation("com.google.android.material:material:1.9.0")

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.ui:ui-util")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-extended")

    // Media
    // Media3 replaces legacy ExoPlayer 2. All media3 artifacts use the same version.
    val media3Version = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Paging3
    implementation("androidx.paging:paging-runtime:3.3.2")
    implementation("androidx.paging:paging-compose:3.3.2")

    // Accompanist
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("com.google.accompanist:accompanist-permissions:0.29.2-rc")

    // Splash
    implementation("androidx.core:core-splashscreen:1.0.1")

    // MMUPnP
    implementation("net.mm2d.mmupnp:mmupnp:3.1.6")
    // Setting
    implementation("com.github.re-ovo:compose-setting:1.1")

    // Koin
    implementation("io.insert-koin:koin-androidx-compose:3.4.4")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.10.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    // Lottie
    implementation("com.airbnb.android:lottie-compose:6.0.0")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

    // Markdown
    implementation("org.jetbrains:markdown:0.4.1")

    // Coil
    implementation("io.coil-kt:coil-compose:2.3.0")
    implementation("io.coil-kt:coil-svg:2.3.0")

    // Profile Installer
    implementation("androidx.profileinstaller:profileinstaller:1.3.1")

    // Test
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    // Instrumented Media3/DoH transport smoke tests need a direct test compile dependency.
    androidTestImplementation("androidx.media3:media3-datasource-okhttp:$media3Version")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}