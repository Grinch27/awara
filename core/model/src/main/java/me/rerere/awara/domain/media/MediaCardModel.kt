// Requirements: pure display values shared by feed and search.
// Verify: content identity is explicit; this type has no UI or repository dependency.
package me.rerere.awara.domain.media

data class MediaCardModel(
    val id: String,
    val title: String,
    val author: String,
    val createdAt: String,
    val thumbnail: String,
    val image: Boolean = false,
    val isPrivate: Boolean = false,
)
