package me.rerere.awara.data.entity

import kotlinx.serialization.Serializable
import me.rerere.awara.util.EmptyStringSerializer
import me.rerere.awara.util.InstantSerializer
import java.time.Instant

@Serializable
data class Post(
    @Serializable(with = EmptyStringSerializer::class)
    val id: String = "",
    @Serializable(with = EmptyStringSerializer::class)
    val title: String = "",
    @Serializable(with = EmptyStringSerializer::class)
    val body: String = "",
    val numViews: Int = 0,
    val user: User? = null,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant? = null,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant? = null,
)