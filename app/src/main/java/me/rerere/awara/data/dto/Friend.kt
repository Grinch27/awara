package me.rerere.awara.data.dto

import kotlinx.serialization.Serializable
import me.rerere.awara.data.entity.User
import me.rerere.awara.util.InstantSerializer
import java.time.Instant

@Serializable
data class FriendStatusDto(
    val status: String,
)

@Serializable
data class FriendRequestDto(
    val id: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    val target: User,
    val user: User
)

enum class FriendStatus {
    NONE,
    PENDING,
    FRIENDS;

    companion object {
        fun parse(status: String): FriendStatus {
            return when(status) {
                "none" -> NONE
                "pending" -> PENDING
                "friends" -> FRIENDS
                else -> NONE
            }
        }
    }
}