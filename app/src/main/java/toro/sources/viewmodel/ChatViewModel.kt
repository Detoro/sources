package toro.sources.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.toro.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import toro.sources.MessageSyncWorker
import toro.sources.db.ComicRepository
import toro.sources.media.MediaUploadManager
import toro.sources.network.ChatConnectionManager
import toro.sources.network.RetrofitClient
import toro.sources.session.SessionManager
import toro.sources.viewmodel.common.UserSearchDelegate
import java.util.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val sessionManager: SessionManager,
    private val repository: ComicRepository,
    private val chatConnectionManager: ChatConnectionManager,
    private val mediaUploadManager: MediaUploadManager
) : AndroidViewModel(application) {

    private val _chatUiState = MutableStateFlow(ChatUiState())
    val chatUiState = _chatUiState.asStateFlow()

    private val _chatRequests = MutableStateFlow<List<ChatRequest>>(emptyList())
    val chatRequests = _chatRequests.asStateFlow()

    private val userSearch = UserSearchDelegate(viewModelScope)
    val userSuggestions = userSearch.userSuggestions

    @OptIn(ExperimentalCoroutinesApi::class)
    val inbox: StateFlow<List<Conversation>> = sessionManager.userProfile
        .flatMapLatest { user ->
            if (user != null) repository.getConversations() else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentConversationId = MutableStateFlow<String?>(null)
    val currentConversationId = _currentConversationId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val chatMessages: StateFlow<List<ChatMessage>> = sessionManager.userProfile
        .flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else _currentConversationId.flatMapLatest { id ->
                if (id != null) repository.getMessagesForConversation(id)
                else flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _typingUsers = MutableStateFlow<Set<String>>(emptySet())
    val typingUsers = _typingUsers.asStateFlow()

    private val typingJobs = mutableMapOf<String, Job>()

    private val _replyingToMessage = MutableStateFlow<ChatMessage?>(null)
    val replyingToMessage = _replyingToMessage.asStateFlow()

    private val _editingMessage = MutableStateFlow<ChatMessage?>(null)
    val editingMessage = _editingMessage.asStateFlow()

    private val _inboxSearchQuery = MutableStateFlow("")
    val inboxSearchQuery = _inboxSearchQuery.asStateFlow()
    val pendingRequestsCount: StateFlow<Int> = _chatRequests
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val filteredInbox = combine(inbox, _inboxSearchQuery) { list, query ->
        if (query.isBlank()) list
        else list.filter {
            it.otherUser.username.contains(query, ignoreCase = true) ||
                    it.lastMessage?.content?.contains(query, ignoreCase = true) == true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        clearChatError()
        resyncOfflineMessages()
        viewModelScope.launch {
            chatConnectionManager.incomingMessages.collect { message ->
                handleIncomingMessage(message)
            }
        }
        viewModelScope.launch {
            chatConnectionManager.connectionState.collect { connected ->
                if (connected == ChatConnectionManager.ConnectionState.CONNECTED) {
                    resyncOfflineMessages()
                }
            }
        }
        chatConnectionManager.connect()
    }

    private suspend fun handleIncomingMessage(message: ChatMessage) {
        if (message.mediaType == "SYSTEM_TYPING") {
            val senderId = message.senderId
            if (message.content == "TYPING_START") {
                _typingUsers.update { it + senderId }
                typingJobs[senderId]?.cancel()
                typingJobs[senderId] = viewModelScope.launch {
                    delay(5000.milliseconds)
                    _typingUsers.update { it - senderId }
                    typingJobs.remove(senderId)
                }
            } else {
                _typingUsers.update { it - senderId }
            }
            return
        }

        withContext(Dispatchers.IO) {
            val myUserId = sessionManager.userProfile.value?.id
            if (message.senderId == myUserId) {
                val pending = repository.getMessageById(message.id)
                if (pending != null) {
                    repository.confirmPendingMessage(message.id, message.content)
                } else {
                    repository.saveMessage(message)
                    repository.updateMessageDeliveryStatus(message.id, true)
                }
            } else {
                repository.saveMessage(message)
                RetrofitClient.comicApiService.markMessageAsDelivered(message.id)
                repository.getConversationById(message.conversationId)?.let { convo ->
                    val content = message.toContent()
                    val summary = toMessageSummary(content, message.senderId, message.timestamp)
                    val updatedConvo = convo.copy(
                        lastMessage = summary,
                        timestamp = message.timestamp,
                        unreadCount = convo.unreadCount + 1
                    )
                    repository.saveConversations(listOf(updatedConvo))
                }
            }
        }
    }

    private fun resyncOfflineMessages() {
        MessageSyncWorker.enqueue(getApplication())
    }

    fun getChatMessages(conversationId: String) {
        _currentConversationId.value = conversationId
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val lastTs = repository.getLastMessageTimestamp(conversationId)
                    val messages = RetrofitClient.comicApiService.getChatMessages(conversationId, lastTs)
                    if (messages.isNotEmpty()) repository.saveMessages(messages)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to fetch messages: ${e.message}")
            }
        }
    }

    private suspend fun uploadAttachment(uri: Uri): Pair<String, Boolean> = withContext(Dispatchers.IO) {
        val mimeType = getApplication<Application>().contentResolver.getType(uri) ?: ""
        val isVideo = mimeType.startsWith("video")
        val url = mediaUploadManager.uploadFileToCloudinary(uri)
        url to isVideo
    }

    fun sendMessage(
        conversationId: String,
        content: MessageContent,
        isSpoiler: Boolean = false,
        receiverId: String? = null,
        receiverName: String? = null,
        attachment: Uri? = null
    ) {
        viewModelScope.launch {
            try {
                val resolvedContent = if (attachment != null && content is MessageContent.Text) {
                    try {
                        val (url, isVideo) = uploadAttachment(attachment)
                        val body = content.body
                        when {
                            body.isNotBlank() && isVideo -> MessageContent.TextWithMedia(body, emptyList(), listOf(url))
                            body.isNotBlank() -> MessageContent.TextWithMedia(body, listOf(url), emptyList())
                            isVideo -> MessageContent.Video(listOf(url))
                            else -> MessageContent.Image(listOf(url))
                        }
                    } catch (e: Exception) {
                        _chatUiState.update { it.copy(errorMessage = "Failed to upload attachment: ${e.message}") }
                        return@launch
                    }
                } else {
                    content
                }

                val tempId = UUID.randomUUID().toString()
                val convo = withContext(Dispatchers.IO) {
                    repository.getConversationById(conversationId) ?: receiverId?.let { rid ->
                        val newConvo = Conversation(
                            conversationId = conversationId,
                            otherUser = ChatUser(
                                userId = rid,
                                username = receiverName ?: "Chat"
                            ),
                            timestamp = System.currentTimeMillis()
                        )
                        repository.saveConversations(listOf(newConvo))
                        newConvo
                    }
                }

                if (convo == null) {
                    _chatUiState.update { it.copy(errorMessage = "Can't send message: no recipient") }
                    return@launch
                }

                if (resolvedContent is MessageContent.System) {
                    _chatUiState.update { it.copy(errorMessage = "Can't send a system signal as a message") }
                    return@launch
                }

                val fields = resolvedContent.toChatMessageFields()

                val newMessage = ChatMessage(
                    id = tempId,
                    conversationId = conversationId,
                    senderId = sessionManager.userProfile.value?.id ?: "",
                    receiverId = convo.otherUser.userId,
                    content = fields.content,
                    timestamp = System.currentTimeMillis(),
                    isSpoiler = isSpoiler,
                    replyToMessageId = _replyingToMessage.value?.id,
                    sharedId = fields.sharedId,
                    sharedType = fields.sharedType,
                    sharedComicId = fields.sharedComicId,
                    imageUrls = fields.imageUrls,
                    videoUrls = fields.videoUrls
                )

                withContext(Dispatchers.IO) {
                    repository.saveMessage(newMessage)
                    val summary = toMessageSummary(resolvedContent, newMessage.senderId, newMessage.timestamp)
                    repository.saveConversations(
                        listOf(convo.copy(lastMessage = summary, timestamp = newMessage.timestamp))
                    )
                }

                if (!chatConnectionManager.sendMessage(newMessage)) {
                    _chatUiState.update { it.copy(errorMessage = "Message queued offline") }
                    resyncOfflineMessages()
                }
                _replyingToMessage.value = null
            } catch (e: Exception) {
                _chatUiState.update { it.copy(errorMessage = "Failed to send message: ${e.message}") }
            }
        }
    }

    internal fun conversationPreviewText(content: MessageContent): String = when (content) {
        is MessageContent.Text -> content.body
        is MessageContent.TextWithMedia -> content.body
        is MessageContent.Image -> "Photo"
        is MessageContent.Video -> "Video"
        is MessageContent.Shared -> "Shared a ${content.type.name.lowercase()}"
        is MessageContent.System -> ""
    }

    private fun toMessageSummary(content: MessageContent, senderId: String, timestamp: Long): MessageSummary {
        val type = when (content) {
            is MessageContent.Text -> MessageType.TEXT
            is MessageContent.TextWithMedia -> MessageType.TEXT
            is MessageContent.Image -> MessageType.IMAGE
            is MessageContent.Video -> MessageType.VIDEO
            is MessageContent.Shared -> MessageType.SHARE
            is MessageContent.System -> MessageType.SYSTEM
        }
        return MessageSummary(
            content = conversationPreviewText(content),
            senderId = senderId,
            timestamp = timestamp,
            type = type
        )
    }

    fun deleteMessage(conversationId: String, messageId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.deleteMessageById(messageId)
                    RetrofitClient.comicApiService.deleteMessage(conversationId, messageId)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Delete failed: ${e.message}")
            }
        }
    }

    fun editMessage(conversationId: String, messageId: String, content: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateMessageContent(messageId, content)
                    RetrofitClient.comicApiService.updateMessage(conversationId, messageId, ChatMessage(id = messageId, content = content))
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Edit failed: ${e.message}")
            }
        }
    }

    fun markMessageAsRead(messageId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    repository.updateMessageReadStatus(messageId, true)
                    RetrofitClient.comicApiService.markMessageAsRead(messageId)
                }
            } catch (e: Exception) {
                _chatUiState.update { it.copy(errorMessage = "Failed to mark message as read: ${e.message}") }
            }
        }
    }

    fun sendTypingIndicator(conversationId: String, isTyping: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val signal = ChatMessage(
                id = "TYPING_${conversationId}_${sessionManager.userProfile.value?.id}",
                conversationId = conversationId,
                senderId = sessionManager.userProfile.value?.id ?: "",
                content = if (isTyping) "TYPING_START" else "TYPING_STOP",
                timestamp = System.currentTimeMillis(),
                mediaType = "SYSTEM_TYPING"
            )
            chatConnectionManager.sendMessage(signal)
        }
    }

    fun setChatBackground(conversationId: String, uri: Uri?) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repository.updateChatBackground(conversationId, uri?.toString()) }
        }
    }

    fun getChatRequests() {
        viewModelScope.launch {
            try {
                _chatRequests.value = withContext(Dispatchers.IO) { RetrofitClient.comicApiService.getChatRequests() }
            } catch (e: Exception) {
                _chatUiState.update { it.copy(errorMessage = "Failed to fetch requests: ${e.message}") }
            }
        }
    }

    fun acceptFriend(requestId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { RetrofitClient.comicApiService.acceptChatRequest(requestId) }
                _chatRequests.update { it.filter { r -> r.id != requestId } }
                getInbox()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to accept friend: ${e.message}")
            }
        }
    }

    fun declineFriend(requestId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { RetrofitClient.comicApiService.declineChatRequest(requestId) }
                _chatRequests.update { it.filter { r -> r.id != requestId } }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to decline friend: ${e.message}")
            }
        }
    }

    fun unAddFriend(userId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { RetrofitClient.comicApiService.unfriendUser(userId) }
                _currentConversationId.value?.let { id ->
                    repository.deleteMessagesForConversation(id)
                    repository.deleteConversationById(id)
                }
                resetChatState()
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to unfriend: ${e.message}")
            }
        }
    }

    fun getInbox() {
        viewModelScope.launch {
            try {
                val conversations = withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.getInbox()
                }
                withContext(Dispatchers.IO) {
                    repository.saveConversations(conversations)
                }
            } catch (e: Exception) {
                _chatUiState.update { it.copy(errorMessage = "Failed to fetch inbox: ${e.message}") }
            }
        }
    }

    fun updateInboxSearchQuery(query: String) { _inboxSearchQuery.value = query }

    fun searchUsers(query: String) = userSearch.search(query)

    fun clearUserSuggestions() = userSearch.clear()

    fun sendChatRequest(receiverId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    RetrofitClient.comicApiService.sendChatRequest(receiverId)
                }
                onSuccess()
            } catch (e: Exception) {
                _chatUiState.update { it.copy(errorMessage = "Failed to send request: ${e.message}") }
            }
        }
    }

    fun resetChatState() { _currentConversationId.value = null }
    fun clearChatHistory(conversationId: String) {
        viewModelScope.launch { withContext(Dispatchers.IO) { repository.deleteMessagesForConversation(conversationId) } }
    }
    fun setReplyTarget(message: ChatMessage?) { _replyingToMessage.value = message }
    fun setEditingMessage(message: ChatMessage?) { _editingMessage.value = message }
    fun clearChatError() { _chatUiState.update { it.copy(errorMessage = null) } }
}

data class ChatUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)