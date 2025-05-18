package com.eritlab.jexmon.presentation.graphs.home_graph

sealed class ShopHomeScreen(val route: String) {
    object DashboardScreen : ShopHomeScreen("dashboard_screen")
    object FavouriteScreen : ShopHomeScreen("favourite_screen")
    object ChatListScreen : ShopHomeScreen("chat_list_screen")
    object ProfileScreen : ShopHomeScreen("profile_screen")
    object DonMuaScreen : ShopHomeScreen("don_mua_screen")
    object OrderDetailScreen : ShopHomeScreen("order_detail_screen/{orderId}") {
        fun createRoute(orderId: String) = "order_detail_screen/$orderId"
    }
    object BrandsHomeScreen : ShopHomeScreen("brands_home_screen/{category}") {
        fun createRoute(category: String) = "brands_home_screen/$category"
    }
    object ProductsHomeScreen : ShopHomeScreen("products_home_screen/{category}") {
        fun createRoute(category: String) = "products_home_screen/$category"
    }
    object ProductsHome : ShopHomeScreen("products_home") {
        fun createRoute(minPrice: Double, maxPrice: Double, brand: String) = "products_home?minPrice=$minPrice&maxPrice=$maxPrice&brand=$brand"
    }
    // Với chat cũ, dùng route có id
    object ExistingConversationScreen : ShopHomeScreen("conversation_screen/{chatId}") {
        fun createRoute(chatId: String) = "conversation_screen/$chatId"
    }
}
