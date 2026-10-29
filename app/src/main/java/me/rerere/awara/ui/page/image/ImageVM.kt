package me.rerere.awara.ui.page.image

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import me.rerere.awara.data.entity.MediaProgress
import me.rerere.awara.data.source.onError
import me.rerere.awara.data.source.onException
import me.rerere.awara.data.entity.HistoryItem
import me.rerere.awara.data.entity.HistoryType
import me.rerere.awara.data.entity.Image
import me.rerere.awara.data.entity.thumbnailUrl
import me.rerere.awara.data.repo.MediaRepo
import me.rerere.awara.data.repo.UserRepo
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching
import me.rerere.awara.di.AppDatabase
import java.time.Instant

class ImageVM(
    savedStateHandle: SavedStateHandle,
    private val mediaRepo: MediaRepo,
    private val userRepo: UserRepo,
    private val appDatabase: AppDatabase
) : ViewModel() {
    val id = checkNotNull(savedStateHandle.get<String>("id"))

    var state by mutableStateOf(ImageState())
        private set

    init {
        load()
    }

    fun followOrUnfollow() {
        viewModelScope.launch {
            runAPICatching {
                if(state.state?.user?.following == true) {
                    userRepo.unfollowUser(state.state?.user?.id ?: return@runAPICatching)
                } else {
                    userRepo.followUser(state.state?.user?.id ?: return@runAPICatching)
                }
            }.onSuccess {
                load()
            }
        }
    }

    fun likeOrDislike() {
        viewModelScope.launch {
            state = state.copy(likeLoading = true)
            runAPICatching {
                if(state.state?.liked == true) {
                    mediaRepo.unlikeImage(id)
                } else {
                    mediaRepo.likeImage(id)
                }

                mediaRepo.getImage(id).let {
                    state = state.copy(state = it)
                }
            }
            state = state.copy(likeLoading = false)
        }
    }

    fun load() {
        viewModelScope.launch {
            state = state.copy(loading = true, failed = false)
            runAPICatching {
                mediaRepo.getImage(id)
            }.onSuccess {
                val progress = appDatabase.transferDao().mediaProgress("image", id)
                val restoredPage = it.files.indexOfFirst { file -> file.id == progress?.fileId }
                    .takeIf { page -> page >= 0 } ?: (progress?.position?.toInt() ?: 0).coerceIn(0, (it.files.size - 1).coerceAtLeast(0))
                state = state.copy(state = it, savedPage = restoredPage)
                writeHistory()
            }.onError { state = state.copy(failed = true) }
                .onException { state = state.copy(failed = true) }
            state = state.copy(loading = false)
        }
    }

    fun savePage(page: Int) {
        val file = state.state?.files?.getOrNull(page) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.transferDao().saveProgress(MediaProgress("image", id, file.id, page.toLong(),
                state.state?.files?.size?.toLong() ?: 0))
        }
    }

    private fun writeHistory() {
        if (!me.rerere.compose_setting.preference.mmkvPreference.getBoolean("setting.record_history", true)) return
        viewModelScope.launch {
            kotlin.runCatching {
                appDatabase.historyDao().insertHistory(
                    HistoryItem(
                        time = Instant.now(),
                        type = HistoryType.IMAGE,
                        resourceId = id,
                        title = state.state?.title ?: "",
                        thumbnail = state.state?.thumbnailUrl() ?: ""
                    )
                )
            }.onFailure {
                it.printStackTrace()
            }
        }
    }


    data class ImageState(
        val loading: Boolean = false,
        val failed: Boolean = false,
        val savedPage: Int = 0,
        val likeLoading: Boolean = false,
        val state: Image? = null,
    )
}