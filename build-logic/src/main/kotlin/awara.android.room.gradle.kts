// TODO(user): Decide whether Room schema export should remain local to modules that use Room or become a stricter repo-wide requirement.
// TODO(agent): If multiple modules adopt Room, add migration-test conventions here instead of scattering schema config and test fixtures manually.

import com.google.devtools.ksp.gradle.KspExtension
import org.gradle.kotlin.dsl.configure

plugins {
    id("com.google.devtools.ksp")
}

extensions.configure<KspExtension> {
    arg("room.schemaLocation", project.layout.projectDirectory.dir("schemas").asFile.path)
}