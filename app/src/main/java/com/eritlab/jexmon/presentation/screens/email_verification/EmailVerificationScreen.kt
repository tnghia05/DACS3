package com.eritlab.jexmon.presentation.screens.email_verification

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.eritlab.jexmon.presentation.common.CustomDefaultBtn
import com.eritlab.jexmon.presentation.graphs.auth_graph.AuthScreen
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import com.eritlab.jexmon.presentation.viewmodel.SignUpState
import com.eritlab.jexmon.presentation.viewmodel.SignUpViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun EmailVerificationScreen(navController: NavController) {
    val context = LocalContext.current
    val viewModel: SignUpViewModel = viewModel()
    val signUpState by viewModel.signUpState.collectAsState()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Xác Minh Email",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Chúng tôi đã gửi email xác minh đến:",
            color = MaterialTheme.colors.TextColor,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = userEmail,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "Vui lòng kiểm tra hộp thư của bạn và nhấp vào liên kết xác minh",
            color = MaterialTheme.colors.TextColor,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        CustomDefaultBtn(
            shapeSize = 50f,
            btnText = "Đã xác minh email"
        ) {
            viewModel.checkEmailVerification()
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        CustomDefaultBtn(
            shapeSize = 50f,
            btnText = "Gửi lại email xác minh"
        ) {
            currentUser?.sendEmailVerification()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(
                        context,
                        "Email xác minh đã được gửi lại",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Không thể gửi lại email xác minh. Vui lòng thử lại sau.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        LaunchedEffect(signUpState) {
            when (signUpState) {
                is SignUpState.Success -> {
                    Toast.makeText(
                        context,
                        "Xác minh email thành công!",
                        Toast.LENGTH_LONG
                    ).show()
                    navController.navigate(AuthScreen.SignInSuccess.route) {
                        popUpTo(AuthScreen.EmailVerificationScreen.route) { inclusive = true }
                    }
                }
                is SignUpState.Error -> {
                    Toast.makeText(
                        context,
                        (signUpState as SignUpState.Error).message,
                        Toast.LENGTH_LONG
                    ).show()
                }
                else -> {}
            }
        }
    }
} 