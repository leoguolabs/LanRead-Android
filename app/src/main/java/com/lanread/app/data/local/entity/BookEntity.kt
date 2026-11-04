package com.lanread.app.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String?,
    val language: String?,
    val coverImageData: ByteArray?,
    val filePath: String,
    val fileFormat: String,
    val fileSize: Long,
    val checksum: String,
    val totalPages: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val metadata: String?,
    val aiSummary: String?,
    val aiKeyPoints: String?,
    @ColumnInfo(name = "aiSummaryGeneratedAt") val aiSummaryGeneratedAt: Long?,
    val skimmingSummaries: String?,
    val notionPageId: String?
) {
    override fun equals(other: Any?): Boolean = other is BookEntity && other.id == id
    override fun hashCode(): Int = id.hashCode()
}
