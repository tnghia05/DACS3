package com.eritlab.jexmon.presentation.screens.home_screen

import com.eritlab.jexmon.R
import com.eritlab.jexmon.presentation.graphs.home_graph.ShopHomeScreen

sealed class BottomNavItem(val tittle: String, val icon: Int, val route: String) {
    object HomeNav : BottomNavItem(
        tittle = "Home",
        icon = R.drawable.bill_icon,
        route = ShopHomeScreen.DashboardScreen.route
    )

    object FavouriteNav : BottomNavItem(
        tittle = "Brands",
        icon = R.drawable.lgoo,
        route =ShopHomeScreen.BrandsHomeScreen.createRoute("all")

    )

    object ChatNav : BottomNavItem(
        tittle = "Chat",
        icon = R.drawable.chat_bubble_icon,
        route = ShopHomeScreen.ChatListScreen.route
    )

    object ProfileNav : BottomNavItem(
        tittle = "Profile",
        icon = R.drawable.user_icon,
        route = ShopHomeScreen.ProfileScreen.route
    )
}
