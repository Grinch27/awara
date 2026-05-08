package me.rerere.awara.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import me.rerere.awara.data.entity.AppLogEntry

@Dao
interface AppLogDao {
    @Insert
    suspend fun insert(entry: AppLogEntry)

    @Query("SELECT * FROM app_logs ORDER BY time DESC, id DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<AppLogEntry>

    @Query("SELECT COUNT(*) FROM app_logs")
    suspend fun count(): Int

    @Query(
        """
        DELETE FROM app_logs
        WHERE id NOT IN (
            SELECT id FROM app_logs
            ORDER BY time DESC, id DESC
            LIMIT :limit
        )
        """
    )
    suspend fun trimToLimit(limit: Int)

    @Transaction
    suspend fun append(entry: AppLogEntry, limit: Int) {
        insert(entry)
        trimToLimit(limit)
    }
}