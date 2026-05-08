package me.rerere.awara.data.entity

// TODO(user): Decide whether users without usernames should stay visible with an ID fallback label or be hidden from profile navigation entirely.
// TODO(agent): If upstream keeps relaxing user payloads, move this resilience from entity defaults into a dedicated DTO-to-domain mapper.

import kotlinx.serialization.Serializable
import me.rerere.awara.util.InstantSerializer
import java.time.Instant

@Serializable
data class User(
    val id: String,
    val username: String = "",
    val name: String = "",
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val deletedAt: Instant? = null,
    val followedBy: Boolean = false,
    val following: Boolean = false,
    val friend: Boolean = false,
    val premium: Boolean = false,
    val role: String = "",
    @Serializable(with = InstantSerializer::class)
    val seenAt: Instant? = null,
    val status: String = "",
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    val avatar: File? = null,
) {
    val displayName: String
        get() = name.ifBlank { username.ifBlank { id } }

    val displayHandle: String
        get() = if (username.isBlank()) id else "@$username"

    val hasNavigableProfile: Boolean
        get() = username.isNotBlank()
}

@Serializable
data class Follower(
    val id: Int,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val follower: User,
)

@Serializable
data class Following(
    val id: Int,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val user: User,
)