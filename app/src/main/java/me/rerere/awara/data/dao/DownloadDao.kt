package me.rerere.awara.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import me.rerere.awara.data.entity.DownloadItem

@Dao
interface DownloadDao {
    @Query("select * from downloaded_items order by time desc")
    fun getDownloadedItems(): PagingSource<Int, DownloadItem>

    @Insert
    suspend fun insertDownloadItem(downloadItem: DownloadItem)

    @Delete
    suspend fun deleteDownloadItem(downloadItem: DownloadItem)

    @Query("select * from downloaded_items where resourceId = :resourceId")
    suspend fun getDownloadItem(resourceId: String): DownloadItem?
}