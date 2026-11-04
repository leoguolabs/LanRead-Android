package com.lanread.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "library_items")
data class LibraryItemEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val statusRaw: String,
    val tags: String?,
    val isFavorite: Boolean,
    val rating: Int,
    val addedAt: Long,
    val lastAccessedAt: Long?,
    val sortOrder: Int
)
