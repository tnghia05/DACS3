package com.eritlab.jexmon.presentation.graphs.home_graph

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.graphs.admin_graph.adminNavGraph
import com.eritlab.jexmon.presentation.graphs.detail_graph.DetailScreen
import com.eritlab.jexmon.presentation.graphs.detail_graph.detailNavGraph
import com.eritlab.jexmon.presentation.screens.brands.BrandsHome
import com.eritlab.jexmon.presentation.screens.chat_list_screen.ChatListScreen
import com.eritlab.jexmon.presentation.screens.conversation_screen.component.ChatRepository
import com.eritlab.jexmon.presentation.screens.conversation_screen.component.ConversationScreen
import com.eritlab.jexmon.presentation.screens.dashboard_screen.component.DashboardScreen
import com.eritlab.jexmon.presentation.screens.favourite_screen.component.FavouriteScreen
import com.eritlab.jexmon.presentation.screens.profile_screen.component.ProfileScreen

@Composable
fun HomeNavGraph(navHostController: NavHostController) {
    val navController = navHostController
    NavHost(
        navController = navHostController,
        route = Graph.HOME,
        startDestination = ShopHomeScreen.DashboardScreen.route

    ) {
        detailNavGraph(navController = navHostController)
        adminNavGraph(navHostController = navHostController)

        composable(ShopHomeScreen.DashboardScreen.route) {
            DashboardScreen(
                onItemClick = { productId ->
                    navHostController.navigate("${DetailScreen.ProductDetailScreen.route}/$productId")
                },
                onBannerClick = { category ->
                    // Navigate to brands screen when banner is clicked
                    navHostController.navigate(ShopHomeScreen.BrandsHomeScreen.createRoute(category))
                }
            )
        }
        composable(ShopHomeScreen.FavouriteScreen.route) {
            FavouriteScreen(

                onProductClick = { productId ->
                    navController.navigate(ShopHomeScreen.ExistingConversationScreen.createRoute(productId))
                },
                onCartClick = { _ ->
                    navHostController.navigate(DetailScreen.CartScreen.route)
                }
            )
        }
        composable(ShopHomeScreen.ChatListScreen.route) {
          ChatListScreen(
                navController = navHostController
            )
        }
            
        composable(ShopHomeScreen.ProfileScreen.route) {
            ProfileScreen(
                navController = navHostController,
                onBackBtnClick = { navHostController.popBackStack() },

            )
        }

        composable(ShopHomeScreen.ConversationScreen.route) {
            ConversationScreen(
                navController = navController,
                idChat = "", // Chat mới
                chatRepository = ChatRepository()
            )
        }
        composable(
            route = "brands_home_screen/{categoryId}",
            arguments = listOf(navArgument("categoryId") {
                defaultValue = "all" // nếu không truyền thì mặc định là "all"
            })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "all"
            BrandsHome(category = category, onItemClick = { selectedCategory ->
                navController.navigate("brands_home_screen/$selectedCategory")
            })
        }


        composable(
            route = ShopHomeScreen.ExistingConversationScreen.route,
            arguments = listOf(navArgument("chatId") { defaultValue = "" })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ConversationScreen(
                navController = navController,
                idChat = chatId,
                chatRepository = ChatRepository()
            )
        }
         
        


    }
}
