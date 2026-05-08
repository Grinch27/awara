// TODO(user): Decide whether future non-Compose Android modules should avoid this plugin entirely or use a lighter UI convention.
// TODO(agent): If Compose compiler options diverge by feature, split the shared opt-ins from module-local experimental flags instead of bloating this plugin.

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import me.rerere.awara.buildlogic.configureAwaraComposeCompiler
import org.gradle.kotlin.dsl.configure

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

plugins.withId("com.android.application") {
    extensions.configure<ApplicationExtension> {
        buildFeatures {
            compose = true
        }
    }
}

plugins.withId("com.android.library") {
    extensions.configure<LibraryExtension> {
        buildFeatures {
            compose = true
        }
    }
}

configureAwaraComposeCompiler()