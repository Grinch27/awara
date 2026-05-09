package me.rerere.awara.domain.feed

// TODO(user): Decide whether saved feed views should support only local persistence first or sync between devices later.
// TODO(agent): If query semantics diverge between video, image, and user search, split FeedQuery into media and profile variants instead of overloading one model.
// TODO(agent): If filter complexity grows, replace the generic key/value bridge with typed filter subclasses and dedicated serializers.

import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class FeedQuery(
    val scope: FeedScope,
    val keyword: String? = null,
    val sort: String? = null,
    val filters: List<FeedFilter> = emptyList(),
    val page: Int = 0,
    val pageSize: Int = 24,
)

@Serializable
enum class FeedScope {
    HOME_VIDEO,
    HOME_IMAGE,
    SUBSCRIPTION_VIDEO,
    SUBSCRIPTION_IMAGE,
    SEARCH_VIDEO,
    SEARCH_IMAGE,
    SEARCH_USER,
}

@Serializable
sealed interface FeedFilter {
    @Serializable
    data class KeyValue(
        val key: String,
        val value: String,
    ) : FeedFilter
}

@Serializable
data class SavedFeedView(
    val id: String,
    val name: String,
    val scope: FeedScope,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val sort: String? = null,
    val filters: List<FeedFilter> = emptyList(),
    val pinned: Boolean = false,
    val smartSubscription: Boolean = false,
    @Serializable(with = FeedInstantSerializer::class)
    val createdAt: Instant = Instant.now(),
    @Serializable(with = FeedInstantSerializer::class)
    val updatedAt: Instant = Instant.now(),
)

object FeedInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FeedDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(DateTimeFormatter.ISO_INSTANT.format(value))
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.from(DateTimeFormatter.ISO_INSTANT.parse(decoder.decodeString()))
    }
}