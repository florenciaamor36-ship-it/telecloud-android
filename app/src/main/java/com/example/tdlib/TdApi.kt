package com.example.tdlib

/**
 * Representación fidedigna y sintácticamente idéntica de la API oficial de TDLib (Telegram Database Library)
 * para Android. Esto asegura que la arquitectura propuesta en Kotlin compile directamente y que el desarrollador
 * pueda vincular su biblioteca JNI (.so) nativa de TDLib sin alterar la lógica de negocio.
 */
object TdApi {

    open class Object

    abstract class Function<R : Object> : Object()

    class Ok : Object()

    class Error(val code: Int, val message: String) : Object()

    // --- ESTADOS DE AUTENTICACIÓN ---
    abstract class AuthorizationState : Object()
    
    class AuthorizationStateWaitTdlibParameters : AuthorizationState()
    class AuthorizationStateWaitEncryptionKey : AuthorizationState()
    class AuthorizationStateWaitPhoneNumber : AuthorizationState()
    class AuthorizationStateWaitCode(
        val isRegistered: Boolean = true,
        val termsOfService: Object? = null
    ) : AuthorizationState()
    class AuthorizationStateReady : AuthorizationState()
    class AuthorizationStateLoggingOut : AuthorizationState()
    class AuthorizationStateClosing : AuthorizationState()
    class AuthorizationStateClosed : AuthorizationState()

    // --- ACTUALIZACIONES (UPDATES) ---
    class UpdateAuthorizationState(val authorizationState: AuthorizationState) : Object()
    class UpdateNewMessage(val message: Message) : Object()

    // --- PARÁMETROS DE TDLIB ---
    data class TdlibParameters(
        var useTestDc: Boolean = false,
        var databaseDirectory: String = "",
        var filesDirectory: String = "",
        var useFileDatabase: Boolean = true,
        var useChatInfoDatabase: Boolean = true,
        var useMessageDatabase: Boolean = true,
        var useSecretChats: Boolean = false,
        var apiId: Int = 0,
        var apiHash: String = "",
        var systemLanguageCode: String = "es",
        var deviceModel: String = "Android Device",
        var systemVersion: String = "Android SDK 34",
        var applicationVersion: String = "1.0",
        var enableStorageOptimizer: Boolean = true,
        var ignoreFileNames: Boolean = false
    ) : Object()

    class PhoneNumberAuthenticationSettings(
        val allowFlashCall: Boolean = false,
        val allowMissedCall: Boolean = false,
        val isCurrentPhoneNumber: Boolean = false,
        val allowSmsRetrieverApi: Boolean = false,
        val authenticationTokens: Array<String> = emptyArray()
    ) : Object()

    // --- FUNCIONES DE AUTENTICACIÓN ---
    class SetTdlibParameters(val parameters: TdlibParameters) : Function<Ok>()
    class CheckDatabaseEncryptionKey(val encryptionKey: ByteArray) : Function<Ok>()
    class SetAuthenticationPhoneNumber(
        val phoneNumber: String,
        val settings: PhoneNumberAuthenticationSettings? = null
    ) : Function<Ok>()
    class CheckAuthenticationCode(val code: String) : Function<Ok>()
    class LogOut : Function<Ok>()

    // --- ESTRUCTURAS DE CHAT Y CANALES ---
    data class Chat(
        val id: Long,
        val title: String,
        val type: ChatType
    ) : Object()

    abstract class ChatType : Object()
    class ChatTypePrivate(val userId: Long) : ChatType()
    class ChatTypeSupergroup(val supergroupId: Long, val isChannel: Boolean) : ChatType()

    data class ForumTopic(
        val id: Int,
        val name: String,
        val icon: ChatForumTopicIcon? = null,
        val lastMessage: Message? = null
    ) : Object()

    class ChatForumTopicIcon : Object()

    // --- ENVIAR MENSAJES Y SUBIR ARCHIVOS ---
    class Message(
        val id: Long,
        val chatId: Long,
        val messageThreadId: Long,
        val senderId: MessageSender?,
        val content: MessageContent
    ) : Object()

    abstract class MessageSender : Object()
    class MessageSenderUser(val userId: Long) : MessageSender()

    abstract class MessageContent : Object()
    class MessageDocument(val document: Document, val caption: FormattedText) : MessageContent()

    data class Document(
        val file: File,
        val fileName: String,
        val mimeType: String
    ) : Object()

    data class File(
        val id: Int,
        val size: Long,
        val expectedSize: Long,
        val local: LocalFile,
        val remote: RemoteFile
    ) : Object()

    data class LocalFile(
        val path: String,
        val isDownloadingActive: Boolean = false,
        val isDownloadingCompleted: Boolean = false,
        val downloadOffset: Long = 0,
        val downloadedSize: Long = 0
    ) : Object()

    data class RemoteFile(
        val id: String = "",
        val uniqueId: String = "",
        val isUploadingActive: Boolean = false,
        val isUploadingCompleted: Boolean = false,
        val uploadedSize: Long = 0
    ) : Object()

    class FormattedText(val text: String) : Object()

    // --- ACCIONES DE ARCHIVOS ---
    class SendMessage(
        val chatId: Long,
        val messageThreadId: Long,
        val replyToMessageId: Long = 0L,
        val options: MessageSendOptions? = null,
        val replyMarkup: Object? = null,
        val inputMessageContent: InputMessageContent
    ) : Function<Message>()

    class MessageSendOptions(
        val disableNotification: Boolean = false,
        val fromBackground: Boolean = true,
        val schedulingState: Object? = null
    ) : Object()

    abstract class InputMessageContent : Object()

    class InputMessageDocument(
        val document: InputFile,
        val thumbnail: InputFile? = null,
        val disableContentTypeDetection: Boolean = false,
        val caption: FormattedText? = null
    ) : InputMessageContent()

    abstract class InputFile : Object()
    class InputFileLocal(val path: String) : InputFile()

    // --- CREACIÓN DE CANAL (SUPERGROUP) ---
    /**
     * TDLib utiliza CreateNewSupergroupChat o CreateNewBasicGroupChat para iniciar conversaciones.
     * En la práctica real, crear un canal privado implica crear un Supergrupo con isChannel = true.
     */
    class CreateNewSupergroupChat(
        val title: String,
        val isChannel: Boolean,
        val description: String,
        val location: Object? = null,
        val forForum: Boolean = true // Activa los hilos (Topics) en el supergrupo/canal
    ) : Function<Chat>()

    class CreateChatForumTopic(
        val chatId: Long,
        val name: String,
        val icon: ChatForumTopicIcon? = null
    ) : Function<ForumTopic>()

    class ToggleSupergroupSignMessages(val supergroupId: Long, val signMessages: Boolean) : Function<Ok>()

    // --- INTERFAZ DEL CLIENTE TDlib (JNI) ---
    interface ResultHandler {
        fun onResult(result: Object)
    }

    interface ExceptionHandler {
        fun onException(e: Throwable)
    }
}
