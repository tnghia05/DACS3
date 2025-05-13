package com.eritlab.jexmon.presentation.graphs.detail_graph

import android.util.Log
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
import com.eritlab.jexmon.presentation.screens.notification_screen.component.NotificationScreen
import com.eritlab.jexmon.presentation.screens.product_detail_screen.ProductDetailViewModel
import com.eritlab.jexmon.presentation.screens.product_detail_screen.component.ProductDetailScreen
import com.eritlab.jexmon.presentation.screens.sign_success_screen.PaymentSuccess


fun NavGraphBuilder.detailNavGraph(navController: NavHostController) {
    navigation(
        route = Graph.DETAILS,
        startDestination = DetailScreen.ProductDetailScreen.route + "/{${Constrains.PRODUCT_ID_PARAM}}"
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
            val cartItems = navController.previousBackStackEntry?.savedStateHandle?.get<List<CartItem>>("cartItems") ?: emptyList()
            Log.d("Navigation", "Navigated to CheckoutScreen with cartItems: $cartItems")
            CheckoutScreen(
                navController = navController,
                cartItems = cartItems,
                onBackClick = { navController.popBackStack() },

                )
        }
        composable("voucher_screen") { backStackEntry ->
            // Lấy voucherCode từ argument
            val voucherCode = backStackEntry.arguments?.getString("voucherCode")

            // Truyền voucherCode vào VoucherScreen
            VoucherScreen(navController = navController,
                viewModel = hiltViewModel<CheckoutViewModel>()
                , onBackClick = { navController.popBackStack() }
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
            val productId = backStackEntry.arguments?.getString(Constrains.PRODUCT_ID_PARAM) ?: ""
            Log.d("Navigation", "Navigating to ProductDetailScreen with productId: $productId")
            ProductDetailScreen(
                productId = productId,
                viewModel = hiltViewModel<ProductDetailViewModel>(),
                popBack = { navController.popBackStack() },
                onNavigateToCart = { navController.navigate(DetailScreen.CartScreen.route) },
                onNavigateToProduct = { newProductId -> 
                    navController.navigate("${DetailScreen.ProductDetailScreen.route}/$newProductId")
                }
            )
        }   
        composable(DetailScreen.ShippingAddressScreen.route) {
            ShippingAddressScreen(
                navController = navController,
                onBackClick = { navController.popBackStack() }
            )
        }





    }
}
