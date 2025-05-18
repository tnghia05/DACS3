package com.eritlab.jexmon.presentation.screens.conversation_screen.component

import android.content.Context
import android.util.Log
import com.eritlab.jexmon.domain.model.Message
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ChatRepository(private val context: Context) {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appCheck = FirebaseAppCheck.getInstance()
    private val vertexAIService = VertexAIService(context)
    private val TAG = "ChatRepository"

    suspend fun createNewChat(userId: String): AIResult<String> {
        return try {
            val chatId = UUID.randomUUID().toString()
            val chat = hashMapOf(
                "userId" to userId,
                "createdAt" to System.currentTimeMillis(),
                "lastMessage" to "Chat started",
                "lastMessageTime" to System.currentTimeMillis()
            )
            
            db.collection("chats").document(chatId).set(chat).await()
            AIResult.Success(chatId)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat", e)
            AIResult.Error(e)
        }
    }

    suspend fun sendUserMessage(chatId: String, userId: String, message: String): AIResult<Unit> {
        var retryCount = 0
        val maxRetries = 3
        
        while (retryCount < maxRetries) {
            try {
                val messageId = "${UUID.randomUUID()}_${System.nanoTime()}"
                val messageData = hashMapOf(
                    "messageId" to messageId,
                    "text" to message,
                    "isFromUser" to 1,
                    "timestamp" to System.currentTimeMillis(),
                    "userId" to userId,
                    "type" to "text"
                )

                db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .document(messageId)
                    .set(messageData)
                    .await()

                // Update last message in chat document
                db.collection("chats")
                    .document(chatId)
                    .update(
                        mapOf(
                            "lastMessage" to message,
                            "lastMessageTime" to FieldValue.serverTimestamp()
                        )
                    ).await()

                return AIResult.Success(Unit)
            } catch (e: Exception) {
                retryCount++
                if (retryCount >= maxRetries) {
                    Log.e(TAG, "Failed to send message after $maxRetries attempts", e)
                    return AIResult.Error(e)
                }
                delay(1000L * retryCount) // Exponential backoff
            }
        }
        return AIResult.Error(Exception("Failed to send message after $maxRetries attempts"))
    }

    suspend fun getBotReplyAndSave(chatId: String, userId: String, userMessage: String): AIResult<Map<String, Any>> {
        return try {
            when (val aiResponse = vertexAIService.generateResponse(userMessage)) {
                is AIResult.Success -> {
                    val processedResponse = if (aiResponse.data.contains("[PRODUCT_START]")) {
                        aiResponse.data
                    } else {
                        aiResponse.data.replace("[PRODUCT_START]", "")
                            .replace("[PRODUCT_END]", "")
                    }

                    val messageId = "${UUID.randomUUID()}_${System.nanoTime()}"
                    val botMessage = hashMapOf(
                        "messageId" to messageId,
                        "text" to processedResponse,
                        "isFromUser" to false,
                        "timestamp" to System.currentTimeMillis(),
                        "userId" to userId,
                        "type" to if (processedResponse.contains("[PRODUCT_START]")) "product" else "text"
                    )

                    db.collection("chats")
                        .document(chatId)
                        .collection("messages")
                        .document(messageId)
                        .set(botMessage)
                        .await()

                    AIResult.Success(mapOf("reply" to processedResponse))
                }
                is AIResult.Error -> {
                    throw aiResponse.exception
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting bot reply", e)
            AIResult.Error(e)
        }
    }

    suspend fun getRecentChatHistory(userId: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("chats")
                .document(userId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()

            snapshot.documents.reversed().mapNotNull { it.data }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getChatHistory(chatId: String): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting chat history", e)
            emptyList()
        }
    }

    suspend fun saveChatMessage(chatId: String, userId: String, userMessage: String, botReply: String) {
        try {
            val chatRef = db.collection("chats").document(chatId)

            chatRef.set(
                hashMapOf(
                    "lastMessage" to userMessage,
                    "lastMessageTime" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            val timestamp = FieldValue.serverTimestamp()

            chatRef.collection("messages").add(
                hashMapOf(
                    "senderId" to userId,
                    "message" to userMessage,
                    "timestamp" to timestamp,
                    "from" to "user"
                )
            ).await()

            chatRef.collection("messages").add(
                hashMapOf(
                    "senderId" to "AI_ASSISTANT",
                    "message" to botReply,
                    "timestamp" to timestamp,
                    "from" to "bot"
                )
            ).await()

        } catch (e: Exception) {
            Log.e("ChatRepository", "Error saving message", e)
        }
    }

    fun getMessagesFromFirestore(chatId: String, onMessagesLoaded: (List<Message>) -> Unit) {
        FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { result ->
                val messages = result.map { doc ->
                    Message(
                        text = doc.getString("message") ?: "",
                        isFromUser = doc.getString("from") == "user",
                        timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: 0L
                    )
                }
                onMessagesLoaded(messages)
            }
    }

    private fun parseMessage(doc: DocumentSnapshot): Message? {
        return try {
            val messageId = doc.getString("messageId") ?: doc.id
            
            val isFromUser = try {
                when (val value = doc.get("isFromUser")) {
                    is Boolean -> value
                    is Long -> value == 1L
                    is Int -> value == 1
                    is String -> value.toLowerCase() == "true" || value == "1"
                    else -> false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing isFromUser", e)
                false
            }

            val text = doc.getString("text") ?: ""

            val timestamp = try {
                doc.getLong("timestamp") ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }

            Message(
                messageId = messageId,
                text = text,
                isFromUser = isFromUser,
                timestamp = timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
            null
        }
    }

    fun listenForMessages(chatId: String, limit: Int = 20, onUpdate: (List<Message>) -> Unit) {
        if (chatId.isBlank()) {
            Log.e(TAG, "Invalid chat ID")
            onUpdate(emptyList())
            return
        }

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening for messages", error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents
                    ?.mapNotNull { doc -> parseMessage(doc) }
                    ?.distinctBy { it.messageId }
                    ?.sortedBy { it.timestamp }
                    ?: emptyList()
                    
                onUpdate(messages)
            }
    }

    suspend fun loadMoreMessages(chatId: String, beforeTimestamp: Long, pageSize: Int): List<Message> {
        if (chatId.isBlank()) {
            Log.e(TAG, "Invalid chat ID")
            return emptyList()
        }

        return try {
            db.collection("chats")
                .document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .whereLessThan("timestamp", beforeTimestamp)
                .limit(pageSize.toLong())
                .get()
                .await()
                .documents
                .mapNotNull { doc -> parseMessage(doc) }
                .sortedBy { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading more messages", e)
            emptyList()
        }
    }
}
