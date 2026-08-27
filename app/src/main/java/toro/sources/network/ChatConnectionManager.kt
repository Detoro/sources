package toro.sources.network

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import toro.sources.models.ChatMessage
import toro.sources.utils.CryptoUtils
import kotlin.time.Duration.Companion.milliseconds

class ChatConnectionManager(
    private val socketFactory: (WebSocketListener) -> WebSocket,
    private val coroutineScope: CoroutineScope
) {
    private var cWebSocket: WebSocket? = null
    private val socketJson = Json { ignoreUnknownKeys = true }
    private val connectionLock = Any()

    private var reconnectionDelay = 5000L
    private var reconnectionJob: Job? = null

    private val _incomingMessages = MutableSharedFlow<ChatMessage>()
    val incomingMessages = _incomingMessages.asSharedFlow()

    private val _connectionState = MutableSharedFlow<ConnectionState>()
    val connectionState = _connectionState.asSharedFlow()

    fun connect() {
        synchronized(connectionLock) {
            if (cWebSocket != null) return
            reconnectionJob?.cancel()

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    coroutineScope.launch { _connectionState.emit(ConnectionState.CONNECTED) }
                    reconnectionDelay = 5000L
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val rawMessage = socketJson.decodeFromString<ChatMessage>(text)
                        val plaintextMessage =
                            rawMessage.copy(
                                content = decryptContent(rawMessage.content),
                            )
                        coroutineScope.launch { _incomingMessages.emit(plaintextMessage) }
                    } catch (e: Exception) {
                        Log.e("ChatConnectionManager", "Failed to decode: ${e.message}")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    handleDisconnect(t)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    synchronized(connectionLock) { cWebSocket = null }
                }
            }
            cWebSocket = socketFactory(listener)
        }
    }

    fun sendMessage(message: ChatMessage): Boolean {
        return try {
            val encryptedContent = CryptoUtils.encrypt(message.content)
            val secureMessage = message.copy(content = encryptedContent)
            val jsonMessage = socketJson.encodeToString(secureMessage)

            cWebSocket?.send(jsonMessage) ?: false
        } catch (e: Exception) {
            Log.e("ChatConnectionManager", "Encryption or sending failed: ${e.message}")
            false
        }
    }

    fun decryptContent(encryptedContent: String): String {
        return try {
            CryptoUtils.decrypt(encryptedContent)
        } catch (e: Exception) {
            "[Unable to decrypt message] ${e.message}"
        }
    }

    fun disconnect() {
        reconnectionJob?.cancel()
        synchronized(connectionLock) {
            cWebSocket?.close(1000, "App backgrounded")
            cWebSocket = null
        }
    }

    private fun handleDisconnect(t: Throwable) {
        synchronized(connectionLock) { cWebSocket = null }
        coroutineScope.launch { _connectionState.emit(ConnectionState.DISCONNECTED) }
        reconnectWithDelay()
    }

    private fun reconnectWithDelay() {
        if (cWebSocket != null) return
        reconnectionJob?.cancel()
        reconnectionJob = coroutineScope.launch {
            delay(reconnectionDelay.milliseconds)
            reconnectionDelay = (reconnectionDelay * 2).coerceAtMost(60000L)
            connect()
        }
    }

    enum class ConnectionState { CONNECTED, DISCONNECTED }
}