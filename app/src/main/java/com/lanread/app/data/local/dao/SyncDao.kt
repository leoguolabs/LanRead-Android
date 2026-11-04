package com.lanread.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lanread.app.data.local.entity.AnnotationEntity
import com.lanread.app.data.local.entity.BookMappingEntity
import com.lanread.app.data.local.entity.NotionSyncConfigEntity
import com.lanread.app.data.local.entity.SyncQueueItemEntity

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY updatedAt DESC")
    suspend fun byBook(bookId: String): List<AnnotationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: AnnotationEntity)

    @Query("DELETE FROM annotations")
    suspend fun deleteAll()
}

@Dao
interface BookMappingDao {
    @Query("SELECT * FROM book_mappings WHERE bookID = :bookId LIMIT 1")
    suspend fun byBookId(bookId: String): BookMappingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(mapping: BookMappingEntity)

    @Query("DELETE FROM book_mappings WHERE bookID = :bookId")
    suspend fun deleteByBookId(bookId: String)

    @Query("DELETE FROM book_mappings")
    suspend fun deleteAll()
}

@Dao
interface NotionSyncConfigDao {
    @Query("SELECT * FROM notion_sync_config WHERE id = 1 LIMIT 1")
    suspend fun current(): NotionSyncConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(config: NotionSyncConfigEntity)

    @Query("DELETE FROM notion_sync_config")
    suspend fun deleteAll()
}

@Dao
interface SyncQueueItemDao {
    @Query("SELECT * FROM sync_queue_items ORDER BY createdAt ASC")
    suspend fun all(): List<SyncQueueItemEntity>

    @Query("SELECT * FROM sync_queue_items WHERE status = 'pending' ORDER BY createdAt ASC LIMIT 1")
    suspend fun oldestPending(): SyncQueueItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: SyncQueueItemEntity)

    @Update
    suspend fun update(item: SyncQueueItemEntity)

    @Query("UPDATE sync_queue_items SET status = 'pending' WHERE status = 'inProgress'")
    suspend fun resetInProgressToPending()

    @Query("DELETE FROM sync_queue_items WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM sync_queue_items WHERE targetBookId = :bookId")
    suspend fun deleteByBookId(bookId: String)

    @Query("DELETE FROM sync_queue_items")
    suspend fun deleteAll()
}
