package com.example.tdlib

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.BackupRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import java.io.File

/**
 * Gestor de cliente TDLib (Telegram Database Library) para Android.
 * Proporciona autenticación nativa, gestión de canales con hilos (Forum Topics)
 * y subida concurrente y resiliente de archivos a la nube de Telegram.
 */
class TdLibManager(private val context: Context) {

    private val TAG = "TdLibManager"
    private val scope = CoroutineScope(Dispatchers.IO)

    // Estados de autenticación observables
    private val _authState = MutableStateFlow<TdApi.AuthorizationState>(TdApi.AuthorizationStateWaitPhoneNumber())
    val authState: StateFlow<TdApi.AuthorizationState> = _authState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private var activePhoneNumber: String = ""
    private var nativeClient: TdLibNativeClient? = null
    @Volatile private var phoneRequestInFlight = false

    // Parámetros reales de Telegram. La sesión TDLib todavía requiere enlazar la biblioteca JNI.
    private val telegramParameters = TdApi.TdlibParameters(
        apiId = BuildConfig.TELEGRAM_API_ID,
        apiHash = BuildConfig.TELEGRAM_API_HASH,
        databaseDirectory = File(context.filesDir, "telegram-db").absolutePath,
        filesDirectory = File(context.filesDir, "telegram-files").absolutePath,
        applicationVersion = BuildConfig.VERSION_NAME
    )

    init {
        nativeClient = TdLibNativeClient(
            context = context,
            onAuthorizationState = { state ->
                when (state) {
                    is org.drinkless.tdlib.TdApi.AuthorizationStateWaitPhoneNumber -> _authState.value = TdApi.AuthorizationStateWaitPhoneNumber()
                    is org.drinkless.tdlib.TdApi.AuthorizationStateWaitCode -> {
                        phoneRequestInFlight = false
                        _authState.value = TdApi.AuthorizationStateWaitCode()
                    }
                    is org.drinkless.tdlib.TdApi.AuthorizationStateWaitPassword -> _authState.value = TdApi.AuthorizationStateWaitPassword()
                    is org.drinkless.tdlib.TdApi.AuthorizationStateReady -> {
                        _authState.value = TdApi.AuthorizationStateReady()
                        _isConnected.value = true
                    }
                    else -> Log.d(TAG, "TDLib authorization state: ${state.javaClass.simpleName}")
                }
            },
            onSelfUser = { userId ->
                scope.launch { updateSavedMessagesDestination(userId) }
            },
            onError = { error ->
                phoneRequestInFlight = false
                _authError.value = error.message ?: "Telegram rechazó la solicitud"
                Log.e(TAG, "TDLib error", error)
            }
        )
        initializeClient()
    }

    private fun initializeClient() {
        if (telegramParameters.apiId <= 0 || telegramParameters.apiHash.isBlank()) {
            Log.e(TAG, "Faltan TELEGRAM_API_ID o TELEGRAM_API_HASH")
        } else {
            Log.d(TAG, "Parámetros de Telegram cargados para TDLib (api_id=${telegramParameters.apiId})")
        }
        Log.d(TAG, "Inicializando cliente TDLib...")
        scope.launch {
            val database = com.example.data.AppDatabase.getDatabase(context)
            val settings = database.backupDao().getSettings()
            if (settings?.activePhoneNumber != null) {
                activePhoneNumber = settings.activePhoneNumber
            }
            // La sesión solo se considera lista cuando TDLib nativo emite AuthorizationStateReady.
            // No se falsifica el estado usando únicamente valores persistidos en Room.
        }
    }

    /**
     * Paso 1: Enviar número de teléfono para recibir código de confirmación
     */
    fun setPhoneNumber(phoneNumber: String) {
        val cleanPhone = phoneNumber.trim()
        if (phoneRequestInFlight) return
        _authError.value = null
        phoneRequestInFlight = true
        Log.d(TAG, "Configurando número de teléfono: $cleanPhone")
        activePhoneNumber = cleanPhone

        nativeClient?.setPhoneNumber(cleanPhone)
    }

    /**
     * Paso 2: Validar el código de verificación de Telegram
     */
    fun checkVerificationCode(code: String, repository: BackupRepository) {
        val cleanCode = code.trim()
        Log.d(TAG, "Validando código de acceso de Telegram")

        if (cleanCode.isNotEmpty()) {
            destinationRepository = repository
            nativeClient?.checkCode(cleanCode)
        } else {
            Log.e(TAG, "Código de verificación no válido")
        }
    }

    fun checkAuthenticationPassword(password: String) {
        val cleanPassword = password.trim()
        if (cleanPassword.isNotEmpty()) nativeClient?.checkPassword(cleanPassword)
    }

    /**
     * Cerrar sesión activa de TDLib
     */
    fun logout(repository: BackupRepository) {
        Log.d(TAG, "Cerrando sesión de TDLib...")
        scope.launch {
            val settings = repository.getSettings()
            repository.updateSettings(
                settings.copy(
                    activePhoneNumber = null,
                    telegramChannelId = 0L,
                    topicCameraId = 0,
                    topicWhatsappPhotosId = 0,
                    topicWhatsappDocsId = 0
                )
            )
            _isConnected.value = false
            _authState.value = TdApi.AuthorizationStateWaitPhoneNumber()
        }
    }

    /**
     * Asegura que el destino de respaldo en Telegram sea "Mensajes Guardados" (Saved Messages).
     * En Telegram, Mensajes Guardados es el chat privado de almacenamiento ilimitado del propio usuario.
     */
    private var destinationRepository: BackupRepository? = null

    private suspend fun updateSavedMessagesDestination(selfUserId: Long) = withContext(Dispatchers.IO) {
        val repository = destinationRepository ?: return@withContext
        val currentSettings = repository.getSettings()
        repository.updateSettings(
            currentSettings.copy(
                telegramChannelId = selfUserId,
                topicCameraId = 0,
                topicWhatsappPhotosId = 0,
                topicWhatsappDocsId = 0
            )
        )
        Log.d(TAG, "Destino real de Mensajes Guardados obtenido con getMe(): $selfUserId")
    }

    suspend fun ensureSavedMessagesDestination(repository: BackupRepository) = withContext(Dispatchers.IO) {
        destinationRepository = repository
        Log.d(TAG, "Esperando que TDLib resuelva Mensajes Guardados mediante getMe().")
    }

    // Compatibilidad para llamadas existentes
    suspend fun ensureBackupChannelAndTopics(repository: BackupRepository) = ensureSavedMessagesDestination(repository)

    private suspend fun sendFileToTelegram(
        filePath: String,
        chatId: Long,
        repository: BackupRepository
    ): Boolean = suspendCancellableCoroutine { continuation ->
        nativeClient?.sendDocument(
            chatId,
            filePath,
            onSent = { messageId ->
                scope.launch { repository.logUploadCompleted(filePath, messageId) }
                continuation.resume(true)
            },
            onFailure = { error ->
                scope.launch { repository.logUploadFailed(filePath, error.message ?: "Error de Telegram") }
                continuation.resume(false)
            }
        ) ?: continuation.resume(false)
    }

    /** Lee una página del historial real de Mensajes Guardados, incluidos mensajes antiguos. */
    suspend fun loadSavedMessagesPage(
        repository: BackupRepository,
        fromMessageId: Long = 0L,
        limit: Int = 50
    ): Result<Array<org.drinkless.tdlib.TdApi.Message>> = withContext(Dispatchers.IO) {
        val settings = repository.getSettings()
        suspendCancellableCoroutine { continuation ->
        if (settings.telegramChannelId == 0L) {
            continuation.resume(Result.failure(IllegalStateException("Mensajes Guardados no inicializado")))
            return@suspendCancellableCoroutine
        }
        nativeClient?.loadChatHistory(
            settings.telegramChannelId,
            fromMessageId,
            limit,
            onLoaded = { continuation.resume(Result.success(it)) },
            onFailure = { continuation.resume(Result.failure(it)) }
        ) ?: continuation.resume(Result.failure(IllegalStateException("TDLib no está inicializado")))
        }
    }

    suspend fun downloadCloudFile(fileId: Int): Result<String> = suspendCancellableCoroutine { continuation ->
        nativeClient?.downloadFile(
            fileId,
            onDownloaded = { continuation.resume(Result.success(it)) },
            onFailure = { continuation.resume(Result.failure(it)) }
        ) ?: continuation.resume(Result.failure(IllegalStateException("TDLib no está inicializado")))
    }

    /**
     * Sube un archivo real a 'Mensajes Guardados' de Telegram con confirmación real de TDLib.
     */
    private suspend fun awaitTelegramReady() {
        if (_isConnected.value && _authState.value is TdApi.AuthorizationStateReady) return
        withTimeout(30_000L) {
            authState.first { it is TdApi.AuthorizationStateReady }
        }
    }

    suspend fun uploadFileToTopic(
        filePath: String,
        topicId: Int,
        repository: BackupRepository
    ): Boolean = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "Error: El archivo no existe en el almacenamiento: $filePath")
            repository.logUploadFailed(filePath, "El archivo no existe en el almacenamiento local")
            return@withContext false
        }

        try {
            awaitTelegramReady()
        } catch (e: Exception) {
            repository.logUploadFailed(filePath, "Telegram todavía no está listo: ${e.message ?: "sin conexión"}")
            return@withContext false
        }

        val settings = repository.getSettings()
        if (settings.telegramChannelId == 0L) {
            Log.e(TAG, "El destino 'Mensajes Guardados' no está configurado")
            repository.logUploadFailed(filePath, "Mensajes Guardados no inicializado")
            return@withContext false
        }

        return@withContext sendFileToTelegram(filePath, settings.telegramChannelId, repository)
    }
}
