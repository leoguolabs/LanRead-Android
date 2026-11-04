package com.lanread.app.ui.library

import com.lanread.app.data.local.entity.BookEntity
import com.lanread.app.data.local.entity.LibraryItemEntity
import com.lanread.app.data.local.entity.ReadingProgressEntity

enum class ReadingStatus(val raw: String) {
    WANT_TO_READ("want_to_read"),
    READING("reading"),
    FINISHED("finished"),
    PAUSED("paused");

    companion object {
        fun from(raw: String) = entries.firstOrNull { it.raw == raw } ?: WANT_TO_READ
    }
}

sealed class LibraryFilterType {
    object All : LibraryFilterType()
    object Favorites : LibraryFilterType()
    data class Status(val status: ReadingStatus) : LibraryFilterType()
}

data class LibraryCardData(
    val item: LibraryItemEntity,
    val book: BookEntity,
    val progress: ReadingProgressEntity?
) {
    val status: ReadingStatus get() = ReadingStatus.from(item.statusRaw)
    val progressPercentage: Double get() = progress?.progressPercentage ?: 0.0
}

data class LibraryUiState(
    val books: List<LibraryCardData> = emptyList(),
    val searchText: String = "",
    val selectedFilter: LibraryFilterType = LibraryFilterType.All
) {
    val filteredBooks: List<LibraryCardData>
        get() {
            var items = books
            val query = searchText.trim()
            if (query.isNotEmpty()) {
                items = items.filter { card ->
                    card.book.title.contains(query, ignoreCase = true) ||
                        card.book.author?.contains(query, ignoreCase = true) == true
                }
            }
            return when (val filter = selectedFilter) {
                is LibraryFilterType.All -> items
                is LibraryFilterType.Favorites -> items.filter { it.item.isFavorite }
                is LibraryFilterType.Status -> items.filter { it.status == filter.status }
            }
        }
}
