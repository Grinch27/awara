package me.rerere.awara.data.feed

// TODO(user): Decide whether API and local database queries should keep sharing one query model or fork into explicit transport-specific mappers later.
// TODO(agent): If server query semantics change again, centralize all field-name compatibility here instead of leaking it back into ViewModels.

import me.rerere.awara.domain.feed.FeedFilter
import me.rerere.awara.domain.feed.FeedQuery
import me.rerere.awara.domain.feed.FeedScope

fun FeedQuery.toApiParams(): Map<String, String> {
    val params = linkedMapOf(
        "limit" to pageSize.toString(),
        "page" to page.toString(),
    )

    sort?.takeIf(String::isNotBlank)?.let { value ->
        params["sort"] = value
    }
    keyword?.trim()?.takeIf(String::isNotEmpty)?.let { value ->
        params["query"] = value
    }
    if (scope == FeedScope.SUBSCRIPTION_VIDEO || scope == FeedScope.SUBSCRIPTION_IMAGE) {
        params["subscribed"] = "true"
    }

    val groupedFilters = filters.filterIsInstance<FeedFilter.KeyValue>()
        .groupBy(keySelector = { filter -> filter.key }, valueTransform = { filter -> filter.value })

    groupedFilters.forEach { (key, values) ->
        params[key] = values.joinToString(separator = ",")
    }

    return params
}