package me.rerere.awara.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import me.rerere.awara.data.entity.DownloadItem

@Dao
interface DownloadDao {
    @Query("select * from downloaded_items order by time desc")
    fun getDownloadedItems(): PagingSource<Int, DownloadItem>

    @Query("select * from downloaded_items order by time desc")
    suspend fun getAllDownloadedItems(): List<DownloadItem>

    @Query("select count(*) from downloaded_items")
    suspend fun countDownloadedItems(): Int

    @Insert
    suspend fun insertDownloadItem(downloadItem: DownloadItem)

    @Insert
    suspend fun insertDownloadItems(downloadItems: List<DownloadItem>)

    @Delete
    suspend fun deleteDownloadItem(downloadItem: DownloadItem)

    @Query("select * from downloaded_items where resourceId = :resourceId")
    suspend fun getDownloadItem(resourceId: String): DownloadItem?

    @Query("delete from downloaded_items")
    suspend fun clearAllDownloadedItems()

    @Transaction
    suspend fun replaceAllDownloadedItems(downloadItems: List<DownloadItem>) {
        clearAllDownloadedItems()
        if (downloadItems.isNotEmpty()) {
            insertDownloadItems(downloadItems)
        }
    }
}