package me.rerere.awara.ui.page.favorites

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.data.entity.FavoriteImage
import me.rerere.awara.data.entity.FavoriteVideo
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching

class FavoritesVM(
    private val mediaRepo: MediaRepo
) : ViewModel() {
    var state by mutableStateOf(FavoritesState())
        private set

    init {
        loadVideo()
        loadImages()
    }

    fun loadNextVideoPage() {
        if (state.videoLoadingMore || !state.videoHasMore) {
            return
        }
        loadVideo(replaceResults = false)
    }

    fun loadNextImagePage() {
        if (state.imageLoadingMore || !state.imageHasMore) {
            return
        }
        loadImages(replaceResults = false)
    }

    fun loadVideo(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.videoPage + 1
        state = state.copy(
            videoLoading = replaceResults,
            videoLoadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                mediaRepo.getFavoriteVideos(targetPage - 1)
            }.onSuccess {
                val mergedList = if (replaceResults) it.results else state.videoList + it.results
                state = state.copy(
                    videoPage = targetPage,
                    videoCount = it.count,
                    videoList = mergedList,
                    videoHasMore = mergedList.size < it.count,
                )
            }
            state = state.copy(videoLoading = false, videoLoadingMore = false)
        }
    }

    fun loadImages(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.imagePage + 1
        state = state.copy(
            imageLoading = replaceResults,
            imageLoadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                mediaRepo.getFavoriteImages(targetPage - 1)
            }.onSuccess {
                val mergedList = if (replaceResults) it.results else state.imageList + it.results
                state = state.copy(
                    imagePage = targetPage,
                    imageCount = it.count,
                    imageList = mergedList,
                    imageHasMore = mergedList.size < it.count,
                )
            }
            state = state.copy(imageLoading = false, imageLoadingMore = false)
        }
    }

    data class FavoritesState(
        val videoLoading: Boolean = false,
        val videoPage: Int = 1,
        val videoCount: Int = 0,
        val videoLoadingMore: Boolean = false,
        val videoHasMore: Boolean = true,
        val videoList: List<FavoriteVideo> = emptyList(),
        val imageLoading: Boolean = false,
        val imagePage: Int = 1,
        val imageCount: Int = 0,
        val imageLoadingMore: Boolean = false,
        val imageHasMore: Boolean = true,
        val imageList: List<FavoriteImage> = emptyList(),
    )
}