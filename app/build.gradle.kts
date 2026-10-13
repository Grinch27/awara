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
    implementation(platform("androidx.compose:compose-bom:+"))
    val roomVersion = "+"

    implementation(project(":core:model"))
    implementation(project(":data"))
    implementation(project(":feature:player"))
    implementation(project(":feature:search"))

    // Android KTX
    implementation("androidx.core:core-ktx:+")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:+")
    implementation("androidx.activity:activity-compose:+")

    // MDC (仅用于提供动态取色算法，用于在视频页面动态取色)
    implementation("com.google.android.material:material:+")

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
    val media3Version = "+"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:+")

    // Room
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-paging:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Paging3
    implementation("androidx.paging:paging-runtime:+")
    implementation("androidx.paging:paging-compose:+")

    // Accompanist
    implementation("androidx.navigation:navigation-compose:+")
    implementation("com.google.accompanist:accompanist-permissions:+")

    // Splash
    implementation("androidx.core:core-splashscreen:+")

    // MMUPnP
    implementation("net.mm2d.mmupnp:mmupnp:+")
    // Setting
    implementation("com.github.re-ovo:compose-setting:+")

    // Koin
    implementation("io.insert-koin:koin-androidx-compose:+")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:+")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:+")
    implementation("com.squareup.okhttp3:logging-interceptor:+")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:+")

    // Lottie
    implementation("com.airbnb.android:lottie-compose:+")

    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:+")

    // Markdown
    implementation("org.jetbrains:markdown:+")

    // Coil
    implementation("io.coil-kt.coil3:coil:+")
    implementation("io.coil-kt.coil3:coil-compose:+")
    implementation("io.coil-kt.coil3:coil-network-okhttp:+")
    implementation("io.coil-kt.coil3:coil-svg:+")

    // Profile Installer
    implementation("androidx.profileinstaller:profileinstaller:+")

    // Test
    testImplementation("junit:junit:+")
    androidTestImplementation("androidx.test.ext:junit:+")
    androidTestImplementation("androidx.test.espresso:espresso-core:+")
    // Instrumented Media3/DoH transport smoke tests need a direct test compile dependency.
    androidTestImplementation("androidx.media3:media3-datasource-okhttp:$media3Version")
    androidTestImplementation(platform("androidx.compose:compose-bom:+"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
tasks.register("reportResolvedVersions") {
    group = "reporting"
    description = "Record actual resolved application dependencies for each build variant."
    doLast {
        val output = layout.buildDirectory.dir("reports/versions").get().asFile
        output.mkdirs()
        val names = listOf(
            "debugCompileClasspath", "debugRuntimeClasspath",
            "releaseCompileClasspath", "releaseRuntimeClasspath",
        )
        for (name in names) {
            val configuration = project.configurations.findByName(name) ?: continue
            val versions = configuration.incoming.resolutionResult.allComponents
                .mapNotNull { it.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier }
                .map { "${it.group}:${it.module}:${it.version}" }
                .distinct().sorted()
            output.resolve("${name}.txt").writeText(
                versions.joinToString(separator = "\n", postfix = "\n")
            )
        }
        output.resolve("build-environment.txt").writeText(
            "gradle=${gradle.gradleVersion}\n" +
                "java=${System.getProperty("java.version")}\n" +
                "compileSdk=37.0\nminSdk=26\ntargetSdk=33\n"
        )
    }
}
