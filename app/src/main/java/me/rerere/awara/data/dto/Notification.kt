package me.rerere.awara.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val friendRequests: Int = 0,
    val messages: Int = 0,
    val notifications: Int = 0
)