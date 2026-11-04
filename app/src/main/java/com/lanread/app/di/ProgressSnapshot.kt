package com.lanread.app.di

import com.lanread.app.ui.progress.RecentEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Snapshot of reading entries for the Progress statistics screen. */
suspend fun AppContainer.progressSnapshot(): List<RecentEntry> = withContext(Dispatchers.IO) {
    val dao = database.libraryDao()
    val entries = dao.observeLibrary().first()
    entries.map { entry ->
        RecentEntry(
            bookId = entry.book.id,
            title = entry.book.title,
            author = entry.book.author,
            cover = entry.book.coverImageData,
            progress = entry.progress?.progressPercentage ?: 0.0,
            lastReadAt = entry.progress?.lastReadAt ?: entry.item.addedAt,
            detailedSeconds = entry.progress?.detailedReadingTime ?: 0,
            skimmingSeconds = entry.progress?.skimmingReadingTime ?: 0,
            statusFinished = entry.item.statusRaw == "finished"
        )
    }
}
