package me.rerere.awara.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import me.rerere.awara.data.entity.HistoryItem

@Dao
interface HistoryDao {
    @Query("select * from history_items order by time desc")
    fun getHistory(): PagingSource<Int, HistoryItem>

    @Query("select * from history_items order by time desc")
    suspend fun getAllHistory(): List<HistoryItem>

    @Query("select count(*) from history_items")
    suspend fun countHistory(): Int

    @Insert
    suspend fun insertHistory(historyItem: HistoryItem)

    @Insert
    suspend fun insertHistoryItems(historyItems: List<HistoryItem>)

    @Delete
    suspend fun deleteHistory(historyItem: HistoryItem)

    @Query("delete from history_items")
    suspend fun clearAllHistory()

    @Transaction
    suspend fun replaceAllHistory(historyItems: List<HistoryItem>) {
        clearAllHistory()
        if (historyItems.isNotEmpty()) {
            insertHistoryItems(historyItems)
        }
    }
}