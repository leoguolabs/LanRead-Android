package com.lanread.app.data

import android.content.Context
import com.lanread.app.data.local.LanReadDatabase
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.data.local.entity.LibraryItemEntity
import com.lanread.app.data.local.entity.ReadingProgressEntity
import com.lanread.app.util.EPubParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

class BookImportService(
    private val context: Context,
    private val database: LanReadDatabase
) {
    val appContext: Context get() = context
    val isImporting = MutableStateFlow(false)
    val importProgress = MutableStateFlow(0.0)
    val importError = MutableStateFlow<String?>(null)

    val booksDirectory: File
        get() = File(context.filesDir, "Books").apply { mkdirs() }

    sealed class ImportResult {
        data class Success(val book: BookEntity) : ImportResult()
        data class Failure(val messageKey: String, val arg: String? = null) : ImportResult()
    }

    suspend fun importBook(from: File): BookEntity = withContext(Dispatchers.IO) {
        isImporting.value = true
        importProgress.value = 0.0
        importError.value = null
        try {
            setProgress(0.1)
            val checksum = sha256(from)
            setProgress(0.3)
            database.libraryDao().findBookByChecksum(checksum)?.let {
                throw ImportException("book_already_exists", it.title)
            }
            setProgress(0.4)
            val metadata = EPubParser.parse(from)
            setProgress(0.6)
            val fileName = "${UUID.randomUUID()}.epub"
            val destination = File(booksDirectory, fileName)
            from.copyTo(destination, overwrite = true)
            setProgress(0.8)

            val now = System.currentTimeMillis()
            val bookId = UUID.randomUUID().toString()
            val chaptersJson = JSONArray().apply {
                metadata.chapters.forEach { chapter ->
                    put(
                        JSONObject()
                            .put("title", chapter.title)
                            .put("content", chapter.content)
                            .put("order", chapter.order.toString())
                    )
                }
            }.toString()

            val book = BookEntity(
                id = bookId,
                title = metadata.title,
                author = metadata.author,
                language = metadata.language,
                coverImageData = metadata.coverImageData,
                filePath = fileName,
                fileFormat = "epub",
                fileSize = from.length(),
                checksum = checksum,
                totalPages = metadata.totalPages,
                createdAt = now,
                updatedAt = now,
                metadata = chaptersJson,
                aiSummary = null,
                aiKeyPoints = null,
                aiSummaryGeneratedAt = null,
                skimmingSummaries = null,
                notionPageId = null
            )
            val libraryItem = LibraryItemEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                statusRaw = "want_to_read",
                tags = null,
                isFavorite = false,
                rating = 0,
                addedAt = now,
                lastAccessedAt = now,
                sortOrder = (now / 1000).toInt()
            )
            val initialPosition = JSONObject()
                .put("chapterIndex", 0)
                .put("pageIndex", 0)
                .put("totalPages", 1)
                .put("globalPage", 1)
                .put("bookTotalPages", maxOf(metadata.totalPages, 1))
                .toString()
            val progress = ReadingProgressEntity(
                id = UUID.randomUUID().toString(),
                bookId = bookId,
                currentPage = 0,
                currentChapter = null,
                currentPosition = initialPosition,
                progressPercentage = 0.0,
                lastReadAt = now,
                totalReadingTime = 0,
                detailedReadingTime = 0,
                skimmingReadingTime = 0,
                createdAt = now,
                updatedAt = now
            )
            database.libraryDao().insertBook(book)
            database.libraryDao().insertLibraryItem(libraryItem)
            database.libraryDao().insertProgress(progress)
            setProgress(1.0)
            book
        } finally {
            isImporting.value = false
        }
    }

    private suspend fun setProgress(value: Double) {
        importProgress.value = value
    }

    fun locateBookFile(book: BookEntity): File {
        val direct = File(book.filePath)
        if (direct.exists()) return direct
        return File(booksDirectory, book.filePath)
    }

    fun deleteBookFile(book: BookEntity) {
        val file = locateBookFile(book)
        if (file.exists() && file.parentFile == booksDirectory) file.delete()
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read >= 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

class ImportException(val messageKey: String, val arg: String? = null) : Exception(messageKey)
