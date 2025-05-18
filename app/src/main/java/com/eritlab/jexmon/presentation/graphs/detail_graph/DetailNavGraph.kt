package com.eritlab.jexmon.presentation.graphs.detail_graph

import android.util.Log
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.eritlab.jexmon.common.Constrains
import com.eritlab.jexmon.domain.model.CartItem
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.screens.cart_screen.component.CartScreen
import com.eritlab.jexmon.presentation.screens.checkout_screen.CheckoutScreen
import com.eritlab.jexmon.presentation.screens.checkout_screen.CheckoutViewModel
import com.eritlab.jexmon.presentation.screens.checkout_screen.ShippingAddressScreen
import com.eritlab.jexmon.presentation.screens.checkout_screen.VoucherScreen
import com.eritlab.jexmon.presentation.screens.conversation_screen.component.ChatRepository
import com.eritlab.jexmon.presentation.screens.conversation_screen.component.ConversationScreen
import com.eritlab.jexmon.presentation.screens.notification_screen.component.NotificationScreen
import com.eritlab.jexmon.presentation.screens.product_detail_screen.ProductDetailViewModel
import com.eritlab.jexmon.presentation.screens.product_detail_screen.component.ProductDetailScreen
import com.eritlab.jexmon.presentation.screens.sign_success_screen.PaymentSuccess

fun NavGraphBuilder.detailNavGraph(navController: NavHostController) {
    Log.d("DetailNavGraph", "Setting up DetailNavGraph")
    navigation(
        route = Graph.DETAILS,
        startDestination = "${DetailScreen.ProductDetailScreen.route}/{${Constrains.PRODUCT_ID_PARAM}}"
    ) {
        composable(DetailScreen.CartScreen.route) {
            Log.d("Navigation", "Navigated to CartScreen")
            CartScreen(
                onBackClick = { navController.popBackStack() },
                onNavigateToCheckout = { cartItems -> 
                    navController.currentBackStackEntry?.savedStateHandle?.set("cartItems", cartItems)
                    navController.navigate(DetailScreen.CheckoutScreen.route)
                }
            )
        }

        composable(DetailScreen.CheckoutScreen.route) {
            Log.d("Navigation", "Navigated to CheckoutScreen")
            val cartItems = navController.previousBackStackEntry?.savedStateHandle?.get<List<CartItem>>("cartItems")
            Log.d("Navigation", "Retrieved cartItems from savedStateHandle: $cartItems")
            
            if (cartItems != null) {
                CheckoutScreen(
                    navController = navController,
                    cartItems = cartItems,
                    onBackClick = { navController.popBackStack() }
                )
            } else {
                Log.e("Navigation", "No cart items found in savedStateHandle")
                // Handle the error case, maybe show a message or navigate back
                navController.popBackStack()
            }
        }
        composable(DetailScreen.VoucherScreen.route) { backStackEntry ->
            VoucherScreen(
                navController = navController,
                viewModel = hiltViewModel<CheckoutViewModel>(),
                onBackClick = { navController.popBackStack() }
            )
        }


        composable(DetailScreen.NotificationScreen.route) {
            Log.d("Navigation", "Navigated to NotificationScreen, route: ${DetailScreen.NotificationScreen.route}")
            NotificationScreen()
        }
        composable(DetailScreen.PaymentSuccessScreen.route){
            Log.d("Navigation", "Navigated to PaymentSuccessScreen, route: ${DetailScreen.PaymentSuccessScreen.route}")
            // Handle payment success screen here
            PaymentSuccess(
                navController = navController
            )
        }

        composable(
            route = "${DetailScreen.ProductDetailScreen.route}/{${Constrains.PRODUCT_ID_PARAM}}",
            arguments = listOf(navArgument(Constrains.PRODUCT_ID_PARAM) { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString(Constrains.PRODUCT_ID_PARAM)
            Log.d("DetailNavGraph", "Attempting to show ProductDetailScreen with productId: $productId")
            
            if (productId != null) {
                Log.d("DetailNavGraph", "ProductId is valid, showing ProductDetailScreen")
                ProductDetailScreen(
                    productId = productId,
                    viewModel = hiltViewModel<ProductDetailViewModel>(),
                    popBack = { 
                        Log.d("DetailNavGraph", "Popping back from ProductDetailScreen")
                        navController.previousBackStackEntry?.savedStateHandle?.get<String>("active_chat_id")?.let { chatId ->
                            Log.d("DetailNavGraph", "Restoring chat ID: $chatId")
                            navController.currentBackStackEntry?.savedStateHandle?.set("active_chat_id", chatId)
                        }
                        navController.popBackStack() 
                    },
                    onNavigateToCart = { 
                        Log.d("DetailNavGraph", "Navigating to CartScreen")
                        navController.navigate(DetailScreen.CartScreen.route) 
                    },
                    onNavigateToProduct = { newProductId -> 
                        Log.d("DetailNavGraph", "Navigating to new product: $newProductId")
                        val newRoute = "${DetailScreen.ProductDetailScreen.route}/$newProductId"
                        navController.navigate(newRoute) {
                            popUpTo("${DetailScreen.ProductDetailScreen.route}/$productId") {
                                inclusive = true
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToCheckout = { cartItems ->
                        Log.d("DetailNavGraph", "Navigating to CheckoutScreen")
                        navController.currentBackStackEntry?.savedStateHandle?.set("cartItems", cartItems)
                        navController.navigate(DetailScreen.CheckoutScreen.route)
                    }
                )
            } else {
                Log.e("DetailNavGraph", "ProductId is null, cannot show ProductDetailScreen")
                navController.popBackStack()
            }
        }   
        composable(DetailScreen.ShippingAddressScreen.route) {
            ShippingAddressScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(DetailScreen.NewChatScreen.route) {
            val context = LocalContext.current
            ConversationScreen(
                navController = navController,
                idChat = "", // Chat mới
                chatRepository = ChatRepository(context),
                onNavigateToProduct = { productId ->
                    try {
                        Log.d("DetailNavGraph", "Received navigation request for product: $productId")
                        // Save current chat ID before navigating
                        navController.currentBackStackEntry?.savedStateHandle?.get<String>("active_chat_id")?.let { chatId ->
                            Log.d("DetailNavGraph", "Saving chat ID before navigation: $chatId")
                            navController.getBackStackEntry(DetailScreen.NewChatScreen.route)
                                .savedStateHandle.set("active_chat_id", chatId)
                        }
                        
                        val route = "${DetailScreen.ProductDetailScreen.route}/$productId"
                        Log.d("DetailNavGraph", "Navigating to route: $route")
                        
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    } catch (e: Exception) {
                        Log.e("DetailNavGraph", "Navigation error: ${e.message}", e)
                    }
                }
            )
        }

        composable(
            route = DetailScreen.ExistingChatScreen.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val context = LocalContext.current
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ConversationScreen(
                navController = navController,
                idChat = chatId,
                chatRepository = ChatRepository(context),
                onNavigateToProduct = { productId ->
                    try {
                        Log.d("DetailNavGraph", "Received navigation request for product: $productId")
                        // Save current chat ID before navigating
                        navController.currentBackStackEntry?.savedStateHandle?.get<String>("active_chat_id")?.let { activeChatId ->
                            Log.d("DetailNavGraph", "Saving chat ID before navigation: $activeChatId")
                            navController.getBackStackEntry(DetailScreen.ExistingChatScreen.route)
                                .savedStateHandle.set("active_chat_id", activeChatId)
                        }
                        
                        val route = "${DetailScreen.ProductDetailScreen.route}/$productId"
                        Log.d("DetailNavGraph", "Navigating to route: $route")
                        
                        navController.navigate(route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    } catch (e: Exception) {
                        Log.e("DetailNavGraph", "Navigation error: ${e.message}", e)
                    }
                }
            )
        }
    }
}
