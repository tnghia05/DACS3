package com.eritlab.jexmon.presentation.screens.sign_in_screen.component

import android.content.Context
import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.eritlab.jexmon.R
import com.eritlab.jexmon.presentation.common.CustomDefaultBtn
import com.eritlab.jexmon.presentation.common.CustomTextField
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.common.component.ErrorSuggestion
import com.eritlab.jexmon.presentation.graphs.auth_graph.AuthScreen
import com.eritlab.jexmon.presentation.ui.theme.PrimaryColor
import com.eritlab.jexmon.presentation.ui.theme.PrimaryLightColor
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val sharedPreferences = context.getSharedPreferences("login_pref", Context.MODE_PRIVATE)
    
    // Load saved data
    val savedEmail = sharedPreferences.getString("saved_email", "") ?: ""
    val savedPassword = sharedPreferences.getString("saved_password", "") ?: ""
    val savedRememberMe = sharedPreferences.getBoolean("remember_login", false)
    
    var email by remember { mutableStateOf(TextFieldValue(if (savedRememberMe) savedEmail else "")) }
    var password by remember { mutableStateOf(TextFieldValue(if (savedRememberMe) savedPassword else "")) }
    var checkBox by remember { mutableStateOf(savedRememberMe) }
    
    val emailErrorState = remember { mutableStateOf(false) }
    val passwordErrorState = remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Effect to update text fields when component is created
    LaunchedEffect(Unit) {
        if (savedRememberMe) {
            email = TextFieldValue(savedEmail)
            password = TextFieldValue(savedPassword)
        } else {
            email = TextFieldValue("")
            password = TextFieldValue("")

        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        )
        {
            Box(modifier = Modifier.weight(0.7f)) {
                DefaultBackArrow {
                    navController.popBackStack()
                }
            }
            Box(modifier = Modifier.weight(1.0f)) {
                Text(text = "Đăng nhập", color = MaterialTheme.colors.TextColor, fontSize = 18.sp)
            }
        }
        Spacer(modifier = Modifier.height(50.dp))
        Text(text = "Chào mừng trở lại", fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text(
            text = "Đăng nhập bằng email và mật khẩu\nhoặc tiếp tục với mạng xã hội.",
            color = MaterialTheme.colors.TextColor,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(50.dp))
        CustomTextField(
            placeholder = "example@email.com",
            trailingIcon = R.drawable.mail,
            label = "Email",
            defaultValue = if (savedRememberMe) savedEmail else "",
            errorState = emailErrorState,
            keyboardType = KeyboardType.Email,
            visualTransformation = VisualTransformation.None,
            onChanged = { newEmail ->
                email = newEmail
            }
        )
        Spacer(modifier = Modifier.height(20.dp))
        CustomTextField(
            placeholder = "********",
            trailingIcon = R.drawable.lock,
            label = "Mật khẩu",
            defaultValue = if (savedRememberMe) savedPassword else "",
            errorState = passwordErrorState,
            keyboardType = KeyboardType.Password,
            visualTransformation = PasswordVisualTransformation(),
            onChanged = { newPass ->
                password = newPass
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        if (emailErrorState.value) {
            ErrorSuggestion("Vui lòng nhập email hợp lệ")
        }
        if (passwordErrorState.value) {
            Row() {
                ErrorSuggestion("Vui lòng nhập mật khẩu hợp lệ")
            }
        }
        if (errorMessage.isNotEmpty()) {
            ErrorSuggestion(errorMessage)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = checkBox, onCheckedChange = {
                        checkBox = it
                    },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colors.PrimaryColor)
                )
                Text(text = "Ghi nhớ đăng nhập", color = MaterialTheme.colors.TextColor, fontSize = 14.sp)
            }
            Text(
                text = "Quên mật khẩu",
                color = MaterialTheme.colors.TextColor,
                style = TextStyle(textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable {
                    navController.navigate(AuthScreen.ForgetPasswordScreen.route)
                }
            )
        }
        CustomDefaultBtn(shapeSize = 50f, btnText = "Tiếp tục") {
            val pattern = Patterns.EMAIL_ADDRESS
            val isEmailValid = pattern.matcher(email.text).matches()
            val isPassValid = password.text.length >= 8
            emailErrorState.value = !isEmailValid
            passwordErrorState.value = !isPassValid
            if (isEmailValid && isPassValid) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email.text, password.text)
                    .addOnSuccessListener { authResult ->
                        val editor = sharedPreferences.edit()
                        if (checkBox) {
                            editor.putString("saved_email", email.text)
                            editor.putString("saved_password", password.text)
                            editor.putBoolean("remember_login", true)
                            val success = editor.commit()
                            Log.d("LoginScreen", "Saved credentials - Email: ${email.text}, Password: ${password.text}, Success: $success")
                        } else {
                            editor.clear()
                            editor.commit()
                            Log.d("LoginScreen", "Cleared saved credentials")
                        }

                        val user = authResult.user
                        val uid = user?.uid
                        user?.getIdToken(true)?.addOnSuccessListener { result ->
                            val isAdmin = result.claims["admin"] as? Boolean ?: false
                            if (isAdmin) {
                                println("✅ Người dùng là admin!")
                                navController.navigate(AuthScreen.SignInSuccess.route)
                            } else {
                                println("👤 Người dùng thường")
                                navController.navigate(AuthScreen.SignInSuccess.route)
                            }
                        }
                    }
                    .addOnFailureListener { exception ->
                        errorMessage = when {
                            exception.message?.contains("no user record") == true -> 
                                "Tài khoản không tồn tại"
                            exception.message?.contains("password is invalid") == true ->
                                "Mật khẩu không đúng"
                            exception.message?.contains("network error") == true ->
                                "Lỗi kết nối mạng. Vui lòng thử lại"
                            exception.message?.contains("incorrect, malformed") == true ->
                                "Email hoặc mật khẩu không chính xác"
                            else -> "Đăng nhập thất bại: ${exception.message}"
                        }
                    }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 50.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 10.dp,
                    alignment = Alignment.CenterHorizontally
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.google_icon),
                        contentDescription = "Google Login Icon"
                    )
                }
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = CircleShape
                        )
                        .clickable {

                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.twitter),
                        contentDescription = "Twitter Login Icon"
                    )
                }
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            MaterialTheme.colors.PrimaryLightColor,
                            shape = CircleShape
                        )
                        .clickable {

                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.facebook_2),
                        contentDescription = "Facebook Login Icon"
                    )
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Chưa có tài khoản? ", color = MaterialTheme.colors.TextColor)
                Text(
                    text = "Đăng ký",
                    color = MaterialTheme.colors.PrimaryColor,
                    modifier = Modifier.clickable {
                        navController.navigate(AuthScreen.SignUpScreen.route)
                    })
            }
        }
    }
}

fun sendUidToServer(uid: String) {
    val client = OkHttpClient()
    val json = """{"uid": "$uid"}"""
    val mediaType = "application/json; charset=utf-8".toMediaType()
    val body = json.toRequestBody(mediaType)

    val request = Request.Builder()
        .url("https://us-central1-doan3-65701.cloudfunctions.net/setAdminRole")
        .post(body)
        .build()

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = client.newCall(request).execute()
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    println("✅ Đã gửi UID thành công: $uid")
                } else {
                    println("❌ Gửi UID thất bại: ${response.message}")
                }
            }
        } catch (e: Exception) {
            println("❌ Lỗi khi gửi UID: ${e.message}")
        }
    }
}



