// TODO(user): Decide whether dependency versions should move next into a version catalog once build-logic is stable.
// TODO(agent): If root-level shared verification tasks are added later, keep them here and leave module conventions inside build-logic.
// Resolve new releases on each build, but never opt into preview, RC or snapshots.
configurations.configureEach {
    resolutionStrategy.componentSelection.all {
        val unstable = Regex("(?i)(?:^|[.-])(?:alpha|beta|rc|cr|snapshot|preview|canary|dev|eap|milestone|m[0-9]+)(?:[.-]?[0-9]+)?(?:$|[.-])")
        if (unstable.containsMatchIn(candidate.version)) {
            reject("Awara uses stable releases only")
        }
    }
}
