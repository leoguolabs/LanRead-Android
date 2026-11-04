package com.lanread.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "book_mappings")
data class BookMappingEntity(
    @PrimaryKey val bookID: String,
    val notionPageID: String,
    val updatedAt: Long
)
