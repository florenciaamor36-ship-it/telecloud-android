package com.example.observer

import android.content.Context
import android.os.Build
import android.os.FileObserver
import android.util.Log
import androidx.work.*
import com.example.data.AppDatabase
import com.example.data.BackupRepository
import com.example.worker.BackupWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class BackupFileObserver(
    private val context: Context,
    private val folderPath: String,
    private val folderType: String // "Camera", "WhatsApp Photos", "WhatsApp Docs"
) : FileObserver(
    File(folderPath),
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        CLOSE_WRITE or CREATE
    } else {
        CLOSE_WRITE
    }
) {

    private val TAG = "BackupFileObserver"
    private val scope = CoroutineScope(Dispatchers.IO)
    private val repository: BackupRepository

    init {
        val database = AppDatabase.getDatabase(context)
        repository = BackupRepository(database.backupDao())
    }

    override fun onEvent(event: Int, path: String?) {
        if (path == null) return

        // Nos interesa cuando un archivo se termina de escribir o es creado
        if ((event and CLOSE_WRITE != 0) || (event and CREATE != 0)) {
            val absolutePath = "${folderPath.removeSuffix("/")}/$path"
            val file = File(absolutePath)

            // Ignorar directorios temporales o archivos vacíos
            if (file.isDirectory || file.name.startsWith(".")) {
                return
            }

            Log.i(TAG, "Nuevo archivo detectado en [$folderType]: $absolutePath (Event ID: $event)")

            // Programar subida en segundo plano de forma asíncrona
            scope.launch {
                // Registrar archivo en la base de datos local en estado PENDING
                repository.logUploadStarted(
                    filePath = absolutePath,
                    fileName = file.name,
                    fileType = folderType
                )

                // Encolar trabajo en WorkManager
                enqueueBackupWork(absolutePath, folderType)
            }
        }
    }

    /**
     * Encola la tarea de subida asíncrona en WorkManager aplicando las restricciones
     * de red (Wi-Fi o Datos) y estado de batería configurados por el usuario.
     */
    private suspend fun enqueueBackupWork(filePath: String, folderType: String) {
        val settings = repository.getSettings()
        
        // Determinar las restricciones de red
        val networkConstraint = if (settings.wifiOnly) {
            NetworkType.UNMETERED // Requiere Wi-Fi
        } else {
            NetworkType.CONNECTED // Cualquier red (Wi-Fi o datos móviles)
        }

        // Crear restricciones de WorkManager
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkConstraint)
            .setRequiresBatteryNotLow(true) // No subir si la batería está baja
            .setRequiresCharging(settings.batteryChargingOnly) // Restricción configurable opcional
            .build()

        // Pasar parámetros al Worker
        val inputData = Data.Builder()
            .putString("FILE_PATH", filePath)
            .putString("FOLDER_TYPE", folderType)
            .build()

        // Configurar una política de reintento exponencial con WorkManager
        val uploadWorkRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                WorkRequest.MIN_BACKOFF_MILLIS,
                java.util.concurrent.TimeUnit.MILLISECONDS
            )
            .addTag("BackupUploadJob")
            .build()

        // Programar el trabajo de forma segura
        WorkManager.getInstance(context).enqueueUniqueWork(
            "Upload_${filePath.hashCode()}",
            ExistingWorkPolicy.REPLACE, // Reemplazar en caso de que ya se haya registrado un intento para este archivo
            uploadWorkRequest
        )

        Log.d(TAG, "Tarea de WorkManager programada con éxito para: $filePath (Red: ${if(settings.wifiOnly) "Wi-Fi" else "Cualquiera"})")
    }
}
