package com.eritlab.jexmon.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.eritlab.jexmon.presentation.graphs.root_graph.RootNavigationGraph
import com.eritlab.jexmon.presentation.screens.checkout_screen.CheckoutViewModel
import com.eritlab.jexmon.presentation.ui.theme.JexmonTheme
import dagger.hilt.android.AndroidEntryPoint
import vn.zalopay.sdk.Environment
import vn.zalopay.sdk.ZaloPaySDK


@AndroidEntryPoint // Giữ annotation này nếu bạn dùng Hilt
class MainActivity : ComponentActivity() { // Hoặc AppCompatActivity()

    // Lấy instance của CheckoutViewModel. ViewModel này sẽ được giữ bởi Activity
    // và chia sẻ giữa Activity và Composable (qua hiltViewModel()).
    // annotation @AndroidEntryPoint cho phép dùng viewModels() delegation với Hilt
    private val checkoutViewModel: CheckoutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // *** Khởi tạo ZaloPay SDK ***
        // Nên đặt trong Application class nếu có (khuyến nghị), nhưng có thể đặt ở đây
        // nếu MainActivity là điểm vào chính và tồn tại suốt quá trình thanh toán.
        // Thay thế <appID> bằng ZaloPay App ID của bạn (ví dụ: 2553)
        // Thay thế Environment.SANDBOX bằng Environment.RELEASE khi deploy thật
        ZaloPaySDK.init(2553, Environment.SANDBOX) // <-- THÊM DÒNG NÀY (có thể báo lỗi Unresolved)

        setContent {
            JexmonTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    // Truyền viewModel xuống navigation graph và màn hình Checkout
                    ShowScreen(
                        context = LocalContext.current,
                        checkoutViewModel = checkoutViewModel // <-- TRUYỀN ViewModel
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("MainActivityLifecycle", "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivityLifecycle", "onStop called")
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivityLifecycle", "onStart called")
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivityLifecycle", "onResume called")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivityLifecycle", "onDestroy called")
    }

    // *** Xử lý kết quả trả về từ ZaloPay (qua Deeplink) ***
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivityLifecycle", "onNewIntent called with intent: $intent")
        // Quan trọng: Chuyển Intent cho ZaloPay SDK xử lý
        if (intent != null) {
            // Gọi hàm onResult của ZaloPay SDK để xử lý kết quả trả về
            Log.d("MainActivityLifecycle", "ZaloPay SDK onResult called")
            ZaloPaySDK.getInstance().onResult(intent) // <-- THÊM DÒNG NÀY (có thể báo lỗi Unresolved)
            // ZaloPay SDK sẽ gọi lại PayOrderListener mà bạn đã định nghĩa trong Composable
            // Listener đó sẽ gọi hàm handleZaloPayResult của ViewModel.
            // Không cần gọi handleZaloPayResult trực tiếp ở đây nếu Listener trong Composable gọi nó.
        }
    }

    // ... Các phương thức khác của Activity ...
}

@Composable
private fun ShowScreen(context: Context, checkoutViewModel: CheckoutViewModel) { // <-- NHẬN ViewModel
    val navHostController = rememberNavController()

    // Truyền ViewModel xuống RootNavigationGraph để các màn hình bên trong (như CheckoutScreen) có thể sử dụng
    RootNavigationGraph(
        navHostController = navHostController,
        context = context,
        checkoutViewModel = checkoutViewModel // <-- TRUYỀN ViewModel vào Navigation Graph
    )
}

// TODO: Sửa RootNavigationGraph để nhận và truyền ViewModel xuống các màn hình cần nó (CheckoutScreen)
/*
@Composable
fun RootNavigationGraph(
    navHostController: NavHostController,
    context: Context,
    checkoutViewModel: CheckoutViewModel // <-- NHẬN ViewModel
) {
    NavHost(...) {
        composable("checkout_route") {
            CheckoutScreen(
                navController = navHostController,
                onBackClick = { ... },
                viewModel = checkoutViewModel // <-- TRUYỀN ViewModel xuống CheckoutScreen
            )
        }
        // ... các màn hình khác ...
    }
}
*/