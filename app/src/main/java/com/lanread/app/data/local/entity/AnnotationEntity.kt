package com.lanread.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey val id: String,
    val bookId: String,
    val content: String,
    val position: String,
    val chapter: String?,
    val pageNumber: Int,
    val associatedText: String?,
    val createdAt: Long,
    val updatedAt: Long
)
