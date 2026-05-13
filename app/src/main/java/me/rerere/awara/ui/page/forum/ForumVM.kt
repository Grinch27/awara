package me.rerere.awara.ui.page.forum

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import me.rerere.awara.data.entity.ForumPost
import me.rerere.awara.data.entity.ForumSection
import me.rerere.awara.data.entity.ForumThread
import me.rerere.awara.data.repo.ForumRepo
import me.rerere.awara.data.source.onError
import me.rerere.awara.data.source.onException
import me.rerere.awara.data.source.onSuccess
import me.rerere.awara.data.source.runAPICatching
import me.rerere.awara.ui.component.common.UiState

class ForumIndexVM(
    private val forumRepo: ForumRepo,
) : ViewModel() {
    var state by mutableStateOf(ForumIndexState())
        private set

    init {
        loadSections()
    }

    fun loadSections() {
        state = state.copy(uiState = UiState.Loading)
        viewModelScope.launch {
            runAPICatching {
                forumRepo.getSections()
            }.onSuccess { sections ->
                state = state.copy(
                    uiState = if (sections.isEmpty()) UiState.Empty else UiState.Success,
                    sections = sections,
                )
            }.onError { error ->
                state = state.copy(uiState = UiState.Error(messageText = error.message))
            }.onException { error ->
                state = state.copy(uiState = UiState.Error(throwable = error.exception, messageText = error.exception.readableMessage()))
            }
        }
    }
}

data class ForumIndexState(
    val uiState: UiState = UiState.Initial,
    val sections: List<ForumSection> = emptyList(),
)

class ForumSectionVM(
    savedStateHandle: SavedStateHandle,
    private val forumRepo: ForumRepo,
) : ViewModel() {
    val sectionId: String = Uri.decode(checkNotNull(savedStateHandle.get<String>("sectionId")))
    var state by mutableStateOf(ForumSectionState(section = ForumSection(id = sectionId)))
        private set

    init {
        loadThreads()
    }

    fun loadNextPage() {
        if (state.loadingMore || !state.hasMore) {
            return
        }
        loadThreads(replaceResults = false)
    }

    fun loadThreads(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.page + 1
        state = state.copy(
            uiState = if (replaceResults) UiState.Loading else state.uiState,
            loadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                forumRepo.getSectionThreads(sectionId, targetPage - 1)
            }.onSuccess { page ->
                val mergedThreads = if (replaceResults) {
                    page.threads
                } else {
                    state.threads + page.threads
                }
                state = state.copy(
                    uiState = if (mergedThreads.isEmpty()) UiState.Empty else UiState.Success,
                    section = page.section,
                    threads = mergedThreads,
                    page = targetPage,
                    count = page.count,
                    loadingMore = false,
                    hasMore = mergedThreads.size < page.count,
                )
            }.onError { error ->
                state = state.copy(
                    uiState = if (replaceResults) UiState.Error(messageText = error.message) else state.uiState,
                    loadingMore = false,
                )
            }.onException { error ->
                state = state.copy(
                    uiState = if (replaceResults) {
                        UiState.Error(throwable = error.exception, messageText = error.exception.readableMessage())
                    } else {
                        state.uiState
                    },
                    loadingMore = false,
                )
            }
        }
    }
}

data class ForumSectionState(
    val uiState: UiState = UiState.Initial,
    val section: ForumSection = ForumSection(),
    val threads: List<ForumThread> = emptyList(),
    val page: Int = 1,
    val count: Int = 0,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
)

class ForumThreadVM(
    savedStateHandle: SavedStateHandle,
    private val forumRepo: ForumRepo,
) : ViewModel() {
    val threadId: String = Uri.decode(checkNotNull(savedStateHandle.get<String>("threadId")))
    var state by mutableStateOf(ForumThreadState(thread = ForumThread(id = threadId)))
        private set

    init {
        loadPosts()
    }

    fun loadNextPage() {
        if (state.loadingMore || !state.hasMore) {
            return
        }
        loadPosts(replaceResults = false)
    }

    fun loadPosts(replaceResults: Boolean = true) {
        val targetPage = if (replaceResults) 1 else state.page + 1
        state = state.copy(
            uiState = if (replaceResults) UiState.Loading else state.uiState,
            loadingMore = !replaceResults,
        )
        viewModelScope.launch {
            runAPICatching {
                forumRepo.getThreadPosts(threadId, targetPage - 1)
            }.onSuccess { page ->
                val mergedPosts = if (replaceResults) {
                    page.results
                } else {
                    state.posts + page.results
                }
                state = state.copy(
                    uiState = UiState.Success,
                    thread = page.thread,
                    posts = mergedPosts,
                    page = targetPage,
                    count = page.count,
                    pendingCount = page.pendingCount ?: 0,
                    loadingMore = false,
                    hasMore = mergedPosts.size < page.count,
                )
            }.onError { error ->
                state = state.copy(
                    uiState = if (replaceResults) UiState.Error(messageText = error.message) else state.uiState,
                    loadingMore = false,
                )
            }.onException { error ->
                state = state.copy(
                    uiState = if (replaceResults) {
                        UiState.Error(throwable = error.exception, messageText = error.exception.readableMessage())
                    } else {
                        state.uiState
                    },
                    loadingMore = false,
                )
            }
        }
    }
}

data class ForumThreadState(
    val uiState: UiState = UiState.Initial,
    val thread: ForumThread = ForumThread(),
    val posts: List<ForumPost> = emptyList(),
    val page: Int = 1,
    val count: Int = 0,
    val pendingCount: Int = 0,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = true,
)

private fun Throwable.readableMessage(): String = localizedMessage ?: message ?: javaClass.simpleName
