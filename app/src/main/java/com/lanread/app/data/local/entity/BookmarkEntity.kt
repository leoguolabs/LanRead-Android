package com.lanread.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val pageIndex: Int,
    val chapterTitle: String?,
    val createdAt: Long,
    val colorHex: String?
)
