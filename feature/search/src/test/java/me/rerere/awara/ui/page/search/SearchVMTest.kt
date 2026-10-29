// Requirements: late responses cannot replace a newer query; pagination must terminate.
// Verify: non-cooperative cancellation, duplicate pages, criteria restoration.
package me.rerere.awara.ui.page.search

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import me.rerere.awara.domain.feed.FeedQuery
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchVMTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { Dispatchers.resetMain() }

    @Test fun lateResponseCannotOverwriteNewQuery() = runTest(dispatcher) {
        val repository = FakeSearch()
        val vm = SearchVM(repository, SavedStateHandle())
        vm.query = "old"; vm.submitSearch(); runCurrent()
        vm.query = "new"; vm.submitSearch(); runCurrent()
        repository.responses[1].complete(SearchPageResult(1, listOf(item("new"))))
        runCurrent()
        repository.responses[0].complete(SearchPageResult(1, listOf(item("old"))))
        advanceUntilIdle()
        assertEquals(listOf("new"), vm.state.videoList.map { it.id })
        assertEquals(listOf("old", "new"), repository.queries.map { it.keyword })
    }

    @Test fun duplicatePageStopsAndKeepsStableIds() = runTest(dispatcher) {
        val repository = FakeSearch()
        val vm = SearchVM(repository, SavedStateHandle())
        vm.submitSearch(); runCurrent()
        repository.responses[0].complete(SearchPageResult(20, listOf(item("one"))))
        runCurrent(); vm.loadNextPage(); runCurrent()
        repository.responses[1].complete(SearchPageResult(20, listOf(item("one"))))
        advanceUntilIdle()
        assertEquals(1, vm.state.videoList.size)
        assertFalse(vm.state.hasMore)
    }

    @Test fun queryAndTypeAreRestored() = runTest(dispatcher) {
        val handle = SavedStateHandle(mapOf("query" to "restored", "type" to SearchTypes.USER))
        val vm = SearchVM(FakeSearch(), handle)
        assertEquals("restored", vm.query)
        assertEquals(SearchTypes.USER, vm.state.searchType)
    }

    private fun item(id: String) = SearchMediaItem(id, SearchMediaType.VIDEO, id, "author",
        0, 0, "", null, "", false)

    private class FakeSearch : SearchRepository {
        val responses = mutableListOf<CompletableDeferred<SearchPageResult<SearchMediaItem>>>()
        val queries = mutableListOf<FeedQuery>()
        override suspend fun searchVideos(query: FeedQuery): SearchPageResult<SearchMediaItem> {
            queries.add(query)
            val response = CompletableDeferred<SearchPageResult<SearchMediaItem>>()
            responses.add(response)
            return withContext(NonCancellable) { response.await() }
        }
        override suspend fun searchImages(query: FeedQuery) = error("Unexpected")
        override suspend fun searchUsers(query: String, page: Int) = error("Unexpected")
        override suspend fun searchPosts(query: String, page: Int) = error("Unexpected")
        override suspend fun searchPlaylists(query: String, page: Int) = error("Unexpected")
        override suspend fun searchForumPosts(query: String, page: Int) = error("Unexpected")
        override suspend fun searchForumThreads(query: String, page: Int) = error("Unexpected")
        override suspend fun suggestTags(query: String) = error("Unexpected")
        override suspend fun browseTags(filter: String, page: Int) = error("Unexpected")
    }
}
