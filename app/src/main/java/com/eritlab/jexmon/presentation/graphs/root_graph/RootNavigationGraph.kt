package com.eritlab.jexmon.presentation.graphs.root_graph

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.graphs.admin_graph.adminNavGraph
import com.eritlab.jexmon.presentation.graphs.authNavGraph
import com.eritlab.jexmon.presentation.graphs.detail_graph.DetailScreen
import com.eritlab.jexmon.presentation.graphs.detail_graph.detailNavGraph
import com.eritlab.jexmon.presentation.screens.admin.AdminDashboard
import com.eritlab.jexmon.presentation.screens.checkout_screen.CheckoutViewModel
import com.eritlab.jexmon.presentation.screens.home_screen.component.HomeScreen
import com.eritlab.jexmon.presentation.screens.product_detail_screen.ProductDetailViewModel
import com.eritlab.jexmon.presentation.screens.product_detail_screen.component.ProductDetailScreen

@Composable
fun RootNavigationGraph(navHostController: NavHostController, context: Context,
                        checkoutViewModel: CheckoutViewModel
) {
    Log.d("Navigation", "Setting up RootNavigationGraph")

    NavHost(
        navController = navHostController,
        route = Graph.ROOT,
        startDestination = Graph.HOME
    ) {
        authNavGraph(navHostController, context)

        // ✅ Đăng ký adminNavGraph đúng cách
        adminNavGraph(navHostController)
        detailNavGraph(navHostController) // ✅ Thêm dòng này nè !!!

        composable(route = Graph.HOME) {
            Log.d("Navigation", "Loading HomeScreen")
            HomeScreen()
        }

        composable(route = Graph.ADMIN) {
            Log.d("Navigation", "Loading AdminDashboard")
            AdminDashboard(navHostController)
        }
        composable(
            route = "product_detail_screen/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                productId = productId,
                viewModel = hiltViewModel<ProductDetailViewModel>(),


                popBack = { navHostController.popBackStack() },
                onNavigateToCart = { navHostController.navigate(DetailScreen.CartScreen.route) }

            )
        }

    }


    // ✅ In ra toàn bộ route để kiểm tra
    // ✅ In ra toàn bộ route (hỗ trợ duyệt qua tất cả các màn hình con)
    LaunchedEffect(Unit) {
        Log.d("Navigation", "Alsls destinations:")
        navHostController.graph.forEach { node ->
            Log.d("Navigation", "Destination: ${node.route}")
        }
    }


}
