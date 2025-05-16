package com.eritlab.jexmon.di

import android.app.Application
import android.content.pm.ApplicationInfo
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.pow

@HiltAndroidApp
class JexmonApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tokenRetryCount = 0
    private val maxRetries = 5
    private var tokenRefreshJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        initializeAppCheck()
    }

    private fun isDebugBuild(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun initializeAppCheck() {
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        
        // Tắt auto refresh để kiểm soát việc làm mới token
        firebaseAppCheck.setTokenAutoRefreshEnabled(false)
        
        try {
            if (isDebugBuild()) {
                // Chỉ sử dụng Debug Provider trong môi trường debug
                val debugProviderFactory = DebugAppCheckProviderFactory.getInstance()
                
                // Đặt debug token trước khi cài đặt provider
                debugProviderFactory.apply {
                    // Sử dụng debug token từ log
                    val debugToken = "caabfa32-57f4-46c9-80a7-bec6db63095a"
                    Log.d(TAG, "Debug token being used: $debugToken")
                }
                
                firebaseAppCheck.installAppCheckProviderFactory(debugProviderFactory)
                Log.d(TAG, "✅ Đã cài đặt Debug Provider thành công")
                
                // Đợi lâu hơn trong môi trường debug để đảm bảo token được xử lý
                applicationScope.launch {
                    delay(2000) // Đợi 2 giây
                    getAppCheckToken(true)
                }
            } else {
                // Sử dụng Play Integrity trong môi trường production
                val playIntegrityFactory = PlayIntegrityAppCheckProviderFactory.getInstance()
                firebaseAppCheck.installAppCheckProviderFactory(playIntegrityFactory)
                Log.d(TAG, "✅ Đã cài đặt Play Integrity Provider thành công")
                
                // Trong production có thể lấy token ngay lập tức
                applicationScope.launch {
                    delay(500) // Đợi 0.5 giây để đảm bảo
                    getAppCheckToken(true)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi khởi tạo App Check: ${e.message}")
            appCheckReady = false
        }
    }

    private fun getAppCheckToken(forceRefresh: Boolean = false) {
        if (tokenRetryCount >= maxRetries) {
            Log.e(TAG, "❌ Đã vượt quá số lần thử lại cho phép")
            applicationScope.launch {
                delay(30000) // Đợi 30 giây trước khi reset số lần thử
                tokenRetryCount = 0
                getAppCheckToken(true)
            }
            return
        }

        try {
            FirebaseAppCheck.getInstance().getAppCheckToken(forceRefresh)
                .addOnSuccessListener { token ->
                    tokenRetryCount = 0
                    appCheckReady = true
                    Log.d(TAG, "✅ Đã nhận được token App Check mới")
                    scheduleTokenRefresh(token.token)
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "❌ Lỗi khi lấy token App Check: ${exception.message}")
                    
                    when {
                        exception.message?.contains("Too many attempts") == true -> {
                            // Đợi lâu hơn khi gặp lỗi này
                            tokenRetryCount++
                            val delayTime = (30000L * tokenRetryCount) // Tăng thời gian đợi theo số lần thử
                            applicationScope.launch {
                                delay(delayTime)
                                getAppCheckToken(true)
                            }
                        }
                        exception.message?.contains("attestation failed") == true -> {
                            // Thử lại với provider khác
                            try {
                                val provider = if (isDebugBuild()) {
                                    PlayIntegrityAppCheckProviderFactory.getInstance()
                                } else {
                                    DebugAppCheckProviderFactory.getInstance()
                                }
                                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(provider)
                                applicationScope.launch {
                                    delay(2000) // Đợi provider mới được cài đặt
                                    getAppCheckToken(true)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Lỗi khi thay đổi provider: ${e.message}")
                                appCheckReady = false
                            }
                        }
                        else -> {
                            // Thử lại với exponential backoff
                            tokenRetryCount++
                            val baseDelay = 1000L * 2.0.pow(tokenRetryCount.toDouble())
                            val jitter = (Math.random() * 1000).toLong()
                            val delayMillis = baseDelay.toLong() + jitter
                            
                            applicationScope.launch {
                                delay(delayMillis)
                                getAppCheckToken(true)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Lỗi không mong đợi khi lấy token: ${e.message}")
            appCheckReady = false
        }
    }

    private fun scheduleTokenRefresh(currentToken: String) {
        tokenRefreshJob?.cancel()
        tokenRefreshJob = applicationScope.launch {
            // Làm mới token 5 phút trước khi hết hạn
            delay(55 * 60 * 1000L)
            
            try {
                val latestToken = FirebaseAppCheck.getInstance().getAppCheckToken(false).await().token
                if (latestToken == currentToken) {
                    getAppCheckToken(true)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Lỗi khi làm mới token: ${e.message}")
                getAppCheckToken(true)
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        tokenRefreshJob?.cancel()
        applicationScope.cancel()
    }

    companion object {
        private const val TAG = "AppCheck"
        
        @Volatile
        private var appCheckReady = false
        
        fun isAppCheckInitialized(): Boolean = appCheckReady
        
        fun resetAppCheckState() {
            appCheckReady = false
        }
    }
}