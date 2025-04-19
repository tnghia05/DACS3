package com.eritlab.jexmon.presentation.screens.verification_screen

import android.os.CountDownTimer
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.eritlab.jexmon.presentation.graphs.auth_graph.AuthScreen

@Composable
fun EmailVerificationScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    var isVerified by remember { mutableStateOf(auth.currentUser?.isEmailVerified ?: false) }
    var timeLeft by remember { mutableStateOf(60) }

    LaunchedEffect(Unit) {
        object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeft = (millisUntilFinished / 1000).toInt()
            }
            override fun onFinish() {
                timeLeft = 0
            }
        }.start()
    }

    LaunchedEffect(isVerified) {
        val user = auth.currentUser
        user?.reload()?.addOnCompleteListener {
            isVerified = user.isEmailVerified
            if (isVerified) {
                navController.navigate(AuthScreen.SignInScreen.route)
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Vui lòng xác nhận email của bạn", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Chúng tôi đã gửi một email xác thực đến địa chỉ của bạn.")
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "Thời gian chờ: $timeLeft giây")
        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            auth.currentUser?.reload()?.addOnCompleteListener {
                isVerified = auth.currentUser?.isEmailVerified ?: false
                if (isVerified) {
                    navController.navigate(AuthScreen.SignInScreen.route)
                }
            }
        }) {
            Text("Tôi đã xác thựdc, tiếp tục")
        }
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            auth.currentUser?.sendEmailVerification()
        }) {
            Text("Gửi lại email xác thực")
        }
    }
}
