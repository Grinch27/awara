package me.rerere.awara.data.entity

import kotlinx.serialization.Serializable
import me.rerere.awara.util.EmptyStringSerializer
import me.rerere.awara.util.InstantSerializer
import java.time.Instant

@Serializable
data class ForumSection(
    @Serializable(with = EmptyStringSerializer::class)
    val id: String = "",
    @Serializable(with = EmptyStringSerializer::class)
    val group: String = "",
    val locked: Boolean = false,
    val numPosts: Int = 0,
    val numThreads: Int = 0,
    val lastThread: ForumThread? = null,
) {
    val displayTitle: String
        get() = id
            .replace('-', ' ')
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

@Serializable
data class ForumThread(
    @Serializable(with = EmptyStringSerializer::class)
    val id: String = "",
    val approved: Boolean = false,
    @Serializable(with = EmptyStringSerializer::class)
    val slug: String = "",
    @Serializable(with = EmptyStringSerializer::class)
    val section: String = "",
    @Serializable(with = EmptyStringSerializer::class)
    val title: String = "",
    val locked: Boolean = false,
    val sticky: Boolean = false,
    val lastPost: ForumLastPost? = null,
    val numViews: Int = 0,
    val numPosts: Int = 0,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    val user: ForumUser? = null,
)

@Serializable
data class ForumLastPost(
    @Serializable(with = EmptyStringSerializer::class)
    val id: String = "",
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    val user: ForumUser? = null,
    @Serializable(with = EmptyStringSerializer::class)
    val threadId: String = "",
)

@Serializable
data class ForumPost(
    @Serializable(with = EmptyStringSerializer::class)
    val id: String = "",
    val approved: Boolean = false,
    @Serializable(with = EmptyStringSerializer::class)
    val body: String = "",
    val replyNum: Int = 0,
    @Serializable(with = EmptyStringSerializer::class)
    val threadId: String = "",
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
    val user: ForumUser? = null,
    val thread: ForumThread? = null,
)

@Serializable
data class ForumUser(
    @Serializable(with = EmptyStringSerializer::class)
    val id: String = "",
    @Serializable(with = EmptyStringSerializer::class)
    val username: String = "",
    @Serializable(with = EmptyStringSerializer::class)
    val name: String = "",
    @Serializable(with = EmptyStringSerializer::class)
    val role: String = "",
    @Serializable(with = EmptyStringSerializer::class)
    val status: String = "",
    val premium: Boolean = false,
    val avatar: File? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
) {
    val displayName: String
        get() = name.ifBlank { username.ifBlank { id } }

    val displayHandle: String
        get() = if (username.isBlank()) id else "@$username"

    val hasNavigableProfile: Boolean
        get() = username.isNotBlank()
}

@Serializable
data class ForumSectionPage(
    val section: ForumSection = ForumSection(),
    val threads: List<ForumThread> = emptyList(),
    val count: Int = 0,
    val limit: Int = 32,
    val page: Int = 0,
    val pendingCount: Int? = null,
)

@Serializable
data class ForumThreadPage(
    val thread: ForumThread = ForumThread(),
    val results: List<ForumPost> = emptyList(),
    val count: Int = 0,
    val limit: Int = 32,
    val page: Int = 0,
    val pendingCount: Int? = null,
)
