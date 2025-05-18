package com.eritlab.jexmon.presentation.screens.conversation_screen.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.eritlab.jexmon.R
import com.eritlab.jexmon.domain.model.Message
import com.google.firebase.auth.FirebaseAuth
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
fun ProductCard(
    message: Message,
    isFromUser: Boolean,
    navController: NavController,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val messageText = message.text
    
    // Memoize the sections to prevent unnecessary recomposition
    val sections = remember(messageText) {
        messageText.split("[PRODUCT_START]")
    }

    // Extract productId from message text
    var extractedProductId by remember { mutableStateOf("") }
    
    LaunchedEffect(messageText) {
        // Tìm productId từ link trong message
        val linkPattern = Regex("/product/([^\\s]+)")
        linkPattern.find(messageText)?.let { matchResult ->
            extractedProductId = matchResult.groupValues[1]
        }
    }
    
    Column(
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth(0.85f),
        horizontalAlignment = if (isFromUser) Alignment.End else Alignment.Start
    ) {
        // Display text before first product
        if (sections.isNotEmpty() && sections[0].isNotEmpty()) {
            val beforeText = sections[0].trim()
            Text(
                text = beforeText,
                style = MaterialTheme.typography.body1,
                color = if (isFromUser) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Process each product section with memoization
        sections.drop(1).forEach { section ->
            val productParts = remember(section) {
                section.split("[PRODUCT_END]")
            }
            
            if (productParts.isNotEmpty()) {
                val productSection = productParts[0].trim()
                
                // Memoize the product card item
                ProductCardItem(
                    productSection = productSection,
                    onProductClick = { productId ->
                        onProductClick(productId)
                    }
                )
                
                if (productParts.size > 1 && productParts[1].isNotEmpty()) {
                    val afterText = productParts[1].trim()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = afterText,
                        style = MaterialTheme.typography.body1,
                        color = if (isFromUser) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // View Detail Button

    }
}

@Composable
private fun ProductCardItem(
    productSection: String,
    onProductClick: (String) -> Unit
) {
    var productId by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var productName by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var discount by remember { mutableStateOf("") }
    var sizes by remember { mutableStateOf("") }
    var colors by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var sold by remember { mutableStateOf("") }
    var features by remember { mutableStateOf(mapOf<String, List<String>>()) }

    // Parse product information
    LaunchedEffect(productSection) {
        var currentCategory = ""
        var currentFeatures = mutableListOf<String>()
        
        productSection.split("\n").forEach { line ->
            when {
                line.startsWith("Tên:") -> productName = line.substringAfter("Tên:").trim()
                line.startsWith("Hình ảnh:") -> imageUrl = line.substringAfter("Hình ảnh:").trim()
                line.startsWith("Link:") -> productId = line.substringAfter("/product/").trim()
                line.startsWith("Giá:") -> price = line.substringAfter("Giá:").trim()
                line.startsWith("Giảm giá:") -> discount = line.substringAfter("Giảm giá:").trim()
                line.startsWith("Size:") -> sizes = line.substringAfter("Size:").trim()
                line.startsWith("Màu:") -> colors = line.substringAfter("Màu:").trim()
                line.startsWith("Đánh giá:") -> rating = line.substringAfter("Đánh giá:").trim()
                line.startsWith("Đã bán:") -> sold = line.substringAfter("Đã bán:").trim()
                line.startsWith("🔹") -> {
                    // Save previous category if exists
                    if (currentCategory.isNotEmpty() && currentFeatures.isNotEmpty()) {
                        features = features + (currentCategory to currentFeatures)
                    }
                    // Start new category
                    currentCategory = line.substringAfter("🔹").substringBefore(":").trim()
                    currentFeatures = mutableListOf()
                }
                line.startsWith("   •") -> {
                    currentFeatures.add(line.substringAfter("•").trim())
                }
            }
        }
        // Add last category
        if (currentCategory.isNotEmpty() && currentFeatures.isNotEmpty()) {
            features = features + (currentCategory to currentFeatures)
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { if (productId.isNotEmpty()) onProductClick(productId) },
        elevation = 4.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colors.surface)
        ) {
            // Product Image
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                ) {
                                    var isLoading by remember { mutableStateOf(true) }
                                    var isError by remember { mutableStateOf(false) }
                                    
                                    AsyncImage(
                                        model = imageUrl,
                    contentDescription = productName,
                    modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        onSuccess = { isLoading = false },
                                        onError = { 
                                            isLoading = false
                                            isError = true 
                                        }
                                    )
                                    
                                    if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colors.primary
                                        )
                    }
                                    }
                                    
                                    if (isError) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_error),
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Discount badge
                if (discount.isNotEmpty()) {
                    Surface(
                        color = Color.Red,
                        shape = RoundedCornerShape(bottomEnd = 12.dp),
                        modifier = Modifier.align(Alignment.TopStart)
                    ) {
                                        Text(
                            text = discount,
                            color = Color.White,
                            style = MaterialTheme.typography.caption,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Product Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.h6,
                    maxLines = 2,
                    color = MaterialTheme.colors.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Price and Rating Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.subtitle1,
                        color = MaterialTheme.colors.primary
                    )
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_star),
                            contentDescription = "Rating",
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = rating,
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        Text(
                            text = " | $sold đã bán",
                            style = MaterialTheme.typography.caption,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Size and Color
                if (sizes.isNotEmpty()) {
                    Text(
                        text = "Size: $sizes",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                            }
                
                if (colors.isNotEmpty()) {
                    Text(
                        text = "Màu sắc: $colors",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f)
                    )
                }

                // Features Section
                if (features.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "📋 ĐẶC ĐIỂM NỔI BẬT:",
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.onSurface
                            )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    features.forEach { (category, featureList) ->
                        Text(
                            text = "🔹 $category:",
                                style = MaterialTheme.typography.subtitle2,
                            color = MaterialTheme.colors.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        featureList.forEach { feature ->
                            Text(
                                text = "   • $feature",
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.8f),
                                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    navController: NavController,
    onProductClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = if (message.isFromUser) Alignment.End else Alignment.Start
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            if (!message.isFromUser) {
                Image(
                    painter = painterResource(id = R.drawable.user),
                    contentDescription = "AI Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.surface)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromUser) 4.dp else 16.dp
                ),
                color = if (message.isFromUser) MaterialTheme.colors.primary else MaterialTheme.colors.surface,
                elevation = 1.dp,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                ProductCard(
                    message = message,
                    isFromUser = message.isFromUser,
                    navController = navController,
                    onProductClick = { productId ->
                        Log.d("MessageBubble", "Product click received: $productId")
                        onProductClick(productId)
                    }
                )
            }

            if (message.isFromUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    painter = painterResource(id = R.drawable.user),
                    contentDescription = "User Avatar",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.surface)
                )
            }
        }

        // Timestamp
        Text(
            text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date(message.timestamp)),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
            modifier = Modifier
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .align(if (message.isFromUser) Alignment.End else Alignment.Start)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    chatRepository: ChatRepository,
    navController: NavController,
    idChat: String,
    onNavigateToProduct: (String) -> Unit = {}
) {
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var chatIdState by remember { mutableStateOf(idChat) }
    var retryCount by remember { mutableStateOf(0) }
    var lastFailedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var isInitialized by remember { mutableStateOf(false) }
    
    // Retrieve chat ID from saved state handle when returning from product detail
    LaunchedEffect(Unit) {
        if (isInitialized) return@LaunchedEffect
        
        val savedChatId = navController.currentBackStackEntry?.savedStateHandle?.get<String>("active_chat_id")
        Log.d("ConversationScreen", "Attempting to restore chat ID. Saved: $savedChatId, Current: $chatIdState")
        
        if (!savedChatId.isNullOrEmpty()) {
            Log.d("ConversationScreen", "Restoring chat ID: $savedChatId")
            chatIdState = savedChatId
            isInitialized = true
        } else if (chatIdState.isEmpty()) {
            Log.d("ConversationScreen", "No saved chat ID found, creating new chat")
            // Will create new chat in the initialization block below
        }
    }

    // Store chat ID in NavController's saved state handle whenever it changes
    LaunchedEffect(chatIdState) {
        if (chatIdState.isNotEmpty()) {
            Log.d("ConversationScreen", "Saving chat ID to state handle: $chatIdState")
            navController.currentBackStackEntry?.savedStateHandle?.set("active_chat_id", chatIdState)
        }
    }

    // Add pagination state
    var isLoadingMore by remember { mutableStateOf(false) }
    var hasMoreMessages by remember { mutableStateOf(true) }
    val pageSize = 20 // Number of messages per page
    
    val listState = rememberLazyListState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Function to handle errors and retries
    fun handleError(error: Exception, action: () -> Unit) {
        errorMessage = when {
            error.message?.contains("network") == true -> 
                "Không thể kết nối mạng. Vui lòng kiểm tra kết nối và thử lại."
            error.message?.contains("permission") == true -> 
                "Bạn không có quyền thực hiện hành động này."
            error.message?.contains("not found") == true -> 
                "Không tìm thấy dữ liệu yêu cầu."
            else -> error.message ?: "Đã xảy ra lỗi. Vui lòng thử lại."
        }
        lastFailedAction = action
        retryCount++
    }

    // Function to retry last failed action
    fun retryLastAction() {
        lastFailedAction?.let { action ->
            scope.launch {
                try {
                    action()
                    errorMessage = null
                    lastFailedAction = null
                    retryCount = 0
                } catch (e: Exception) {
                    handleError(e, action)
                }
            }
        }
    }

    // Error dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                errorMessage = null 
                lastFailedAction = null
                retryCount = 0
            },
            title = { Text("Lỗi") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                if (retryCount < 3 && lastFailedAction != null) {
                    TextButton(onClick = { retryLastAction() }) {
                        Text("Thử lại")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    errorMessage = null
                    lastFailedAction = null
                    retryCount = 0
                }) {
                    Text("Đóng")
                }
            }
        )
    }

    // Function to handle product navigation
    fun navigateToProduct(productId: String) {
        try {
            Log.d("Navigation", "ConversationScreen: Attempting to navigate to product: $productId")
            onNavigateToProduct(productId)
        } catch (e: Exception) {
            Log.e("Navigation", "ConversationScreen: Navigation error: ${e.message}", e)
            handleError(Exception("Không thể mở chi tiết sản phẩm. Vui lòng thử lại sau.")) { 
                navigateToProduct(productId) 
            }
        }
    }

    // Load more messages when scrolling up
    LaunchedEffect(listState) {
        if (listState.firstVisibleItemIndex <= 1 && !isLoadingMore && hasMoreMessages) {
            isLoadingMore = true
            try {
                val oldestMessageTimestamp = messages.minByOrNull { it.timestamp }?.timestamp ?: Long.MAX_VALUE
                val olderMessages = chatRepository.loadMoreMessages(chatIdState, oldestMessageTimestamp, pageSize)
                if (olderMessages.isNotEmpty()) {
                    messages = (messages + olderMessages)
                        .distinctBy { it.messageId }
                        .sortedBy { it.timestamp }
                } else {
                    hasMoreMessages = false
                }
            } catch (e: Exception) {
                handleError(e) {
                    isLoadingMore = false
                    hasMoreMessages = true
                }
            } finally {
                isLoadingMore = false
            }
        }
    }

    LaunchedEffect(Unit) {
        Log.d("ConversationScreen", "🔍 Initializing chat screen")
        currentUser?.let { user ->
            try {
                if (chatIdState.isBlank()) {
                    when (val result = chatRepository.createNewChat(user.uid)) {
                        is AIResult.Success -> {
                            Log.d("Chat", "✅ Created new chat with ID: ${result.data}")
                            chatIdState = result.data
                            messages = emptyList()
                        }
                        is AIResult.Error -> {
                            errorMessage = "Failed to create new chat: ${result.exception.message}"
                        }
                    }
                }
                
                // Load initial messages
                chatRepository.listenForMessages(chatIdState, pageSize) { newMessages ->
                    messages = newMessages.distinctBy { it.messageId }.sortedBy { it.timestamp }
                }
            } catch (e: Exception) {
                errorMessage = "Failed to load chat history: ${e.message}"
            }
        } ?: run {
            errorMessage = "Please sign in to chat"
        }
    }

    // Handle sending message
    fun sendMessage(text: String) {
        if (text.isBlank()) {
            errorMessage = "Vui lòng nhập tin nhắn"
            return
        }

        Log.d("User_Message", "👤 User: $text")
        inputText = ""
        isLoading = true
        errorMessage = null

        CoroutineScope(Dispatchers.Main).launch {
            val user = currentUser
            if (user == null) {
                errorMessage = "Vui lòng đăng nhập để chat"
                isLoading = false
                return@launch
            }

            if (chatIdState.isBlank()) {
                errorMessage = "Không tìm thấy cuộc trò chuyện"
                isLoading = false
                return@launch
            }

            when (val sendResult = chatRepository.sendUserMessage(
                chatIdState,
                user.uid,
                text
            )) {
                is AIResult.Error -> {
                    errorMessage = sendResult.exception.message ?: "Không thể gửi tin nhắn"
                    isLoading = false
                    return@launch
                }
                is AIResult.Success -> {
                    when (val botResult = chatRepository.getBotReplyAndSave(
                        chatIdState,
                        user.uid,
                        text
                    )) {
                        is AIResult.Success -> {
                            Log.d("AI_Processing", "🤖 AI đang xử lý tin nhắn...")
                        }
                        is AIResult.Error -> {
                            errorMessage = botResult.exception.message ?: "Không thể nhận phản hồi từ bot"
                            Log.e("AI_Error", "❌ Lỗi AI: ${botResult.exception.message}")
                        }
                    }
                }
            }
            isLoading = false
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = { Text("AI Assistant") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Test Image

            // Chat messages area
            Box(
                modifier = Modifier
                    .height(550.dp)
                    .fillMaxWidth()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .height(550.dp)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    reverseLayout = false,
                    state = listState,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (hasMoreMessages) {
                        item {
                            if (isLoadingMore) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = MaterialTheme.colors.primary
                                    )
                                }
                            }
                        }
                    }
                    
                    items(
                        items = messages,
                        key = { message -> message.messageId } // Use messageId as unique key
                    ) { message ->
                        MessageBubble(
                            message = message,
                            navController = navController,
                            onProductClick = { productId -> navigateToProduct(productId) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            // Input area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colors.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        placeholder = { Text("Hãy hỏi tôi về sản phẩm...") },
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        enabled = !isLoading,
                        maxLines = 3
                    )

                    IconButton(
                        onClick = { sendMessage(inputText.trim()) },
                        enabled = !isLoading,
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (!isLoading) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Gửi",
                            tint = if (!isLoading) Color.White else MaterialTheme.colors.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}


