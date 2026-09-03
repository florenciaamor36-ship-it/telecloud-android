package com.example.data

enum class MediaType {
    IMAGE,
    VIDEO,
    DOCUMENT,
    AUDIO,
    TEMPORARY,
    HIDDEN,
    OTHER
}

data class GalleryMediaItem(
    val filePath: String,
    val fileName: String,
    val folderName: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mediaType: MediaType,
    val isBackedUp: Boolean,
    val isHidden: Boolean = false,
    val isTemporary: Boolean = false,
    val telegramMessageId: Long? = null,
    val backupStatus: String = "LOCAL" // "LOCAL", "PENDING", "UPLOADING", "BACKED_UP", "BACKED_UP_FREED"
)

data class CleanerCandidate(
    val log: UploadLog,
    val filePath: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val folderType: String,
    val isHidden: Boolean = false,
    val isTemporary: Boolean = false,
    val isSelected: Boolean = true
)

data class DeviceDirectoryNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val isHidden: Boolean,
    val isTemporary: Boolean,
    val sizeBytes: Long,
    val fileCount: Int = 0,
    val isWatched: Boolean = false
)
