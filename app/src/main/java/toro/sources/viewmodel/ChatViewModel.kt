package toro.sources.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
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
import toro.sources.db.ComicRepository
import toro.sources.network.ChatConnectionManager
import toro.sources.network.RetrofitClient
import toro.sources.session.SessionManager
import toro.sources.viewmodel.common.UserSearchDelegate
import java.util.*
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val repository: ComicRepository,
    private val chatConnectionManager: ChatConnectionManager
) : ViewModel() {

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
        else list.filter { it.otherUserName.contains(query, ignoreCase = true) || it.lastMessage?.contains(query, ignoreCase = true) == true }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        clearChatError()
        viewModelScope.launch {
            chatConnectionManager.incomingMessages.collect { message ->
                handleIncomingMessage(message)
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
                    RetrofitClient.comicApiService.markMessageAsDelivered(message.id)
                } else {
                    repository.saveMessage(message)
                    repository.updateMessageDeliveryStatus(message.id, true)
                }
            } else {
                repository.saveMessage(message)
                repository.getConversationById(message.conversationId)?.let { convo ->
                    repository.saveConversations(listOf(convo.copy(lastMessage = message.content, timestamp = message.timestamp)))
                }
            }
        }
    }

    fun getChatMessages(conversationId: String) {
        _currentConversationId.value = conversationId
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val lastTs = repository.getLastMessageTimestamp(conversationId)
                    val msgs = RetrofitClient.comicApiService.getChatMessages(conversationId, lastTs)
                    if (msgs.isNotEmpty()) repository.saveMessages(msgs)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Failed to fetch messages: ${e.message}")
            }
        }
    }

    fun sendMessage(
        conversationId: String,
        content: String,
        isSpoiler: Boolean = false,
        sharedId: String? = null,
        sharedType: ShareType? = null,
        sharedComicId: String? = null,
        receiverId: String? = null,
        receiverName: String? = null
    ) {
        viewModelScope.launch {
            try {
                val tempId = UUID.randomUUID().toString()
                val convo = withContext(Dispatchers.IO) {
                    repository.getConversationById(conversationId) ?: receiverId?.let { rid ->
                        val newConvo = Conversation(
                            conversationId = conversationId,
                            otherUserId = rid,
                            otherUserName = receiverName ?: "Chat",
                            lastMessage = content,
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

                val newMessage = ChatMessage(
                    id = tempId,
                    conversationId = conversationId,
                    senderId = sessionManager.userProfile.value?.id ?: "",
                    receiverId = convo.otherUserId,
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    isSpoiler = isSpoiler,
                    replyToMessageId = _replyingToMessage.value?.id,
                    sharedId = sharedId,
                    sharedType = sharedType,
                    sharedComicId = sharedComicId
                )

                withContext(Dispatchers.IO) {
                    repository.saveMessage(newMessage)
                    repository.saveConversations(listOf(convo.copy(lastMessage = content, timestamp = newMessage.timestamp)))
                }

                if (!chatConnectionManager.sendMessage(newMessage)) {
                    _chatUiState.update { it.copy(errorMessage = "Message queued offline") }
                }
                _replyingToMessage.value = null
            } catch (e: Exception) {
                _chatUiState.update { it.copy(errorMessage = "Failed to send message: ${e.message}") }
            }
        }
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
                _chatUiState.update { it.copy(errorMessage = "Failed to accept: ${e.message}") }
            }
        }
    }

    fun declineFriend(requestId: String) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { RetrofitClient.comicApiService.declineChatRequest(requestId) }
                _chatRequests.update { it.filter { r -> r.id != requestId } }
            } catch (e: Exception) {
                _chatUiState.update { it.copy(errorMessage = "Failed to decline: ${e.message}") }
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
                _chatUiState.update { it.copy(errorMessage = "Failed to unfriend: ${e.message}") }
            }
        }
    }

    fun getInbox() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val conversations = RetrofitClient.comicApiService.getInbox()
                    for (convo in conversations) {
                        if (convo.lastMessage != null) convo.lastMessage = chatConnectionManager.decryptContent(convo.lastMessage!!)
                    }
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
                _chatUiState.update { it.copy(errorMessage = "Failed to send request ${e.message}") }
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