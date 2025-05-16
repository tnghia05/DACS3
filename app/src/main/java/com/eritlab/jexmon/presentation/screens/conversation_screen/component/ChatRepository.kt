package com.eritlab.jexmon.presentation.screens.conversation_screen.component

import android.util.Log
import com.eritlab.jexmon.domain.model.Message
import com.eritlab.jexmon.di.JexmonApplication
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val functions = FirebaseFunctions.getInstance("us-central1")
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val appCheck = FirebaseAppCheck.getInstance()

    suspend fun createNewChat(userId: String): Result<String> {
        return try {
            // Kiểm tra xác thực
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("Vui lòng đăng nhập để tiếp tục"))
            }

            // Tạo chat mới
            val chatData = mapOf(
                "participants" to listOf(userId, "AI_ASSISTANT"),
                "name" to "AI Assistant",
                "lastMessage" to "",
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "unreadCount" to 0,
                "ownerId" to userId
            )

            val chatRef = db.collection("chats").document()
            chatRef.set(chatData).await()

            Result.success(chatRef.id)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error creating new chat", e)
            Result.failure(e)
        }
    }

    suspend fun sendUserMessage(chatId: String, userId: String, message: String): Result<Unit> {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("Vui lòng đăng nhập để tiếp tục"))
            }

            // Đợi App Check khởi tạo với thời gian chờ tăng dần
            var waitTime = 1000L
            var attempts = 0
            val maxAttempts = 3

            while (!JexmonApplication.isAppCheckInitialized() && attempts < maxAttempts) {
                delay(waitTime)
                waitTime *= 2
                attempts++
                Log.d(TAG, "Đang đợi App Check khởi tạo, lần thử $attempts")
            }

            if (!JexmonApplication.isAppCheckInitialized()) {
                JexmonApplication.resetAppCheckState()
                return Result.failure(Exception("Không thể xác thực ứng dụng. Vui lòng thử lại sau."))
            }

            // Lấy token App Check mới
            val appCheckToken = try {
                appCheck.getToken(true).await()
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi lấy token App Check", e)
                return Result.failure(Exception("Lỗi xác thực. Vui lòng thử lại sau."))
            }

            // Lưu tin nhắn người dùng
            val chatRef = db.collection("chats").document(chatId)
            chatRef.collection("messages").add(
                mapOf(
                    "senderId" to userId,
                    "message" to message,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "from" to "user"
                )
            ).await()

            // Cập nhật thông tin chat
            chatRef.set(
                mapOf(
                    "lastMessage" to message,
                    "lastMessageTime" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            ).await()

            // Gọi Cloud Function với token mới
            val data = hashMapOf(
                "message" to message,
                "appCheckToken" to appCheckToken.token
            )
            
            try {
                val result = functions
                    .getHttpsCallable("processChat")
                    .call(data)
                    .await()

                val response = result.data as? Map<*, *>
                val botReply = response?.get("reply") as? String 
                    ?: "Xin lỗi, tôi không thể xử lý yêu cầu lúc này"

                // Lưu phản hồi của bot
                chatRef.collection("messages").add(
                    mapOf(
                        "senderId" to "AI_ASSISTANT",
                        "message" to botReply,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "from" to "bot"
                    )
                ).await()

                // Cập nhật thông tin chat với phản hồi của bot
                chatRef.set(
                    mapOf(
                        "lastMessage" to botReply,
                        "lastMessageTime" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi xử lý phản hồi của bot", e)
                
                val errorMessage = when {
                    e.message?.contains("Too many attempts") == true -> 
                        "Hệ thống đang xử lý nhiều yêu cầu. Vui lòng thử lại sau ít phút."
                    e.message?.contains("Unauthenticated") == true -> {
                        JexmonApplication.resetAppCheckState()
                        "Phiên đăng nhập đã hết hạn. Vui lòng thử lại."
                    }
                    e.message?.contains("resource-exhausted") == true -> 
                        "Hệ thống đang tạm thời quá tải. Vui lòng thử lại sau."
                    e.message?.contains("permission-denied") == true -> 
                        "Bạn không có quyền thực hiện thao tác này."
                    e.message?.contains("not-found") == true -> 
                        "Không tìm thấy thông tin yêu cầu."
                    e.message?.contains("failed-precondition") == true -> {
                        JexmonApplication.resetAppCheckState()
                        "Lỗi xác thực ứng dụng. Vui lòng thử lại sau."
                    }
                    e.message?.contains("network") == true ->
                        "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối và thử lại."
                    else -> "Đã có lỗi xảy ra. Vui lòng thử lại."
                }

                chatRef.collection("messages").add(
                    mapOf(
                        "senderId" to "AI_ASSISTANT",
                        "message" to errorMessage,
                        "timestamp" to FieldValue.serverTimestamp(),
                        "from" to "error"
                    )
                ).await()

                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi gửi tin nhắn", e)
            Result.failure(Exception("Không thể gửi tin nhắn. Vui lòng thử lại sau."))
        }
    }

    suspend fun getBotReplyAndSave(
        chatId: String,
        userId: String,
        userMessage: String
    ): Result<Map<String, Any>> {
        return try {
            // Kiểm tra xác thực
            val user = auth.currentUser
            if (user == null) {
                return Result.failure(Exception("Vui lòng đăng nhập để tiếp tục"))
            }

            // Lấy token App Check
            val appCheckToken = appCheck.getToken(false).await()

            // Gọi Cloud Function
            val data = mapOf(
                "message" to userMessage,
                "appCheckToken" to appCheckToken.token
            )

            val result = functions.getHttpsCallable("processChat")
                .call(data)
                .await()

            val responseData = result.data
            val responseMap = responseData as? Map<*, *>
            val botReply = responseMap?.get("reply") as? String ?: "Không có phản hồi"

            // Lưu phản hồi vào Firestore
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

            @Suppress("UNCHECKED_CAST")
            Result.success(responseMap as? Map<String, Any> ?: mapOf("reply" to botReply))
        } catch (e: Exception) {
            Log.e("ChatRepository", "Error getting bot reply", e)
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
        return db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatRepository", "Error listening for messages", error)
                    return@addSnapshotListener
                }
                
                if (snapshot == null) {
                    Log.d("ChatRepository", "Snapshot is null")
                    return@addSnapshotListener
                }

                val messages = snapshot.documents.mapNotNull { doc ->
                    try {
                        Message(
                            text = doc.getString("message") ?: "",
                            isFromUser = doc.getString("from") == "user",
                            timestamp = doc.getTimestamp("timestamp")?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        Log.e("ChatRepository", "Error mapping message", e)
                        null
                    }
                }
                
                onMessagesUpdated(messages)
            }
    }

    companion object {
        private const val TAG = "ChatRepository"
    }
}
