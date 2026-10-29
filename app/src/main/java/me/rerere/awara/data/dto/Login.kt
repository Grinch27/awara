package me.rerere.awara.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRes(
    val token: String,
) { override fun toString() = "LoginRes([redacted])" }

@Serializable
data class LoginReq(
    val email: String,
    val password: String,
) { override fun toString() = "LoginReq([redacted])" }

@Serializable
data class AccessTokenRes(
    val accessToken: String
) { override fun toString() = "AccessTokenRes([redacted])" }