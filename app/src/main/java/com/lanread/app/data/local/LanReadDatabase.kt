package com.lanread.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.lanread.app.data.local.dao.AnnotationDao
import com.lanread.app.data.local.dao.BookMappingDao
import com.lanread.app.data.local.dao.BookmarkDao
import com.lanread.app.data.local.dao.HighlightDao
import com.lanread.app.data.local.dao.LibraryDao
import com.lanread.app.data.local.dao.NotionSyncConfigDao
import com.lanread.app.data.local.dao.SyncQueueItemDao
import com.lanread.app.data.local.entity.AnnotationEntity
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.data.local.entity.BookMappingEntity
import com.lanread.app.data.local.entity.BookmarkEntity
import com.lanread.app.data.local.entity.HighlightEntity
import com.lanread.app.data.local.entity.LibraryItemEntity
import com.lanread.app.data.local.entity.NotionSyncConfigEntity
import com.lanread.app.data.local.entity.ReadingProgressEntity
import com.lanread.app.data.local.entity.SyncQueueItemEntity

@Database(
    entities = [
        BookEntity::class,
        LibraryItemEntity::class,
        ReadingProgressEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        AnnotationEntity::class,
        BookMappingEntity::class,
        NotionSyncConfigEntity::class,
        SyncQueueItemEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LanReadDatabase : RoomDatabase() {
    abstract fun libraryDao(): LibraryDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun bookMappingDao(): BookMappingDao
    abstract fun notionSyncConfigDao(): NotionSyncConfigDao
    abstract fun syncQueueItemDao(): SyncQueueItemDao

    companion object {
        fun build(context: Context): LanReadDatabase =
            Room.databaseBuilder(context, LanReadDatabase::class.java, "Isla_Reader")
                .fallbackToDestructiveMigration()
                .build()
    }
}
