package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_entries")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val operationName: String,
    val operationType: String,
    val originalFileName: String,
    val outputFileName: String,
    val filePath: String,
    val uriString: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val pageCount: Int = 1,
    val status: String = "Completed",
    val isFavorite: Boolean = false
)
