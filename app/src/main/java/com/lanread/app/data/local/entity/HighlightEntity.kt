package com.lanread.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "highlights")
data class HighlightEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val selectedText: String,
    val startPosition: String,
    val endPosition: String,
    val chapter: String?,
    val pageNumber: Int,
    val colorHex: String,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)
