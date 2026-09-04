package com.example.ui

import android.app.Application
import android.content.Intent
import android.content.ContentUris
import android.provider.MediaStore
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.observer.BackupService
import com.example.tdlib.TdApi
import com.example.tdlib.TdLibManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "BackupViewModel"
    val repository: BackupRepository
    val tdLibManager: TdLibManager

    val settingsState: StateFlow<BackupSettings>
    val uploadLogsState: StateFlow<List<UploadLog>>
    val watchedFoldersState: StateFlow<List<WatchedFolder>>
    val authState: StateFlow<TdApi.AuthorizationState>
    val isConnected: StateFlow<Boolean>

    // Galería & Medios del Dispositivo (Todos los archivos reales)
    private val _galleryItems = MutableStateFlow<List<GalleryMediaItem>>(emptyList())
    val galleryItemsState: StateFlow<List<GalleryMediaItem>> = _galleryItems.asStateFlow()

    // Mostrar u ocultar archivos ocultos y temporales en la galería
    private val _showHiddenAndTemp = MutableStateFlow(true)
    val showHiddenAndTemp: StateFlow<Boolean> = _showHiddenAndTemp.asStateFlow()

    // Explorador de Todas las Carpetas del Teléfono (File System Browser)
    private val _currentBrowsingPath = MutableStateFlow<String>(
        Environment.getExternalStorageDirectory()?.absolutePath ?: "/storage/emulated/0"
    )
    val currentBrowsingPath: StateFlow<String> = _currentBrowsingPath.asStateFlow()

    private val _directoryNodes = MutableStateFlow<List<DeviceDirectoryNode>>(emptyList())
    val directoryNodesState: StateFlow<List<DeviceDirectoryNode>> = _directoryNodes.asStateFlow()

    private val _isBrowsingLoading = MutableStateFlow(false)
    val isBrowsingLoading: StateFlow<Boolean> = _isBrowsingLoading.asStateFlow()

    // Candidatos para Liberar Espacio (Limpiador Real de Almacenamiento)
    private val _cleanerCandidates = MutableStateFlow<List<CleanerCandidate>>(emptyList())
    val cleanerCandidatesState: StateFlow<List<CleanerCandidate>> = _cleanerCandidates.asStateFlow()

    // Espacio liberado real acumulado en bytes
    private val _freedSpaceBytes = MutableStateFlow(0L)
    val freedSpaceBytes: StateFlow<Long> = _freedSpaceBytes.asStateFlow()

    private val _isCleaning = MutableStateFlow(false)
    val isCleaning: StateFlow<Boolean> = _isCleaning.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BackupRepository(database.backupDao())
        tdLibManager = TdLibManager(application)

        settingsState = repository.settingsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BackupSettings()
        )

        uploadLogsState = repository.uploadLogsFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        watchedFoldersState = repository.watchedFoldersFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        authState = tdLibManager.authState
        isConnected = tdLibManager.isConnected

        viewModelScope.launch {
            repository.initDefaultFolders()
            loadDirectory(_currentBrowsingPath.value)
            refreshGalleryAndCleaner()

            val settings = repository.getSettings()
            if (settings.isAutoBackupEnabled && settings.telegramChannelId != 0L) {
                startBackupService()
            }
        }

        viewModelScope.launch {
            repository.uploadLogsFlow.collect {
                refreshGalleryAndCleaner()
            }
        }
    }

    // ==========================================
    // AUTENTICACIÓN TELEGRAM NATIVA
    // ==========================================

    fun setPhoneNumber(phone: String) {
        tdLibManager.setPhoneNumber(phone)
    }

    fun submitCode(code: String) {
        tdLibManager.checkVerificationCode(code, repository)
    }

    fun logout() {
        tdLibManager.logout(repository)
        stopBackupService()
    }

    // ==========================================
    // CONFIGURACIONES DE RESPALDO Y SERVICIO
    // ==========================================

    fun updateSettings(settings: BackupSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            if (settings.isAutoBackupEnabled) {
                startBackupService()
            } else {
                stopBackupService()
            }
        }
    }

    fun startBackupService() {
        try {
            val intent = Intent(getApplication(), BackupService::class.java).apply {
                action = BackupService.ACTION_START
            }
            getApplication<Application>().startForegroundService(intent)
            Log.d(TAG, "Servicio en primer plano de respaldo iniciado")
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo iniciar el servicio de respaldo: ${e.message}")
        }
    }

    fun stopBackupService() {
        try {
            val intent = Intent(getApplication(), BackupService::class.java).apply {
                action = BackupService.ACTION_STOP
            }
            getApplication<Application>().startService(intent)
            Log.d(TAG, "Servicio de respaldo detenido")
        } catch (e: Exception) {
            Log.e(TAG, "Error al detener el servicio: ${e.message}")
        }
    }

    // ==========================================
    // EXPLORADOR DE TODAS LAS CARPETAS DEL TELÉFONO
    // ==========================================

    fun browseTo(path: String) {
        _currentBrowsingPath.value = path
        loadDirectory(path)
    }

    fun browseUp() {
        val currentFile = File(_currentBrowsingPath.value)
        val parent = currentFile.parentFile
        if (parent != null && parent.canRead()) {
            browseTo(parent.absolutePath)
        }
    }

    fun toggleShowHiddenAndTemp() {
        _showHiddenAndTemp.value = !_showHiddenAndTemp.value
        loadDirectory(_currentBrowsingPath.value)
        refreshGalleryAndCleaner()
    }

    fun loadDirectory(path: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isBrowsingLoading.value = true
            val targetDir = File(path)
            val nodes = mutableListOf<DeviceDirectoryNode>()

            val watchedPaths = repository.getWatchedFolders().associateBy { it.folderPath }

            if (targetDir.exists() && targetDir.isDirectory && targetDir.canRead()) {
                val files = targetDir.listFiles() ?: emptyArray()
                for (file in files) {
                    val isHidden = file.name.startsWith(".")
                    val ext = file.extension.lowercase()
                    val isTemporary = ext in listOf("tmp", "temp", "cache", "log", "bak", "dmp") ||
                            file.name.contains("cache", ignoreCase = true) ||
                            file.name.contains("temp", ignoreCase = true)

                    val isDir = file.isDirectory
                    val childCount = if (isDir) (file.list()?.size ?: 0) else 0
                    val size = if (isDir) 0L else file.length()
                    val isWatched = watchedPaths.containsKey(file.absolutePath) && watchedPaths[file.absolutePath]?.isEnabled == true

                    nodes.add(
                        DeviceDirectoryNode(
                            path = file.absolutePath,
                            name = file.name,
                            isDirectory = isDir,
                            isHidden = isHidden,
                            isTemporary = isTemporary,
                            sizeBytes = size,
                            fileCount = childCount,
                            isWatched = isWatched
                        )
                    )
                }
            }

            // Ordenar: Carpetas primero, luego archivos, alfabéticamente
            nodes.sortWith(compareByDescending<DeviceDirectoryNode> { it.isDirectory }.thenBy { it.name.lowercase() })
            _directoryNodes.value = nodes
            _isBrowsingLoading.value = false
        }
    }

    fun toggleWatchFolderFromBrowser(path: String) {
        viewModelScope.launch {
            repository.toggleWatchPath(path)
            loadDirectory(_currentBrowsingPath.value)
            refreshGalleryAndCleaner()
            if (settingsState.value.isAutoBackupEnabled) {
                startBackupService()
            }
        }
    }

    fun toggleFolderEnabled(folderId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.toggleFolderEnabled(folderId, isEnabled)
            loadDirectory(_currentBrowsingPath.value)
            refreshGalleryAndCleaner()
            if (settingsState.value.isAutoBackupEnabled) {
                startBackupService()
            }
        }
    }

    fun addCustomFolder(folderPath: String, displayName: String) {
        viewModelScope.launch {
            repository.addCustomFolder(folderPath, displayName, "Almacenamiento")
            loadDirectory(_currentBrowsingPath.value)
            refreshGalleryAndCleaner()
            if (settingsState.value.isAutoBackupEnabled) {
                startBackupService()
            }
        }
    }

    fun deleteWatchedFolder(folderId: Long) {
        viewModelScope.launch {
            repository.deleteWatchedFolder(folderId)
            loadDirectory(_currentBrowsingPath.value)
            refreshGalleryAndCleaner()
            if (settingsState.value.isAutoBackupEnabled) {
                startBackupService()
            }
        }
    }

    // ==========================================
    // ESCANEO REAL DE GALERÍA Y LIMPIADOR
    // ==========================================

    fun refreshGalleryAndCleaner() {
        viewModelScope.launch(Dispatchers.IO) {
            val watchedFolders = repository.getWatchedFolders()
            val logs = repository.uploadLogsFlow.first()
            val logMap = logs.associateBy { it.filePath }

            val items = mutableListOf<GalleryMediaItem>()
            val visitedPaths = mutableSetOf<String>()

            // 1. Escanear carpetas observadas activas
            for (folder in watchedFolders.filter { it.isEnabled }) {
                val dir = File(folder.folderPath)
                if (dir.exists() && dir.isDirectory) {
                    scanDirectoryFiles(dir, folder.displayName, items, visitedPaths, logMap, depth = 0, maxDepth = 2)
                }
            }

            // 2. Escanear ubicaciones estándar del almacenamiento del teléfono si están vacías o para asegurar cobertura
            val standardDirs = listOf(
                Pair(File(Environment.getExternalStorageDirectory(), "DCIM"), "Cámara y Fotos"),
                Pair(File(Environment.getExternalStorageDirectory(), "Pictures"), "Imágenes"),
                Pair(File(Environment.getExternalStorageDirectory(), "Download"), "Descargas"),
                Pair(File(Environment.getExternalStorageDirectory(), "Documents"), "Documentos"),
                Pair(File(Environment.getExternalStorageDirectory(), "Music"), "Música"),
                Pair(File(Environment.getExternalStorageDirectory(), "Movies"), "Videos"),
                Pair(File(Environment.getExternalStorageDirectory(), ".thumbnails"), "Miniaturas Ocultas"),
                Pair(getApplication<Application>().cacheDir, "Caché Temporal"),
                Pair(getApplication<Application>().filesDir, "Archivos de App")
            )

            for ((dir, label) in standardDirs) {
                if (dir.exists() && dir.isDirectory) {
                    scanDirectoryFiles(dir, label, items, visitedPaths, logMap, depth = 0, maxDepth = 2)
                }
            }


            // En Android 10+ MediaStore es la fuente fiable y entrega URIs
            // que permiten mostrar miniaturas aunque el sistema limite File().
            scanMediaStore(items, visitedPaths, logMap)

            // Filtrar ocultos y temporales si el usuario lo desactiva
            val finalItems = if (_showHiddenAndTemp.value) {
                items
            } else {
                items.filter { !it.isHidden && !it.isTemporary }
            }

            val sortedItems = finalItems.sortedByDescending { it.lastModified }
            _galleryItems.value = sortedItems

            // 3. Generar candidatos reales para el limpiador:
            // Archivos respaldados en Telegram que todavía existen en el teléfono, y archivos temporales/caché purificables
            val candidates = mutableListOf<CleanerCandidate>()
            for (item in sortedItems) {
                val file = File(item.filePath)
                val log = logMap[item.filePath]
                val existsLocally = file.exists()

                if (existsLocally && (item.isBackedUp || item.isTemporary)) {
                    candidates.add(
                        CleanerCandidate(
                            log = log ?: UploadLog(
                                filePath = item.filePath,
                                fileName = item.fileName,
                                fileType = item.folderName,
                                fileSizeBytes = item.sizeBytes,
                                status = if (item.isBackedUp) "COMPLETED" else "TEMPORARY",
                                telegramMessageId = item.telegramMessageId
                            ),
                            filePath = item.filePath,
                            fileName = item.fileName,
                            fileSizeBytes = item.sizeBytes,
                            folderType = item.folderName,
                            isHidden = item.isHidden,
                            isTemporary = item.isTemporary,
                            isSelected = true
                        )
                    )
                }
            }
            _cleanerCandidates.value = candidates
        }
    }

    private fun scanMediaStore(
        items: MutableList<GalleryMediaItem>,
        visitedPaths: MutableSet<String>,
        logMap: Map<String, UploadLog>
    ) {
        val resolver = getApplication<Application>().contentResolver
        val sources = listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to MediaType.IMAGE,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to MediaType.VIDEO
        )
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        for ((collection, type) in sources) {
            runCatching {
                resolver.query(collection, projection, null, null,
                    "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")?.use { cursor ->
                    val id = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val name = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                    val data = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                    val size = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                    val modified = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                    val relative = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                    while (cursor.moveToNext()) {
                        val uri = ContentUris.withAppendedId(collection, cursor.getLong(id))
                        val path = if (data >= 0) cursor.getString(data).orEmpty() else ""
                        val key = path.ifBlank { uri.toString() }
                        if (!visitedPaths.add(key)) continue
                        val fileName = cursor.getString(name) ?: "Sin nombre"
                        val log = logMap[path]
                        items.add(GalleryMediaItem(
                            filePath = key,
                            fileName = fileName,
                            folderName = if (relative >= 0) cursor.getString(relative) ?: "Galería" else "Galería",
                            sizeBytes = if (size >= 0) cursor.getLong(size) else 0L,
                            lastModified = if (modified >= 0) cursor.getLong(modified) * 1000L else 0L,
                            mediaType = type,
                            isBackedUp = log?.status == "COMPLETED",
                            telegramMessageId = log?.telegramMessageId,
                            backupStatus = log?.status ?: "LOCAL",
                            contentUri = uri.toString()
                        ))
                    }
                }
            }.onFailure { Log.w(TAG, "No se pudo consultar MediaStore", it) }
        }
    }

    private fun scanDirectoryFiles(
        directory: File,
        folderName: String,
        items: MutableList<GalleryMediaItem>,
        visitedPaths: MutableSet<String>,
        logMap: Map<String, UploadLog>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return
        val files = directory.listFiles() ?: return

        for (file in files) {
            if (file.isDirectory) {
                // Subdirectorio (incluye carpetas ocultas y temporales como .thumbnails o cache)
                scanDirectoryFiles(file, folderName, items, visitedPaths, logMap, depth + 1, maxDepth)
            } else if (file.isFile && visitedPaths.add(file.absolutePath)) {
                val fileName = file.name
                val isHidden = fileName.startsWith(".") || file.parentFile?.name?.startsWith(".") == true
                val ext = file.extension.lowercase()
                val isTemporary = ext in listOf("tmp", "temp", "cache", "log", "bak", "dmp") ||
                        fileName.contains("temp", ignoreCase = true) ||
                        fileName.contains("cache", ignoreCase = true) ||
                        file.parentFile?.name?.contains("cache", ignoreCase = true) == true

                val log = logMap[file.absolutePath]
                val isBackedUp = log?.status == "COMPLETED"
                val status = when {
                    isBackedUp -> "BACKED_UP"
                    log?.status == "UPLOADING" -> "UPLOADING"
                    log?.status == "PENDING" -> "PENDING"
                    else -> "LOCAL"
                }

                val mediaType = determineMediaType(fileName, isHidden, isTemporary)

                items.add(
                    GalleryMediaItem(
                        filePath = file.absolutePath,
                        fileName = fileName,
                        folderName = folderName,
                        sizeBytes = file.length(),
                        lastModified = file.lastModified(),
                        mediaType = mediaType,
                        isBackedUp = isBackedUp,
                        isHidden = isHidden,
                        isTemporary = isTemporary,
                        telegramMessageId = log?.telegramMessageId,
                        backupStatus = status
                    )
                )
            }
        }
    }

    private fun determineMediaType(fileName: String, isHidden: Boolean, isTemporary: Boolean): MediaType {
        if (isTemporary) return MediaType.TEMPORARY
        if (isHidden) return MediaType.HIDDEN

        val ext = fileName.substringAfterLast(".", "").lowercase()
        return when (ext) {
            "jpg", "jpeg", "png", "webp", "gif", "heic", "svg", "bmp" -> MediaType.IMAGE
            "mp4", "mkv", "mov", "avi", "webm", "3gp", "ts" -> MediaType.VIDEO
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "zip", "rar", "apk" -> MediaType.DOCUMENT
            "mp3", "m4a", "wav", "ogg", "flac", "aac", "opus" -> MediaType.AUDIO
            else -> MediaType.OTHER
        }
    }

    // ==========================================
    // ACCIONES DE RESPALDO MANUAL A TELEGRAM
    // ==========================================

    fun backupItemNow(item: GalleryMediaItem) {
        viewModelScope.launch {
            repository.logUploadStarted(item.filePath, item.fileName, item.folderName)
            tdLibManager.ensureBackupChannelAndTopics(repository)
            val settings = repository.getSettings()
            val topicId = when {
                item.mediaType == MediaType.IMAGE || item.mediaType == MediaType.VIDEO ->
                    if (settings.topicCameraId != 0) settings.topicCameraId else 101
                else ->
                    if (settings.topicWhatsappDocsId != 0) settings.topicWhatsappDocsId else 103
            }
            tdLibManager.uploadFileToTopic(item.filePath, topicId, repository)
            refreshGalleryAndCleaner()
        }
    }

    // ==========================================
    // LIMPIADOR REAL: LIBERAR ESPACIO DEL TELÉFONO
    // ==========================================

    fun toggleCleanerCandidate(filePath: String) {
        _cleanerCandidates.value = _cleanerCandidates.value.map {
            if (it.filePath == filePath) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAllCleanerCandidates(select: Boolean) {
        _cleanerCandidates.value = _cleanerCandidates.value.map {
            it.copy(isSelected = select)
        }
    }

    fun cleanSelectedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            _isCleaning.value = true
            val candidatesToClean = _cleanerCandidates.value.filter { it.isSelected }
            var totalFreed = 0L

            for (candidate in candidatesToClean) {
                val file = File(candidate.filePath)
                if (file.exists()) {
                    val size = file.length()
                    val deleted = file.delete()
                    if (deleted) {
                        totalFreed += size
                    }
                }
            }

            _freedSpaceBytes.value += totalFreed

            // Actualizar candidatos restantes y recargar la galería
            _cleanerCandidates.value = _cleanerCandidates.value.filter { !it.isSelected }
            _isCleaning.value = false
            refreshGalleryAndCleaner()
            loadDirectory(_currentBrowsingPath.value)
        }
    }
}
