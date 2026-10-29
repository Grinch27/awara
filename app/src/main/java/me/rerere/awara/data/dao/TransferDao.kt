// Requirements: reject late worker updates after pause/cancel; persist only task metadata.
// Verify: generation-guarded writes and deterministic queue ordering.
package me.rerere.awara.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import me.rerere.awara.data.entity.*

@Dao
interface TransferDao {
    @Query("SELECT * FROM download_tasks ORDER BY updatedAt DESC")
    fun observeTasks(): Flow<List<DownloadTask>>

    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun task(id: String): DownloadTask?

    @Query("SELECT * FROM download_tasks WHERE resourceId = :resourceId AND owner = :owner AND state NOT IN ('CANCELLED', 'COMPLETED') LIMIT 1")
    suspend fun activeTask(resourceId: String, owner: String): DownloadTask?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTask)

    @Query("UPDATE download_tasks SET state=:state, bytes=:bytes, totalBytes=:total, validator=:validator, targetPath=:path, error=:error, updatedAt=:now WHERE id=:id AND generation=:generation")
    suspend fun progress(id: String, generation: Long, state: String, bytes: Long, total: Long, validator: String, path: String, error: String = "", now: Long = System.currentTimeMillis()): Int

    @Query("UPDATE download_tasks SET state=:state, generation=generation+1, error='', updatedAt=:now WHERE id=:id AND state != 'COMPLETED'")
    suspend fun control(id: String, state: String, now: Long = System.currentTimeMillis()): Int

    @Query("SELECT * FROM media_progress WHERE type=:type AND resourceId=:id")
    suspend fun mediaProgress(type: String, id: String): MediaProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgress(progress: MediaProgress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markImport(commit: ImportCommit)

    @Query("SELECT COUNT(*) FROM import_commits WHERE id=:id")
    suspend fun importCommitted(id: String): Int

    @Query("DELETE FROM import_commits WHERE id=:id")
    suspend fun forgetImport(id: String)
}
