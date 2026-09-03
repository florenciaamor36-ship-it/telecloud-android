package com.example.data

import android.os.Environment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class BackupRepository(private val backupDao: BackupDao) {

    val settingsFlow: Flow<BackupSettings> = backupDao.getSettingsFlow().map {
        it ?: BackupSettings()
    }

    val uploadLogsFlow: Flow<List<UploadLog>> = backupDao.getUploadLogsFlow()
    val completedLogsFlow: Flow<List<UploadLog>> = backupDao.getCompletedLogsFlow()
    val watchedFoldersFlow: Flow<List<WatchedFolder>> = backupDao.getWatchedFoldersFlow()

    suspend fun getSettings(): BackupSettings {
        return backupDao.getSettings() ?: BackupSettings()
    }

    suspend fun updateSettings(settings: BackupSettings) {
        backupDao.insertSettings(settings)
    }

    suspend fun getLogByFilePath(filePath: String): UploadLog? {
        return backupDao.getLogByFilePath(filePath)
    }

    suspend fun logUploadStarted(filePath: String, fileName: String, fileType: String): Long {
        val file = File(filePath)
        val size = if (file.exists()) file.length() else 0L
        val existing = backupDao.getLogByFilePath(filePath)
        if (existing != null) {
            val updated = existing.copy(
                status = "UPLOADING",
                timestamp = System.currentTimeMillis(),
                fileSizeBytes = if (size > 0) size else existing.fileSizeBytes,
                errorMessage = null
            )
            backupDao.updateUploadLog(updated)
            return updated.id
        } else {
            val newLog = UploadLog(
                filePath = filePath,
                fileName = fileName,
                fileType = fileType,
                fileSizeBytes = size,
                status = "UPLOADING"
            )
            return backupDao.insertUploadLog(newLog)
        }
    }

    suspend fun logUploadCompleted(filePath: String, messageId: Long) {
        val file = File(filePath)
        val size = if (file.exists()) file.length() else 0L
        val existing = backupDao.getLogByFilePath(filePath)
        if (existing != null) {
            backupDao.updateUploadLog(
                existing.copy(
                    status = "COMPLETED",
                    fileSizeBytes = if (size > 0) size else existing.fileSizeBytes,
                    telegramMessageId = messageId,
                    errorMessage = null
                )
            )
        } else {
            backupDao.insertUploadLog(
                UploadLog(
                    filePath = filePath,
                    fileName = file.name,
                    fileType = "General",
                    fileSizeBytes = size,
                    status = "COMPLETED",
                    telegramMessageId = messageId
                )
            )
        }
    }

    suspend fun logUploadFailed(filePath: String, error: String) {
        val existing = backupDao.getLogByFilePath(filePath)
        if (existing != null) {
            backupDao.updateUploadLog(existing.copy(status = "FAILED", errorMessage = error))
        }
    }

    suspend fun clearLogs() {
        backupDao.clearLogs()
    }

    // ==========================================
    // GESTIÓN DE CARPETAS DEL TELÉFONO (WATCHED FOLDERS)
    // ==========================================

    suspend fun getWatchedFolders(): List<WatchedFolder> {
        var folders = backupDao.getWatchedFolders()
        if (folders.isEmpty()) {
            initDefaultFolders()
            folders = backupDao.getWatchedFolders()
        }
        return folders
    }

    suspend fun getEnabledWatchedFolders(): List<WatchedFolder> {
        val folders = backupDao.getEnabledWatchedFolders()
        if (folders.isEmpty()) {
            initDefaultFolders()
            return backupDao.getEnabledWatchedFolders()
        }
        return folders
    }

    suspend fun initDefaultFolders() {
        val existing = backupDao.getWatchedFolders()
        if (existing.isNotEmpty()) return

        val extStorage = Environment.getExternalStorageDirectory().absolutePath
        val defaults = listOf(
            WatchedFolder(
                folderPath = "$extStorage/DCIM/Camera",
                displayName = "Cámara (Fotos y Videos)",
                appName = "Cámara",
                iconType = "camera",
                isEnabled = true
            ),
            WatchedFolder(
                folderPath = "$extStorage/DCIM/Screenshots",
                displayName = "Capturas de Pantalla",
                appName = "Sistema",
                iconType = "image",
                isEnabled = true
            ),
            WatchedFolder(
                folderPath = "$extStorage/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images",
                displayName = "WhatsApp Fotos",
                appName = "WhatsApp",
                iconType = "whatsapp",
                isEnabled = true
            ),
            WatchedFolder(
                folderPath = "$extStorage/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video",
                displayName = "WhatsApp Videos",
                appName = "WhatsApp",
                iconType = "video",
                isEnabled = true
            ),
            WatchedFolder(
                folderPath = "$extStorage/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents",
                displayName = "WhatsApp Documentos",
                appName = "WhatsApp",
                iconType = "document",
                isEnabled = true
            ),
            WatchedFolder(
                folderPath = "$extStorage/Download",
                displayName = "Descargas (Todos los archivos)",
                appName = "Descargas",
                iconType = "download",
                isEnabled = false
            ),
            WatchedFolder(
                folderPath = "$extStorage/Pictures",
                displayName = "Imágenes (Otras Apps)",
                appName = "Galería",
                iconType = "image",
                isEnabled = false
            ),
            WatchedFolder(
                folderPath = "$extStorage/Telegram",
                displayName = "Archivos de Telegram",
                appName = "Telegram",
                iconType = "folder",
                isEnabled = false
            )
        )
        backupDao.insertWatchedFolders(defaults)
    }

    suspend fun toggleFolderEnabled(folderId: Long, isEnabled: Boolean) {
        val folders = backupDao.getWatchedFolders()
        val target = folders.find { it.id == folderId } ?: return
        backupDao.updateWatchedFolder(target.copy(isEnabled = isEnabled))
    }

    suspend fun isPathWatched(path: String): Boolean {
        val cleanPath = path.trim().removeSuffix("/")
        return backupDao.getWatchedFolderByPath(cleanPath)?.isEnabled == true
    }

    suspend fun toggleWatchPath(path: String): Boolean {
        val cleanPath = path.trim().removeSuffix("/")
        val existing = backupDao.getWatchedFolderByPath(cleanPath)
        return if (existing != null) {
            val newState = !existing.isEnabled
            backupDao.updateWatchedFolder(existing.copy(isEnabled = newState))
            newState
        } else {
            val folderName = File(cleanPath).name.ifBlank { "Almacenamiento" }
            val newFolder = WatchedFolder(
                folderPath = cleanPath,
                displayName = folderName,
                appName = "Almacenamiento",
                iconType = "folder",
                isEnabled = true,
                isCustom = true
            )
            backupDao.insertWatchedFolder(newFolder)
            true
        }
    }

    suspend fun addCustomFolder(folderPath: String, displayName: String, appName: String): Boolean {
        val cleanPath = folderPath.trim().removeSuffix("/")
        val existing = backupDao.getWatchedFolderByPath(cleanPath)
        if (existing != null) {
            backupDao.updateWatchedFolder(existing.copy(isEnabled = true, displayName = displayName))
            return true
        }
        val newFolder = WatchedFolder(
            folderPath = cleanPath,
            displayName = displayName.ifBlank { File(cleanPath).name },
            appName = appName.ifBlank { "Personalizada" },
            iconType = "folder",
            isEnabled = true,
            isCustom = true
        )
        backupDao.insertWatchedFolder(newFolder)
        return true
    }

    suspend fun deleteWatchedFolder(folderId: Long) {
        backupDao.deleteWatchedFolderById(folderId)
    }

    // ==========================================
    // LIMPIEZA INTELIGENTE DEL TELÉFONO (CLEANER)
    // ==========================================

    suspend fun getCompletedLogs(): List<UploadLog> {
        return backupDao.getCompletedLogs()
    }

    suspend fun deleteLocalFileFromPhone(log: UploadLog): Boolean {
        return try {
            val file = File(log.filePath)
            if (file.exists()) {
                val deleted = file.delete()
                deleted
            } else {
                true // Ya no existe localmente
            }
        } catch (e: Exception) {
            false
        }
    }
}

