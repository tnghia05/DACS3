package com.eritlab.jexmon.presentation.screens.chat_list_screen

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.eritlab.jexmon.R
import com.eritlab.jexmon.presentation.graphs.home_graph.ShopHomeScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun ChatListScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    var chatList by remember { mutableStateOf<List<ChatPreview>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (currentUser != null) {
            firestore.collection("chats")
                .whereArrayContains("participants", currentUser.uid)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("ChatList", "Error fetching chats", error)
                        return@addSnapshotListener
                    }

                    val newChats = snapshot?.documents?.mapNotNull { doc ->
                        try {
                            ChatPreview(
                                id = doc.id,
                                name = doc.getString("name") ?: "Unknown",
                                lastMessage = doc.getString("lastMessage") ?: "",
                                time = doc.getTimestamp("lastMessageTime")?.toDate()?.toString() ?: "",
                                unreadCount = doc.getLong("unreadCount")?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            Log.e("ChatList", "Error parsing chat", e)
                            null
                        }
                    } ?: emptyList()
                    
                    chatList = newChats
                }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.fillMaxSize()) { // 👈 phải fillMaxSize ở đây
            TopAppBar(
                title = { 
                    Text(
                        text = "Chats",
                        style = MaterialTheme.typography.h6
                    )
                },
                backgroundColor = MaterialTheme.colors.surface,
                elevation = 0.dp
            )
    
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // 👈 QUAN TRỌNG: để tránh lỗi infinite height
            ) {
                items(chatList) { chat ->
                    ChatListItem(
                        chat = chat,
                        onItemClick = {
                            navController.navigate("conversation_screen/${chat.id}")
                        }
                    )
                    Divider(thickness = 0.5.dp)
                }
            }
        }

        FloatingActionButton(
            onClick = {
                navController.navigate(ShopHomeScreen.ConversationScreen.route)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.chat_bubble_icon),
                contentDescription = "Add Chat",
                modifier = Modifier.size(24.dp)
            )

        }
    }
    
}

@Composable
fun ChatListItem(
    chat: ChatPreview,
    onItemClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.user),
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chat.name,
                    style = MaterialTheme.typography.subtitle1
                )
                Text(
                    text = chat.lastMessage,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                )
            }
            
            Text(
                text = chat.time,
                style = MaterialTheme.typography.caption,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

data class ChatPreview(
    val id: String,
    val name: String,
    val lastMessage: String,
    val time: String,
    val unreadCount: Int = 0
)