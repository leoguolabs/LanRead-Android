package com.lanread.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val currentPage: Int,
    val currentChapter: String?,
    val currentPosition: String?,
    val progressPercentage: Double,
    val lastReadAt: Long,
    val totalReadingTime: Long,
    val detailedReadingTime: Long,
    val skimmingReadingTime: Long,
    val createdAt: Long,
    val updatedAt: Long
)
