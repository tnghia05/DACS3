package com.eritlab.jexmon.presentation.graphs.home_graph

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
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
import com.eritlab.jexmon.presentation.screens.don_da_mua.DonMua
import com.eritlab.jexmon.presentation.screens.don_da_mua.OrderDetailScreen
import com.eritlab.jexmon.presentation.screens.favourite_screen.component.FavouriteScreen
import com.eritlab.jexmon.presentation.screens.products.ProductsHome
import com.eritlab.jexmon.presentation.screens.profile_screen.component.EditProfileScreen
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
        composable(ShopHomeScreen.DonMuaScreen.route){
            DonMua (
                navController = navHostController,
                onBackBtnClick = { navHostController.popBackStack() },

                )
        }

        composable(
            route = ShopHomeScreen.OrderDetailScreen.route,
            arguments = listOf(navArgument("orderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            OrderDetailScreen(
                navController = navHostController,
                orderId = orderId,
                onBackClick = { navHostController.popBackStack() }
            )
        }

        composable(ShopHomeScreen.ConversationScreen.route) {
            ConversationScreen(
                navController = navController,
                idChat = "", // Chat mới
                chatRepository = ChatRepository()
            )
        }

        composable("products?brand={brand}&sort={sort}") { backStackEntry ->
            val brand = backStackEntry.arguments?.getString("brand") ?: "all" // Lấy giá trị brand, mặc định là "all"
            val sort = backStackEntry.arguments?.getString("sort") ?: "" // Lấy giá trị sort, mặc định là ""

            ProductsHome(
                brandId = brand,
                sortOption = sort,
                navController = navHostController,
                onItemClick = { productId ->
                    navHostController.navigate("${DetailScreen.ProductDetailScreen.route}/$productId")
                }
            )
        }


        composable(
            route = "brands_home_screen/{categoryId}",
            arguments = listOf(navArgument("categoryId") {
                defaultValue = "all" // nếu không truyền thì mặc định là "all"
            })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "all"
            BrandsHome(category = category , navController = navController, onItemClick = { selectedCategory ->
                navController.navigate("products_home_screen/$selectedCategory")
            })
        }
        // su dung route de tuong ung voi san pham
        composable(
            route = "products_home_screen/{brandId}",
            arguments = listOf(navArgument("brandId") {
                defaultValue = "all" // nếu không truyền thì mặc định là "all"
            })

        ) { backStackEntry ->
            val brandId = backStackEntry.arguments?.getString("brandId") ?: "all"
            ProductsHome(
                brandId = brandId,
                navController = navHostController, // Thay thế navController = rememberNavController()
                onItemClick = { productId ->
                    navHostController.navigate("${DetailScreen.ProductDetailScreen.route}/$productId") // Sử dụng navHostController
                },
                sortOption = ""
            )
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

        composable(
            route = "profile_detail/{uid}",
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: ""
            EditProfileScreen(uid = uid)
        }

    }
}
