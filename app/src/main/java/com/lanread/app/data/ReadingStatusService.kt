package com.lanread.app.data

import com.lanread.app.data.local.LanReadDatabase
import com.lanread.app.data.local.entity.LibraryItemEntity
import java.util.concurrent.TimeUnit

class ReadingStatusService(private val database: LanReadDatabase) {

    suspend fun updateAllReadingStatuses() {
        val items = database.libraryDao().readingItems()
        val now = System.currentTimeMillis()
        var updated = 0
        for (item in items) {
            if (shouldPause(item, now)) {
                database.libraryDao().setStatusByBookId(item.bookId, "paused")
                updated += 1
            }
        }
    }

    private suspend fun shouldPause(item: LibraryItemEntity, now: Long): Boolean {
        if (item.statusRaw != "reading") return false
        val lastAccessed = item.lastAccessedAt
            ?: database.libraryDao().findProgressByBookId(item.bookId)?.lastReadAt
            ?: return false
        val days = TimeUnit.MILLISECONDS.toDays(now - lastAccessed)
        return days >= 7
    }
}
