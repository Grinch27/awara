package me.rerere.awara.ui.page.savedview

// TODO(user): Decide whether saved view management should eventually support cross-device sync conflicts instead of staying local-first.
// TODO(agent): If saved view operations grow further, move the orchestration logic into a dedicated use-case layer instead of expanding this view model.

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.data.repo.SavedFeedViewRepo
import me.rerere.awara.domain.feed.FeedScope
import me.rerere.awara.domain.feed.SavedFeedView

class SavedFeedViewsVM(
    private val savedFeedViewRepo: SavedFeedViewRepo,
) : ViewModel() {
    var state by mutableStateOf(SavedFeedViewsState())

    init {
        viewModelScope.launch {
            refresh()
        }
    }

    suspend fun refresh() {
        state = state.copy(isLoading = true)
        state = state.copy(
            isLoading = false,
            views = savedFeedViewRepo.getAll(),
        )
    }

    suspend fun updateView(view: SavedFeedView) {
        savedFeedViewRepo.update(view)
        refresh()
    }

    suspend fun deleteView(id: String) {
        savedFeedViewRepo.delete(id)
        refresh()
    }

    suspend fun setPinned(view: SavedFeedView, pinned: Boolean) {
        savedFeedViewRepo.setPinned(view, pinned)
        refresh()
    }

    suspend fun movePinnedView(scope: FeedScope, viewId: String, moveUp: Boolean) {
        savedFeedViewRepo.movePinnedView(scope = scope, viewId = viewId, moveUp = moveUp)
        refresh()
    }

    data class SavedFeedViewsState(
        val isLoading: Boolean = true,
        val views: List<SavedFeedView> = emptyList(),
    )
}