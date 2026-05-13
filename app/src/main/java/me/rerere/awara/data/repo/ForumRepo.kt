package me.rerere.awara.data.repo

import me.rerere.awara.data.source.IwaraAPI

class ForumRepo(
    private val iwaraAPI: IwaraAPI,
) {
    suspend fun getSections() = iwaraAPI.getForumSections()

    suspend fun getSectionThreads(sectionId: String, page: Int) = iwaraAPI.getForumSection(sectionId, page)

    suspend fun getThreadPosts(threadId: String, page: Int) = iwaraAPI.getForumThread(threadId, page)
}
