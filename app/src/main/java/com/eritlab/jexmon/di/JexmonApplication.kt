package com.eritlab.jexmon.di

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JexmonApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)
        
        // Khởi tạo App Check với cấu hình debug
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.setTokenAutoRefreshEnabled(true)  // Thêm dòng này
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )

        // Đợi và kiểm tra token
        firebaseAppCheck.getAppCheckToken(true)
            .addOnSuccessListener { token ->
                isAppCheckReady = true
                Log.d("AppCheck", "✅ App Check token: ${token.token}")
            }
            .addOnFailureListener { exception ->
                Log.e("AppCheck", "❌ Lỗi App Check: ${exception.message}")
                // Thử lại sau 2 giây nếu thất bại
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    retryAppCheckInitialization()
                }, 2000)
            }
    }

    private fun retryAppCheckInitialization() {
        FirebaseAppCheck.getInstance().getAppCheckToken(true)
            .addOnSuccessListener { token ->
                isAppCheckReady = true
                Log.d("AppCheck", "✅ App Check token (retry): ${token.token}")
            }
            .addOnFailureListener { exception ->
                Log.e("AppCheck", "❌ Lỗi App Check (retry): ${exception.message}")
            }
    }

    companion object {
        @Volatile
        private var isAppCheckReady = false
        
        fun isAppCheckInitialized() = isAppCheckReady
    }
}