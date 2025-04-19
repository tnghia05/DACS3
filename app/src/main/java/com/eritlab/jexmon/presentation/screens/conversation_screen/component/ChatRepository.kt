
package com.eritlab.jexmon.presentation.screens.conversation_screen.component
import android.util.Log
import com.eritlab.jexmon.domain.model.Message
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class ChatRepository {

    private val functions = FirebaseFunctions.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val firebaseAppCheck = FirebaseAppCheck.getInstance()
    private val auth = FirebaseAuth.getInstance()
    suspend fun createNewChat(userId: String): Result<String> {
        return try {
            // 👤 Dữ liệu chat mới
            val chatData = mapOf(
                "participants" to listOf(userId, "AI_ASSISTANT"),
                "name" to "AI Assistant",
                "lastMessage" to "",
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "unreadCount" to 0,
                "ownerId" to userId
            )

            // 🆕 Tạo document mới với ID tự động
            val chatRef = db.collection("chats").document()
            chatRef.set(chatData).await()

            // ✅ Trả lại chatId vừa tạo
            Result.success(chatRef.id)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating new chat", e)
            Result.failure(e)
        }
    }

    suspend fun sendUserMessage(chatId: String, userId: String, message: String): Result<Unit> {
        return try {
            val chatRef = db.collection("chats").document(chatId)

            // Cập nhật tin nhắn cuối cùng
            chatRef.set(
                mapOf(
                    "lastMessage" to message,
                    "lastMessageTime" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            // Lưu tin nhắn của người dùng
            chatRef.collection("messages").add(
                mapOf(
                    "senderId" to userId,
                    "message" to message,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "from" to "user"
                )
            ).await()

            // ✅ Gọi Cloud Function để xử lý message
            val data = hashMapOf(
                "message" to message
            )
            functions
                .getHttpsCallable("processChat")
                .call(data)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error sending user message", e)
            Result.failure(e)
        }
    }

    suspend fun getBotReplyAndSave(
        chatId: String,
        userId: String,
        userMessage: String
    ): Result<Map<String, Any>> {
        return try {
            // Chuẩn bị dữ liệu gửi lên Cloud Function
            val data = mapOf("message" to userMessage)

            // Gọi Cloud Function processChat
            val result = functions.getHttpsCallable("processChat")
                .call(data)
                .await()

            // Xử lý dữ liệu trả về
            val responseData = result.data
            val responseMap = responseData as? Map<*, *>
            val botReply = responseMap?.get("reply") as? String ?: "Không có phản hồi"

            Log.d("ChatRepository", "🤖 Bot reply: $botReply")


            // Lưu phản hồi của bot vào Firestore
            val chatRef = db.collection("chats").document(chatId)
            chatRef.set(
                mapOf(
                    "lastMessage" to botReply,
                    "lastMessageTime" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            chatRef.collection("messages").add(
                mapOf(
                    "senderId" to "AI_ASSISTANT",
                    "message" to botReply,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "from" to "bot"
                )
            ).await()

            // Trả về kết quả thành công với dữ liệu phản hồi
            @Suppress("UNCHECKED_CAST")
            Result.success(responseMap as? Map<String, Any> ?: mapOf("reply" to botReply))
        } catch (e: Exception) {
            Log.e("ChatRepository", "❌ Error getting bot reply", e)
            Result.failure(e)
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

    fun listenForMessages(chatId: String, onMessagesUpdated: (List<Message>) -> Unit): ListenerRegistration {
        return FirebaseFirestore.getInstance()
            .collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "❌ Error listening for messages", error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.d("ChatRepository", "⚠️ Snapshot is null")
                    return@addSnapshotListener
                }

                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        Message(
                            text = doc.getString("message") ?: "",
                            isFromUser = doc.getString("from") == "user",
                            timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                        ).also { message ->
                            Log.d("ChatRepository", "📩 Received message: ${message.text} - From User: ${message.isFromUser}")
                        }
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "❌ Error mapping message document", e)
                        null
                    }
                }
                
                Log.d("ChatRepository", "📬 Total messages: ${messages.size}")
                onMessagesUpdated(messages)
            }
    }




}
