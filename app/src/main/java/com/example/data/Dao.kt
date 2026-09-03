package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BackupDao {
    @Query("SELECT * FROM backup_settings WHERE id = 1 LIMIT 1")
    fun getSettingsFlow(): Flow<BackupSettings?>

    @Query("SELECT * FROM backup_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): BackupSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: BackupSettings)

    @Query("SELECT * FROM upload_logs ORDER BY timestamp DESC")
    fun getUploadLogsFlow(): Flow<List<UploadLog>>

    @Query("SELECT * FROM upload_logs WHERE filePath = :filePath LIMIT 1")
    suspend fun getLogByFilePath(filePath: String): UploadLog?

    @Query("SELECT * FROM upload_logs WHERE status = 'COMPLETED'")
    suspend fun getCompletedLogs(): List<UploadLog>

    @Query("SELECT * FROM upload_logs WHERE status = 'COMPLETED'")
    fun getCompletedLogsFlow(): Flow<List<UploadLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUploadLog(log: UploadLog): Long

    @Update
    suspend fun updateUploadLog(log: UploadLog)

    @Delete
    suspend fun deleteUploadLog(log: UploadLog)

    @Query("DELETE FROM upload_logs WHERE filePath = :filePath")
    suspend fun deleteLogByFilePath(filePath: String)

    @Query("DELETE FROM upload_logs")
    suspend fun clearLogs()

    // Operaciones con carpetas observadas (WatchedFolder)
    @Query("SELECT * FROM watched_folders ORDER BY dateAdded ASC")
    fun getWatchedFoldersFlow(): Flow<List<WatchedFolder>>

    @Query("SELECT * FROM watched_folders ORDER BY dateAdded ASC")
    suspend fun getWatchedFolders(): List<WatchedFolder>

    @Query("SELECT * FROM watched_folders WHERE isEnabled = 1")
    suspend fun getEnabledWatchedFolders(): List<WatchedFolder>

    @Query("SELECT * FROM watched_folders WHERE folderPath = :path LIMIT 1")
    suspend fun getWatchedFolderByPath(path: String): WatchedFolder?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatchedFolder(folder: WatchedFolder): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWatchedFolders(folders: List<WatchedFolder>)

    @Update
    suspend fun updateWatchedFolder(folder: WatchedFolder)

    @Delete
    suspend fun deleteWatchedFolder(folder: WatchedFolder)

    @Query("DELETE FROM watched_folders WHERE id = :id")
    suspend fun deleteWatchedFolderById(id: Long)
}
