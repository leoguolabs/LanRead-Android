package com.lanread.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notion_sync_config")
data class NotionSyncConfigEntity(
    @PrimaryKey val id: Int = 1,
    val databaseId: String,
    val containerPageId: String,
    val workspaceName: String,
    val lastSyncedAt: Long?
)
