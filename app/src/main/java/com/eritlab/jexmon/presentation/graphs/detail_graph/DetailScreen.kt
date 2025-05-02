package com.eritlab.jexmon.presentation.graphs.detail_graph

import com.eritlab.jexmon.common.Constrains

sealed class DetailScreen(val route: String) {

    object VoucherScreen : DetailScreen("voucher_screen")
    object CartScreen : DetailScreen("cart_screen")
    object NotificationScreen : DetailScreen("notification_screen")
    object CheckoutScreen : DetailScreen("checkout_screen")
    object PaymentSuccessScreen : DetailScreen("payment_success_screen")

    // Sử dụng Constrains.PRODUCT_ID_PARAM từ common
    object ProductDetailScreen : DetailScreen("product_detail_screen/{${Constrains.PRODUCT_ID_PARAM}}")
}
