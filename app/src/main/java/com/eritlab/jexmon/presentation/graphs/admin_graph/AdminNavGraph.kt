package com.eritlab.jexmon.presentation.graphs.admin_graph

import android.util.Log
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.screens.admin.AddBrandScreen
import com.eritlab.jexmon.presentation.screens.admin.AdminDashboard
import com.eritlab.jexmon.presentation.screens.admin.Brand.EditBrandScreen
import com.eritlab.jexmon.presentation.screens.admin.BrandScreen
import com.eritlab.jexmon.presentation.screens.admin.category.AddProductScreen
import com.eritlab.jexmon.presentation.screens.admin.category.CategoryScreen
import com.eritlab.jexmon.presentation.screens.admin.category.EditCategoryScreen
import com.eritlab.jexmon.presentation.screens.admin.order.OrderDetailScreen
import com.eritlab.jexmon.presentation.screens.admin.order.OrderManagementScreen

fun NavGraphBuilder.adminNavGraph(navHostController: NavHostController) {
    Log.d("Navigation", "AdminNavGraph is being set up")
    navigation(
        route = Graph.ADMIN,  // ✅ Chắc chắn route đúng
        startDestination = AdminScreen.Dashboard.route
    ) {
        composable(AdminScreen.Dashboard.route) {
            Log.d("Navigation", "AdminDashboard composable created")
            AdminDashboard(navHostController)
        }
        composable(AdminScreen.BrandScreen.route) {
            Log.d("Navigation", "BrandScreen composable created")
            BrandScreen(navHostController)
        }
        composable(AdminScreen.AddBrandScreen.route) {
            Log.d("Navigation", "AddBrandScreen composable created")
            // AddBrandScreen(navHostController)
            AddBrandScreen(navHostController)
        }
        composable("admin_edit_brand_screen/{brandId}") { backStackEntry ->
            Log.d("Navigation", "EditBrandScreen composable created")

            // Extract the brandId from the backStackEntry
            val brandId = backStackEntry.arguments?.getString("brandId")
            EditBrandScreen(navHostController, brandId ?: "")
        }
        composable(AdminScreen.CategoryScreen.route) {
            Log.d("Navigation", "CategoryScreen composable created")
            CategoryScreen(navHostController)
        }
        composable(AdminScreen.AddCategoryScreen.route) {
            Log.d("Navigation", "AddCategoryScreen composable created")
            AddProductScreen(navHostController)
        }
        composable(AdminScreen.EditCategoryScreen.route) { backStackEntry ->
            Log.d("Navigation", "EditCategoryScreen composable created")
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            categoryId?.let {
                EditCategoryScreen( navHostController, categoryId = it)
            }
        }
        composable(AdminScreen.OrderManagementScreen.route) {
            Log.d("Navigation", "OrderManagementScreen composable created")
            OrderManagementScreen(
                navController = navHostController,
                onBackBtnClick = { navHostController.popBackStack() }
            )
        }
        composable(AdminScreen.OrderDetailScreen.route) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId")
            orderId?.let {
                OrderDetailScreen(
                    navController = navHostController,
                    orderId = it,
                    onBackClick = { navHostController.navigateUp() }
                )
            }
        }
    }
}
