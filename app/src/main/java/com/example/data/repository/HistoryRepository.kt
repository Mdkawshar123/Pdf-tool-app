package com.example.data.repository

import com.example.data.local.HistoryDao
import com.example.data.local.HistoryEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class HistoryRepository(private val dao: HistoryDao) {

    val allHistory: Flow<List<HistoryEntity>> = dao.getAllHistory()
    val recentFiles: Flow<List<HistoryEntity>> = dao.getRecentFiles()
    val favorites: Flow<List<HistoryEntity>> = dao.getFavorites()

    suspend fun recordOperation(
        operationName: String,
        operationType: String,
        originalFileName: String,
        outputFileName: String,
        filePath: String,
        uriString: String = "",
        fileSizeBytes: Long = 0L,
        pageCount: Int = 1,
        status: String = "Completed"
    ): Long {
        val entity = HistoryEntity(
            operationName = operationName,
            operationType = operationType,
            originalFileName = originalFileName,
            outputFileName = outputFileName,
            filePath = filePath,
            uriString = uriString,
            timestamp = System.currentTimeMillis(),
            fileSizeBytes = fileSizeBytes,
            pageCount = pageCount,
            status = status,
            isFavorite = false
        )
        return dao.insert(entity)
    }

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        dao.setFavorite(id, !currentStatus)
    }

    suspend fun rename(id: Long, newName: String, newPath: String) {
        dao.rename(id, newName, newPath)
    }

    suspend fun delete(id: Long, deleteFile: Boolean = true) {
        if (deleteFile) {
            val item = dao.getById(id)
            item?.filePath?.let { path ->
                val f = File(path)
                if (f.exists()) f.delete()
            }
        }
        dao.deleteById(id)
    }

    suspend fun clearAll() {
        dao.clearAll()
    }
}
