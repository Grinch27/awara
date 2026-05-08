package me.rerere.awara.domain.feed

// TODO(user): Decide when the UI-side FilterValue bridge can be removed after all pages adopt typed feed filters.
// TODO(agent): If more legacy filter entry points appear, move this compatibility layer behind a clearer legacy package rather than keeping it next to the canonical model.

import me.rerere.awara.ui.component.iwara.param.FilterValue

fun List<FilterValue>.toFeedFilters(): List<FeedFilter> {
    return map { filter ->
        FeedFilter.KeyValue(key = filter.key, value = filter.value)
    }
}

fun List<FeedFilter>.toLegacyFilterValues(): List<FilterValue> {
    return mapNotNull { filter ->
        when (filter) {
            is FeedFilter.KeyValue -> FilterValue(key = filter.key, value = filter.value)
        }
    }
}