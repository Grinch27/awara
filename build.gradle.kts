// TODO(user): Decide whether dependency versions should move next into a version catalog once build-logic is stable.
// TODO(agent): If root-level shared verification tasks are added later, keep them here and leave module conventions inside build-logic.
// Resolve new releases on each build, but never opt into preview, RC or snapshots.
configurations.configureEach {
    val currentConfiguration = this
    resolutionStrategy.componentSelection.all {
        // Do not reject pre-release transitive dependencies required by stable artifacts.
        val requestedDirectly = currentConfiguration.allDependencies.any { dependency ->
            dependency.group == candidate.group && dependency.name == candidate.module &&
                dependency.version == "+"
        }
        if (requestedDirectly) {
            val unstable = Regex("(?i)(?:^|[.-])(?:alpha|beta|rc|cr|snapshot|preview|canary|dev|eap|milestone|m[0-9]+)(?:[.-]?[0-9]+)?(?:$|[.-])")
            if (unstable.containsMatchIn(candidate.version)) {
                reject("Awara selects stable versions for dynamic direct dependencies")
            }
        }
    }
}
