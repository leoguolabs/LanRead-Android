package com.lanread.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue_items")
data class SyncQueueItemEntity(
    @PrimaryKey val id: String,
    val targetBookId: String,
    val type: String,
    val payload: ByteArray,
    val status: String,
    val retryCount: Int,
    val createdAt: Long
) {
    override fun equals(other: Any?): Boolean = other is SyncQueueItemEntity && other.id == id
    override fun hashCode(): Int = id.hashCode()
}
