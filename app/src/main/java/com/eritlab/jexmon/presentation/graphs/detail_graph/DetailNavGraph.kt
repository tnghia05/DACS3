package com.eritlab.jexmon.presentation.graphs.detail_graph

import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import com.eritlab.jexmon.common.Constrains
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.screens.cart_screen.component.CartScreen
import com.eritlab.jexmon.presentation.screens.notification_screen.component.NotificationScreen
import com.eritlab.jexmon.presentation.screens.product_detail_screen.component.ProductDetailScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.eritlab.jexmon.presentation.screens.product_detail_screen.ProductDetailViewModel


fun NavGraphBuilder.detailNavGraph(navController: NavHostController) {
    navigation(
        route = Graph.DETAILS,
        startDestination = DetailScreen.ProductDetailScreen.route + "/{${Constrains.PRODUCT_ID_PARAM}}"
    ) {
        composable(DetailScreen.CartScreen.route) {
            CartScreen()
        }
        composable(DetailScreen.NotificationScreen.route) {
            Log.d("DetailNavGraph", "NotificationScreen: Navigated to NotificationScreen, route: ${DetailScreen.NotificationScreen.route}")
            NotificationScreen()
        }
        composable(
            route = "${DetailScreen.ProductDetailScreen.route}/{${Constrains.PRODUCT_ID_PARAM}}",
            arguments = listOf(navArgument(Constrains.PRODUCT_ID_PARAM) { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString(Constrains.PRODUCT_ID_PARAM) ?: ""

            ProductDetailScreen(
                productId = productId,
                viewModel = hiltViewModel<ProductDetailViewModel>(),
                popBack = { navController.popBackStack() }
            )
        }



    }
}
