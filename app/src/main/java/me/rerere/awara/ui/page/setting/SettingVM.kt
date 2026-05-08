package me.rerere.awara.ui.page.setting

// TODO(user): Decide whether diagnostics export/import should move behind an explicit developer mode toggle later.
// TODO(agent): If the settings page grows further, split diagnostics actions into a separate view model or feature module instead of keeping all file IO orchestration here.

import androidx.lifecycle.ViewModel
import me.rerere.awara.data.repo.AppLogRepo
import me.rerere.awara.data.repo.SavedFeedViewRepo

class SettingVM(
    private val appLogRepo: AppLogRepo,
    private val savedFeedViewRepo: SavedFeedViewRepo,
) : ViewModel() {
    suspend fun exportAppLogs(): String = appLogRepo.exportJson()

    suspend fun exportSavedFeedViews(): String = savedFeedViewRepo.exportJson()

    suspend fun importSavedFeedViews(content: String): Int = savedFeedViewRepo.importJson(content)

    suspend fun getSavedFeedViewCount(): Int = savedFeedViewRepo.count()
}