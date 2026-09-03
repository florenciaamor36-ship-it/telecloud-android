package com.example.observer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.data.BackupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class BackupService : Service() {

    private val TAG = "BackupService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val observers = mutableListOf<BackupFileObserver>()

    companion object {
        const val CHANNEL_ID = "telegram_backup_channel_v1"
        const val NOTIFICATION_ID = 4512
        const val ACTION_START = "ACTION_START_BACKUP"
        const val ACTION_STOP = "ACTION_STOP_BACKUP"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "Iniciando servicio de respaldo en primer plano...")
                startForegroundNotification()
                startFolderMonitoring()
            }
            ACTION_STOP -> {
                Log.d(TAG, "Deteniendo servicio de respaldo...")
                stopForeground(true)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nube de Respaldo de Telegram")
            .setContentText("Escuchando fotos y archivos en segundo plano en tiempo real")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Define las carpetas críticas de origen y las prepara para el monitoreo en tiempo real.
     * Crea los directorios si no existen para evitar fallos de inicialización.
     */
    private fun startFolderMonitoring() {
        // Limpiar observadores previos si existen
        stopFolderMonitoring()

        serviceScope.launch {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = BackupRepository(database.backupDao())
            val settings = repository.getSettings()

            if (!settings.isAutoBackupEnabled || settings.telegramChannelId == 0L) {
                Log.w(TAG, "Monitoreo cancelado: Autorespaldo desactivado o cuenta de Telegram desvinculada.")
                stopSelf()
                return@launch
            }

            // Obtener todas las carpetas activadas por el usuario desde la base de datos
            val enabledFolders = repository.getEnabledWatchedFolders()

            for (watched in enabledFolders) {
                val path = watched.folderPath.let { if (it.endsWith("/")) it else "$it/" }
                val type = watched.displayName
                val directory = File(path)
                try {
                    if (!directory.exists()) {
                        val created = directory.mkdirs()
                        Log.d(TAG, "Creando carpeta de origen $path: $created")
                    }

                    val observer = BackupFileObserver(applicationContext, path, type)
                    observer.startWatching()
                    observers.add(observer)
                    Log.i(TAG, "Monitoreando activamente [$type]: $path")

                } catch (e: Exception) {
                    Log.e(TAG, "Error al iniciar observador para $path: ${e.message}")
                }
            }
        }
    }

    private fun stopFolderMonitoring() {
        for (observer in observers) {
            observer.stopWatching()
        }
        observers.clear()
        Log.d(TAG, "Monitoreo de carpetas detenido.")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Canal de Respaldo de Telegram",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantiene activa la detección en tiempo real de nuevos archivos de WhatsApp y Cámara."
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    override fun onDestroy() {
        stopFolderMonitoring()
        serviceScope.cancel()
        Log.d(TAG, "Servicio destruido por completo.")
        super.onDestroy()
    }
}
