package com.lanread.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.data.local.entity.BookmarkEntity
import com.lanread.app.data.local.entity.HighlightEntity
import com.lanread.app.data.local.entity.LibraryItemEntity
import com.lanread.app.data.local.entity.ReadingProgressEntity
import kotlinx.coroutines.flow.Flow

data class LibraryEntry(
    @Embedded val item: LibraryItemEntity,
    @Relation(parentColumn = "bookId", entityColumn = "id") val book: BookEntity,
    @Relation(parentColumn = "bookId", entityColumn = "bookId") val progress: ReadingProgressEntity?
)

@Dao
interface LibraryDao {
    @Transaction
    @Query(
        """
        SELECT * FROM library_items
        ORDER BY COALESCE(lastAccessedAt, addedAt) DESC
        """
    )
    fun observeLibrary(): Flow<List<LibraryEntry>>

    @Query("SELECT * FROM books WHERE checksum = :checksum LIMIT 1")
    suspend fun findBookByChecksum(checksum: String): BookEntity?

    @Query("SELECT * FROM books WHERE id = :bookId LIMIT 1")
    suspend fun findBookById(bookId: String): BookEntity?

    @Query("SELECT * FROM library_items WHERE bookId = :bookId LIMIT 1")
    suspend fun findLibraryItemByBookId(bookId: String): LibraryItemEntity?

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun findProgressByBookId(bookId: String): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItem(item: LibraryItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: ReadingProgressEntity)

    @Update
    suspend fun updateBook(book: BookEntity)

    @Update
    suspend fun updateLibraryItem(item: LibraryItemEntity)

    @Update
    suspend fun updateProgress(progress: ReadingProgressEntity)

    @Query("UPDATE library_items SET isFavorite = :favorite WHERE id = :itemId")
    suspend fun setFavorite(itemId: String, favorite: Boolean)

    @Query("UPDATE library_items SET statusRaw = :status WHERE bookId = :bookId")
    suspend fun setStatusByBookId(bookId: String, status: String)

    @Query("UPDATE library_items SET statusRaw = :status, lastAccessedAt = :accessedAt WHERE bookId = :bookId")
    suspend fun setStatusAndAccess(bookId: String, status: String, accessedAt: Long)

    @Query("UPDATE library_items SET lastAccessedAt = :accessedAt WHERE bookId = :bookId")
    suspend fun touchAccessed(bookId: String, accessedAt: Long)

    @Query("SELECT * FROM library_items WHERE statusRaw = 'reading'")
    suspend fun readingItems(): List<LibraryItemEntity>

    @Query("DELETE FROM library_items WHERE bookId = :bookId")
    suspend fun deleteLibraryItemByBookId(bookId: String)

    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteProgressByBookId(bookId: String)

    @Query("DELETE FROM bookmarks WHERE bookId = :bookId")
    suspend fun deleteBookmarksByBookId(bookId: String)

    @Query("DELETE FROM highlights WHERE bookId = :bookId")
    suspend fun deleteHighlightsByBookId(bookId: String)

    @Query("DELETE FROM annotations WHERE bookId = :bookId")
    suspend fun deleteAnnotationsByBookId(bookId: String)

    @Query("SELECT COUNT(*) FROM books")
    suspend fun bookCount(): Int

    @Query("SELECT * FROM books")
    suspend fun allBooks(): List<BookEntity>

    @Query("SELECT COALESCE(SUM(coverImageData IS NULL), 0) FROM books")
    suspend fun booksWithoutCover(): Int

    @Query("UPDATE books SET aiSummary = NULL, aiKeyPoints = NULL, aiSummaryGeneratedAt = NULL, skimmingSummaries = NULL")
    suspend fun clearAiCaches()

    @Transaction
    suspend fun removeBookCompletely(bookId: String) {
        deleteProgressByBookId(bookId)
        deleteBookmarksByBookId(bookId)
        deleteHighlightsByBookId(bookId)
        deleteAnnotationsByBookId(bookId)
        deleteLibraryItemByBookId(bookId)
        deleteBook(bookId)
    }

    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()

    @Query("DELETE FROM library_items")
    suspend fun deleteAllLibraryItems()

    @Query("DELETE FROM reading_progress")
    suspend fun deleteAllProgress()

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()

    @Query("DELETE FROM highlights")
    suspend fun deleteAllHighlights()

    @Query("DELETE FROM annotations")
    suspend fun deleteAllAnnotations()
}

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    fun observeByBook(bookId: String): Flow<List<BookmarkEntity>>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC")
    suspend fun byBook(bookId: String): List<BookmarkEntity>

    @Query(
        "SELECT * FROM bookmarks WHERE bookId = :bookId AND chapterIndex = :chapterIndex AND pageIndex = :pageIndex LIMIT 1"
    )
    suspend fun findAtLocation(bookId: String, chapterIndex: Int, pageIndex: Int): BookmarkEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmark: BookmarkEntity)

    @Query("UPDATE bookmarks SET colorHex = :colorHex WHERE id = :id")
    suspend fun updateColor(id: String, colorHex: String)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAll()
}

@Dao
interface HighlightDao {
    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY updatedAt DESC")
    fun observeByBook(bookId: String): Flow<List<HighlightEntity>>

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY updatedAt DESC")
    suspend fun byBook(bookId: String): List<HighlightEntity>

    @Query("SELECT * FROM highlights WHERE id = :id LIMIT 1")
    suspend fun byId(id: String): HighlightEntity?

    @Query("SELECT * FROM highlights WHERE bookId = :bookId ORDER BY createdAt DESC")
    suspend fun byBookByCreatedDesc(bookId: String): List<HighlightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(highlight: HighlightEntity)

    @Update
    suspend fun update(highlight: HighlightEntity)

    @Query("UPDATE highlights SET colorHex = :colorHex, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateColor(id: String, colorHex: String, updatedAt: Long)

    @Query("UPDATE highlights SET note = :note, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNote(id: String, note: String?, updatedAt: Long)

    @Query("DELETE FROM highlights WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM highlights")
    suspend fun deleteAll()
}
