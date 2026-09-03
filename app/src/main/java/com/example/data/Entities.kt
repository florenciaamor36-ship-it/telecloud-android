package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backup_settings")
data class BackupSettings(
    @PrimaryKey val id: Int = 1,
    val wifiOnly: Boolean = true,
    val batteryChargingOnly: Boolean = false,
    val activePhoneNumber: String? = null,
    val telegramChannelId: Long = 0L,
    val topicCameraId: Int = 0,
    val topicWhatsappPhotosId: Int = 0,
    val topicWhatsappDocsId: Int = 0,
    val isAutoBackupEnabled: Boolean = true
)

@Entity(tableName = "watched_folders")
data class WatchedFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val folderPath: String,
    val displayName: String,
    val appName: String,
    val iconType: String = "folder", // "camera", "whatsapp", "download", "document", "video", "music", "folder"
    val isEnabled: Boolean = true,
    val topicId: Int = 0,
    val isCustom: Boolean = false,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "upload_logs")
data class UploadLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val filePath: String,
    val fileName: String,
    val fileType: String, // "Camera", "WhatsApp Photos", "WhatsApp Docs", or folder name
    val status: String, // "PENDING", "UPLOADING", "COMPLETED", "FAILED"
    val timestamp: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val telegramMessageId: Long? = null,
    val errorMessage: String? = null
)

