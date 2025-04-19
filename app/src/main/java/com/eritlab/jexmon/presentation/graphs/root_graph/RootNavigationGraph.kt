package com.eritlab.jexmon.presentation.graphs.root_graph

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.eritlab.jexmon.presentation.graphs.Graph
import com.eritlab.jexmon.presentation.graphs.admin_graph.adminNavGraph
import com.eritlab.jexmon.presentation.graphs.authNavGraph
import com.eritlab.jexmon.presentation.screens.admin.AdminDashboard
import com.eritlab.jexmon.presentation.screens.home_screen.component.HomeScreen

@Composable
fun RootNavigationGraph(navHostController: NavHostController, context: Context) {
    Log.d("Navigation", "Setting up RootNavigationGraph")

    NavHost(
        navController = navHostController,
        route = Graph.ROOT,
        startDestination = Graph.HOME
    ) {
        authNavGraph(navHostController, context)

        // ✅ Đăng ký adminNavGraph đúng cách
        adminNavGraph(navHostController)

        composable(route = Graph.HOME) {
            Log.d("Navigation", "Loading HomeScreen")
            HomeScreen()
        }

        composable(route = Graph.ADMIN) {
            Log.d("Navigation", "Loading AdminDashboard")
            AdminDashboard(navHostController)
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
