package com.eritlab.jexmon.presentation.graphs.home_graph

sealed class ShopHomeScreen(val route: String) {
    object DashboardScreen : ShopHomeScreen("dashboard_screen")
    object ConversationScreen : ShopHomeScreen("conversation_screen") // Chat mới
    object ProfileScreen : ShopHomeScreen("profile_screen")
    object FavouriteScreen : ShopHomeScreen("favourite_screen")
    object ChatListScreen : ShopHomeScreen("chat_list_screen")
    object BrandsHomeScreen : ShopHomeScreen("brands_home_screen/{category}") {
        fun createRoute(category: String) = "brands_home_screen/$category"
    }

    // Với chat cũ, dùng route có id
    object ExistingConversationScreen : ShopHomeScreen("conversation_screen/{chatId}") {
        fun createRoute(chatId: String) = "conversation_screen/$chatId"
    }
}
