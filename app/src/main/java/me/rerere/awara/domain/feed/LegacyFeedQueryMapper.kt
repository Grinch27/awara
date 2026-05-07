package me.rerere.awara.domain.feed

// TODO(user): Decide whether legacy query-to-param compatibility should be removed once saved feed views land.
// TODO(agent): If the server starts distinguishing q/query/keyword again, centralize that mapping here instead of duplicating it in ViewModels.
// TODO(agent): If local database filtering starts sharing this model, split API and local mappers into separate files.

import me.rerere.awara.ui.component.iwara.param.FilterValue

fun List<FilterValue>.toFeedFilters(): List<FeedFilter> {
    return map { filter ->
        FeedFilter.KeyValue(key = filter.key, value = filter.value)
    }
}

fun FeedQuery.toApiParams(): Map<String, String> {
    val params = linkedMapOf(
        "limit" to pageSize.toString(),
        "page" to page.toString(),
    )

    sort?.takeIf { it.isNotBlank() }?.let { value ->
        params["sort"] = value
    }
    keyword?.trim()?.takeIf { it.isNotEmpty() }?.let { value ->
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