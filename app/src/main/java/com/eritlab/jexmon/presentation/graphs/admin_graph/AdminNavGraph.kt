package com.eritlab.jexmon.presentation.graphs.admin_graph

import android.util.Log
import androidx.collection.forEach
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.screens.admin.AddBrandScreen
import com.eritlab.jexmon.presentation.screens.admin.AdminDashboard
import com.eritlab.jexmon.presentation.screens.admin.Brand.EditBrandScreen
import com.eritlab.jexmon.presentation.screens.admin.BrandScreen
import com.eritlab.jexmon.presentation.screens.admin.category.AddCategoryScreen
import com.eritlab.jexmon.presentation.screens.admin.category.CategoryScreen
import com.eritlab.jexmon.presentation.screens.admin.category.EditCategoryScreen

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

            // Check if brandId is null and handle accordingly
            brandId?.let {
                EditBrandScreen(navHostController, it)
            } ?: run {
                Log.e("Navigation", "Brand ID is missing!")
                // You can add fallback behavior, like navigating back or showing an error
            }
        }
        composable(AdminScreen.CategoryScreen.route) {
            Log.d("Navigation", "CategoryScreen composable created")
            CategoryScreen(navHostController)
        }
        composable(AdminScreen.AddCategoryScreen.route) {
            Log.d("Navigation", "AddCategoryScreen composable created")

            AddCategoryScreen(navHostController)

            }
        composable("admin_edit_category_screen/{brandId}") { backStackEntry ->
            Log.d("Navigation", "EditBrandScreen composable created")

            // Extract the brandId from the backStackEntry

            val categoryId = backStackEntry.arguments?.getString("categoryId")
            // Check if brandId is null and handle accordingly
            categoryId?.let {
                EditCategoryScreen(navHostController, it) // Pass the brandId to EditBrandScreen
            }
                ?: run {
                    Log.e("Navigation", "Brand ID is missing!")
                    // You can add fallback behavior, like navigating back or showing an error
                }
            }







    }
}
