package com.eritlab.jexmon.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.eritlab.jexmon.presentation.graphs.auth_graph.AuthScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _signUpState = MutableStateFlow<SignUpState>(SignUpState.Idle)
    val signUpState: StateFlow<SignUpState> = _signUpState

    fun signUp(email: String, password: String, firstName: String, lastName: String, phoneNumber: String, address: String) {
        viewModelScope.launch {
            try {
                Log.d("SignUpViewModel", "Đang tạo tài khoản cho: $email")
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null) {
                    Log.d("SignUpViewModel", "Tài khoản được tạo: ${user.uid}")
                    user.sendEmailVerification().await()
                    Log.d("SignUpViewModel", "Email xác minh đã được gửi đến: ${user.email}")
                    _signUpState.value = SignUpState.OTPSent
                } else {
                    Log.e("SignUpViewModel", "Lỗi: user null sau khi tạo tài khoản")
                    _signUpState.value = SignUpState.Error("Tạo tài khoản thất bại")
                }
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Lỗi khi tạo tài khoản", e)
                _signUpState.value = SignUpState.Error(e.message ?: "Registration failed")
            }
        }
    }

    private fun saveUserToFirestore(userId: String, email: String, firstName: String, lastName: String, phoneNumber: String, address: String) {
        val user = hashMapOf(
            "userId" to userId,
            "email" to email,
            "firstName" to firstName,
            "lastName" to lastName,
            "phoneNumber" to phoneNumber,
            "address" to address
        )

        Log.d("SignUpViewModel", "Lưu thông tin người dùng vào Firestore: $userId")

        firestore.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener {
                Log.d("SignUpViewModel", "Dữ liệu người dùng đã được lưu thành công")
                sendEmailVerification()
                _signUpState.value = SignUpState.Success
            }
            .addOnFailureListener { e ->
                Log.e("SignUpViewModel", "Lỗi khi lưu dữ liệu người dùng", e)
                _signUpState.value = SignUpState.Error(e.message ?: "Failed to save user data")
            }
    }

    private fun sendEmailVerification() {
        auth.currentUser?.let { user ->
            Log.d("SignUpViewModel", "Gửi email xác minh đến: ${user.email}")
            user.sendEmailVerification()
                .addOnSuccessListener {
                    Log.d("SignUpViewModel", "Email xác minh đã được gửi thành công")
                }
                .addOnFailureListener { e ->
                    Log.e("SignUpViewModel", "Lỗi khi gửi email xác minh", e)
                }
        } ?: Log.e("SignUpViewModel", "Không tìm thấy người dùng để gửi email xác minh")
    }

    fun checkEmailVerificationAndSaveUser(navController: NavController, userId: String, user: FirebaseUser) {
        viewModelScope.launch {
            try {
                Log.d("SignUpViewModel", "Kiểm tra xác minh email cho: ${user.email}")

                auth.currentUser?.reload()?.await()
                if (auth.currentUser?.isEmailVerified == true) {
                    Log.d("SignUpViewModel", "Email đã được xác minh")

                    firestore.collection("users").document(userId).set(
                        mapOf(
                            "userId" to user.uid,
                            "email" to user.email
                        )
                    ).await()

                    _signUpState.value = SignUpState.Success
                    Log.d("SignUpViewModel", "Người dùng đã được lưu vào Firestore, chuyển đến SignInScreen")

                    // Điều hướng đến màn hình SignInSuccess khi xác minh thành công
                    navController.navigate(AuthScreen.SignInSuccess.route) {
                        popUpTo(AuthScreen.SignUpScreen.route) { inclusive = true }
                    }
                } else {
                    Log.e("SignUpViewModel", "Email chưa được xác minh")
                    _signUpState.value = SignUpState.Error("Vui lòng xác minh email trước.")
                }
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Lỗi khi kiểm tra xác minh email", e)
                _signUpState.value = SignUpState.Error(e.message ?: "Error checking email verification")
            }
        }
    }
}
    sealed class SignUpState {
    object Idle : SignUpState()
    object Loading : SignUpState()
    object Success : SignUpState()
    object OTPSent : SignUpState()
    data class Error(val message: String) : SignUpState()
}
