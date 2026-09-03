package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import com.example.data.BackupRepository
import com.example.tdlib.TdLibManager

class BackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val TAG = "BackupWorker"

    override suspend fun doWork(): Result {
        val filePath = inputData.getString("FILE_PATH") ?: return Result.failure()
        val folderType = inputData.getString("FOLDER_TYPE") ?: return Result.failure()

        Log.d(TAG, "Iniciando subida en segundo plano para: $filePath (Tipo: $folderType)")

        // Inicializar repositorios
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BackupRepository(database.backupDao())
        
        // Obtener configuraciones de respaldo actuales
        val settings = repository.getSettings()
        if (settings.telegramChannelId == 0L || settings.activePhoneNumber == null) {
            Log.e(TAG, "Fallo: Telegram no está configurado o el usuario no está logueado.")
            repository.logUploadFailed(filePath, "TDLib no autenticado o canal de respaldo no creado")
            return Result.failure()
        }

        // Obtener la instancia del gestor de TDLib
        val tdLibManager = TdLibManager(applicationContext)

        // Mapear el tipo de carpeta detectada al ID de hilo (Topic ID) de Telegram
        val topicId: Int = when {
            folderType.contains("Camera", ignoreCase = true) || folderType.contains("Cámara", ignoreCase = true) -> settings.topicCameraId
            folderType.contains("Photo", ignoreCase = true) || folderType.contains("Foto", ignoreCase = true) || folderType.contains("Imagen", ignoreCase = true) -> settings.topicWhatsappPhotosId
            folderType.contains("Doc", ignoreCase = true) || folderType.contains("Descarga", ignoreCase = true) || folderType.contains("Download", ignoreCase = true) -> settings.topicWhatsappDocsId
            folderType.contains("Video", ignoreCase = true) -> settings.topicCameraId
            else -> settings.topicWhatsappDocsId
        }

        val targetTopicId = if (topicId != 0) topicId else 101

        // Ejecutar la subida real del archivo utilizando el gestor de TDLib
        val uploadSuccess = tdLibManager.uploadFileToTopic(filePath, targetTopicId, repository)

        return if (uploadSuccess) {
            Log.i(TAG, "Subida asíncrona completada con éxito por WorkManager para: $filePath")
            Result.success()
        } else {
            Log.w(TAG, "Subida fallida para $filePath. Reintentando de acuerdo a la política de WorkManager.")
            // Retornar retry instruye a WorkManager a ejecutar de nuevo respetando el retroceso exponencial
            Result.retry()
        }
    }
}
