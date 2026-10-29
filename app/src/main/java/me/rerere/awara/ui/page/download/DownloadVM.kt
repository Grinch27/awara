package me.rerere.awara.ui.page.download

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.awara.worker.DownloadScheduler
import me.rerere.awara.data.entity.DownloadItem
import me.rerere.awara.di.AppDatabase
import org.koin.core.component.KoinComponent
import me.rerere.awara.util.ManagedDownloadFiles
import org.koin.core.component.inject

class DownloadVM(
    private val appDatabase: AppDatabase,
) : ViewModel(), KoinComponent {
    private val appContext: Context by inject()

    val downloadedItems = Pager(
        config = PagingConfig(
            pageSize = 32,
            enablePlaceholders = false,
            initialLoadSize = 32,
            prefetchDistance = 4,
        ),
        pagingSourceFactory = {
            appDatabase.downloadDao().getDownloadedItems()
        }
    ).flow.cachedIn(viewModelScope)

    val tasks = appDatabase.transferDao().observeTasks()

    suspend fun controlTask(id: String, state: String) = withContext(Dispatchers.IO) {
        DownloadScheduler.control(appContext, appDatabase, id, state)
    }

    suspend fun delete(items: List<DownloadItem>, deleteFiles: Boolean) = withContext(Dispatchers.IO) {
        items.forEach { item ->
            if (deleteFiles && java.io.File(item.path).exists()) {
                check(ManagedDownloadFiles.deleteIfManaged(appContext, item)) { "Unable to remove managed file" }
            }
            appDatabase.downloadDao().deleteDownloadItem(item)
        }
    }
}
