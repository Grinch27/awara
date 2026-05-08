// TODO(user): Decide whether serialization should stay opt-in per module or become part of the default Android library convention later.
// TODO(agent): If serialization usage spreads widely, keep the plugin separate but centralize JSON library versions in one place rather than per-module dependencies.

plugins {
    kotlin("plugin.serialization")
}