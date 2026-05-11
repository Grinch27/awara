package me.rerere.awara.ui.page.setting

// TODO(user): Decide whether diagnostics export/import should move behind an explicit developer mode toggle later.
// TODO(agent): If the settings page grows further, split diagnostics actions into a separate view model or feature module instead of keeping all file IO orchestration here.

import androidx.lifecycle.ViewModel
import me.rerere.awara.data.repo.AppLogRepo
import me.rerere.awara.data.repo.LocalDataRepo
import me.rerere.awara.data.repo.LocalDataSummary

class SettingVM(
    private val appLogRepo: AppLogRepo,
    private val localDataRepo: LocalDataRepo,
) : ViewModel() {
    suspend fun exportAppLogs(): String = appLogRepo.exportJson()

    suspend fun exportLocalDataBackup(): String = localDataRepo.exportBackupJson()

    suspend fun importLocalDataBackup(content: String): LocalDataSummary = localDataRepo.importBackupJson(content)

    suspend fun getLocalDataSummary(): LocalDataSummary = localDataRepo.getSummary()
}