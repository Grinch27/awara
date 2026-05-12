package me.rerere.awara.ui.page.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.data.entity.Playlist
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching

class PlaylistsVM(
    savedStateHandle: SavedStateHandle,
    private val mediaRepo: MediaRepo
) : ViewModel() {
    private val userId = checkNotNull(savedStateHandle.get<String>("userId"))
    var state by mutableStateOf(PlaylistsState())
        private set

    init {
        loadPlaylist()
    }

    fun loadNextPage() {
        if (state.loadingMore || !state.hasMore) {
            return
        }
        loadPlaylist(replaceResults = false)
    }

    fun loadPlaylist(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.page + 1
        state = state.copy(
            loading = replaceResults,
            loadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                mediaRepo.getPlaylists(
                    userId = userId,
                    page = targetPage - 1,
                )
            }.onSuccess {
                val mergedList = if (replaceResults) it.results else state.list + it.results
                state = state.copy(
                    page = targetPage,
                    list = mergedList,
                    count = it.count,
                    hasMore = mergedList.size < it.count,
                )
            }
            state = state.copy(
                loading = false,
                loadingMore = false,
            )
        }
    }

    data class PlaylistsState(
        val loading: Boolean = false,
        val page: Int = 1,
        val count: Int = 0,
        val loadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val list: List<Playlist> = emptyList()
    )
}