package me.rerere.awara.ui.page.playlist

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.data.entity.Playlist
import me.rerere.awara.data.entity.Video
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching

class PlaylistDetailVM(
    savedStateHandle: SavedStateHandle,
    private val mediaRepo: MediaRepo
) : ViewModel() {
    val id = checkNotNull(savedStateHandle.get<String>("id"))
    var state by mutableStateOf(PlaylistDetailState())
        private set

    init {
        loadPlaylistDetail()
    }

    fun loadNextPage() {
        if (state.loadingMore || !state.hasMore) {
            return
        }
        loadPlaylistDetail(replaceResults = false)
    }

    fun loadPlaylistDetail(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.page + 1
        state = state.copy(
            loading = replaceResults,
            loadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                mediaRepo.getPlaylistContent(
                    playlistId = id,
                    page = targetPage - 1
                )
            }.onSuccess {
                val mergedList = if (replaceResults) it.results else state.list + it.results
                state = state.copy(
                    page = targetPage,
                    count = it.count,
                    list = mergedList,
                    playlist = it.playlist,
                    hasMore = mergedList.size < it.count,
                )
            }
            state = state.copy(loading = false, loadingMore = false)
        }
    }

    data class PlaylistDetailState(
        val loading: Boolean = false,
        val page: Int = 1,
        val count: Int = 0,
        val loadingMore: Boolean = false,
        val hasMore: Boolean = true,
        val list: List<Video> = emptyList(),
        val playlist: Playlist? = null,
    )
}