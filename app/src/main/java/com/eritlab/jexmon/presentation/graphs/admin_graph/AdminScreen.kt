package com.eritlab.jexmon.presentation.graphs.admin_graph

sealed class AdminScreen(val route: String) {
    object Dashboard : AdminScreen("admin_dashboard")
    object BrandScreen : AdminScreen("admin_brand_screen")
    object AddBrandScreen : AdminScreen("admin_add_brand_screen")
    object EditBrandScreen : AdminScreen("admin_edit_brand_screen")
    object CategoryScreen : AdminScreen("admin_category_screen")
    object AddCategoryScreen : AdminScreen("admin_add_category_screen")
    object EditCategoryScreen : AdminScreen("admin_edit_category_screen/{categoryId}") {
        fun passCategoryId(categoryId: String) = "admin_edit_category_screen/$categoryId"
    }
    object OrderManagementScreen : AdminScreen("admin_order_management_screen")
    object OrderDetailScreen : AdminScreen("admin_order_detail_screen/{orderId}") {
        fun passOrderId(orderId: String) = "admin_order_detail_screen/$orderId"
    }
    object UserManagementScreen : AdminScreen("admin_user_management_screen")
    object EditUserScreen : AdminScreen("admin_edit_user_screen/{userId}") {
        fun passUserId(userId: String) = "admin_edit_user_screen/$userId"
    }
}
