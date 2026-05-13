package me.rerere.awara.data.entity

import kotlinx.serialization.Serializable
import me.rerere.awara.data.source.IPager

@Serializable
data class Playlist(
    val id: String = "",
    val numVideos: Int = 0,
    val title: String = "",
    val user: User? = null,
    val added: Boolean? = null,
    val thumbnail: PlaylistThumbnail? = null,
)

@Serializable
data class PlaylistThumbnail(
    val file: File? = null,
    val thumbnail: Int = 0,
)

@Serializable
data class PlaylistPager(
    override val count: Int,
    override val limit: Int,
    override val page: Int,
    override val results: List<Video>,
    val playlist: Playlist
) : IPager<Video>

@Serializable
data class PlaylistCreationDto(
    val title: String
)