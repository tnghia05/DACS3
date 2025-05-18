package com.eritlab.jexmon.presentation.graphs.detail_graph

import com.eritlab.jexmon.common.Constrains

sealed class DetailScreen(val route: String) {

    object VoucherScreen : DetailScreen("voucher_screen")
    object ShippingAddressScreen : DetailScreen("shipping_address_screen")
    object CartScreen : DetailScreen("cart_screen")
    object NotificationScreen : DetailScreen("notification_screen")
    object CheckoutScreen : DetailScreen("checkout_screen")
    object PaymentSuccessScreen : DetailScreen("payment_success_screen")

    object ProductDetailScreen : DetailScreen("product_detail_screen/{${Constrains.PRODUCT_ID_PARAM}}") {
        fun createRoute(productId: String) = "product_detail_screen/$productId"
    }

    object NewChatScreen : DetailScreen("new_chat_screen")

    object ExistingChatScreen : DetailScreen("chat_screen/{chatId}") {
        fun createRoute(chatId: String) = "chat_screen/$chatId"
    }
}
