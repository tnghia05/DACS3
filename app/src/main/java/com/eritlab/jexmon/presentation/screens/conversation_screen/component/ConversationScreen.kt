package com.eritlab.jexmon.presentation.screens.conversation_screen.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.eritlab.jexmon.R
import com.eritlab.jexmon.domain.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ErrorSuggestion(message: String, onDismiss: () -> Unit = {}) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
@Composable
fun MessageBubble(message: Message) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isFromUser) {
            Image(
                painter = painterResource(id = R.drawable.user),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                ),
                color = if (message.isFromUser) Color(0xFF0084FF) else Color(0xFFE4E6EB),
                elevation = 0.dp
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(12.dp),
                    color = if (message.isFromUser) Color.White else Color.Black
                )
            }

            Text(
                text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(message.timestamp)),
                style = MaterialTheme.typography.caption,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 2.dp)
            )
        }
    }
}





@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatRepository: ChatRepository,
    navController: NavController,
    idChat: String,
) {
    val sampleMessages = listOf(
        Message(
            text = "Xin chào! Tôi có thể giúp gì cho bạn?",
            isFromUser = false,
            timestamp = System.currentTimeMillis() - 3600000 // 1 giờ trước
        ),
        Message(
            text = "Tôi muốn tìm một số sản phẩm thời trang",
            isFromUser = true,
            timestamp = System.currentTimeMillis() - 1800000 // 30 phút trước
        ),
        Message(
            text = "Vâng, chúng tôi có nhiều mẫu thời trang mới. Bạn quan tâm đến loại nào?",
            isFromUser = false,
            timestamp = System.currentTimeMillis() - 900000 // 15 phút trước
        ),
        Message(
            text = "Tôi đang tìm áo khoác nam",
            isFromUser = true,
            timestamp = System.currentTimeMillis() - 600000 // 10 phút trước
        ),
        Message(
            text = "Chúng tôi có nhiều mẫu áo khoác nam đang hot. Để tôi gửi cho bạn một số gợi ý nhé!",
            isFromUser = false,
            timestamp = System.currentTimeMillis() - 300000 // 5 phút trước
        ),
        Message(
            text = "Cảm ơn bạn! Tôi sẽ xem xét.",
            isFromUser = true,
            timestamp = System.currentTimeMillis() - 100000 // 1 phút trước
        ),
        Message(
            text = "Không có gì! Nếu bạn cần thêm thông tin, hãy cho tôi biết nhé!",
            isFromUser = false,
            timestamp = System.currentTimeMillis() // Hiện tại
        )
        ,
        Message(
            text = "Tôi muốn tìm một số sản phẩm thời trang",
            isFromUser = true,
            timestamp = System.currentTimeMillis() - 1800000 // 30 phút trước
        ),
    )
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var chatIdState by remember { mutableStateOf(idChat) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        Log.d("ConversationScreen", "🔍 TextField state - inputText: $inputText")
        Log.d("ConversationScreen", "🔍 TextField visibility check")
        currentUser?.let { user ->
            try {
                if (idChat.isBlank()) {
                    chatRepository.createNewChat(user.uid).fold(
                        onSuccess = { newChatId ->
                            Log.d("Chat", "✅ Created new chat with ID: $newChatId")
                            chatIdState = newChatId
                            messages = emptyList()
                        },
                        onFailure = { e ->
                            errorMessage = "Failed to create new chat: ${e.message}"
                        }
                    )
                } else {
                    // Sử dụng ChatRepository để lắng nghe tin nhắn
                    chatRepository.listenForMessages(idChat) { newMessages ->
                        messages = newMessages
                        Log.d("ConversationScreen", "📜 Updated messages: ${messages.size}")
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load chat history: ${e.message}"
            }
        } ?: run {
            errorMessage = "Please sign in to chat"
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(text = chatIdState, style = MaterialTheme.typography.h6) },
                    navigationIcon = {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Call, "Call")
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                    }
                )

                // Chat messages area

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(560.dp)
                ) {

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        reverseLayout = false
                    ) {
                        items(messages) { message ->
                            MessageBubble(message)
                            Log.d("ConversationScreen", "📜 Message: ${message.text} - From User: ${message.isFromUser}")
                            Log.d("ConversationScreen", "📜 Timestamp: ${message.timestamp}")
                            Log.d("ConversationScreen", "📜 Nguoi gui: ${message.isFromUser}")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                
                // Debug log for messages state
                LaunchedEffect(messages) {
                    Log.d("ConversationScreen", "📱 Current messages count: ${messages.size}")
                    messages.forEach { message ->
                        Log.d("ConversationScreen", "📝 Message: ${message.text} - From User: ${message.isFromUser}")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colors.surface, RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Type a message...") },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colors.primary
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (inputText.isBlank()) {
                                    errorMessage = "Vui lòng nhập tin nhắn"
                                    return@IconButton
                                }

                                val userMessage = inputText.trim()
                                messages = messages + Message(userMessage, true)
                                inputText = ""
                                isLoading = true
                                errorMessage = null

                                CoroutineScope(Dispatchers.Main).launch {
                                    val user = currentUser

                                    if (user == null) {
                                        errorMessage = "Please sign in to chat"
                                        isLoading = false
                                        return@launch
                                    }

                                    if (chatIdState.isBlank()) {
                                        errorMessage = "No chat selected"
                                        isLoading = false
                                        return@launch
                                    }

                                    val sendResult = chatRepository.sendUserMessage(
                                        chatIdState,
                                        user.uid,
                                        userMessage
                                    )

                                    if (sendResult.isFailure) {
                                        errorMessage = sendResult.exceptionOrNull()?.message
                                            ?: "Failed to send"
                                        isLoading = false
                                        return@launch
                                    }

                                    val thinkingMessage = Message("...", isFromUser = false)
                                    messages = messages + thinkingMessage

                                    kotlinx.coroutines.delay(1000L)

                                    val botResult = chatRepository.getBotReplyAndSave(
                                        chatIdState,
                                        user.uid,
                                        userMessage
                                    )
                                    botResult.fold(
                                        onSuccess = { response ->
                                            val botMessage =
                                                response["reply"] as? String ?: "Bot didn't reply"
                                            messages = messages.dropLast(1)
                                            messages = messages + Message(botMessage, false)
                                        },
                                        onFailure = { e ->
                                            messages = messages.dropLast(1)
                                            errorMessage = e.message ?: "Bot error"
                                        }
                                    )

                                    isLoading = false
                                }
                            }
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send")
                        }
                    }
                }

                errorMessage?.let {
                    ErrorSuggestion(message = it)
                }
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ConversationScreenPreview() {
    val sampleMessages = listOf(
        Message(
            text = "Xin chào! Tôi có thể giúp gì cho bạn?",
            isFromUser = false,
            timestamp = System.currentTimeMillis() - 3600000 // 1 giờ trước
        ),
        Message(
            text = "Tôi muốn tìm một số sản phẩm thời trang",
            isFromUser = true,
            timestamp = System.currentTimeMillis() - 1800000 // 30 phút trước
        ),
        Message(
            text = "Vâng, chúng tôi có nhiều mẫu thời trang mới. Bạn quan tâm đến loại nào?",
            isFromUser = false,
            timestamp = System.currentTimeMillis() - 900000 // 15 phút trước
        ),
        Message(
            text = "Tôi đang tìm áo khoác nam",
            isFromUser = true,
            timestamp = System.currentTimeMillis() - 600000 // 10 phút trước
        ),
        Message(
            text = "Chúng tôi có nhiều mẫu áo khoác nam đang hot. Để tôi gửi cho bạn một số gợi ý nhé!",
            isFromUser = false,
            timestamp = System.currentTimeMillis() - 300000 // 5 phút trước
        ),
        Message(
            text = "Cảm ơn bạn! Tôi sẽ xem xét.",
            isFromUser = true,
            timestamp = System.currentTimeMillis() - 100000 // 1 phút trước
        ),
        Message(
            text = "Không có gì! Nếu bạn cần thêm thông tin, hãy cho tôi biết nhé!",
            isFromUser = false,
            timestamp = System.currentTimeMillis() // Hiện tại
        )
        ,
        Message(
            text = "Tôi muốn tìm một số sản phẩm thời trang",
            isFromUser = true,
            timestamp = System.currentTimeMillis() - 1800000 // 30 phút trước
        ),
    )

    MaterialTheme {
        Surface {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(text = "Preview Chat", style = MaterialTheme.typography.h6) },
                    navigationIcon = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Call, "Call")
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.MoreVert, "More")
                        }
                    }
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(16.dp),
                    reverseLayout = true
                ) {
                    items(sampleMessages) { message ->
                        MessageBubble(message)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colors.surface, RoundedCornerShape(24.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = "",
                        onValueChange = { },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Nhập tin nhắn...") },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    }
}
