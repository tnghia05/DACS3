package com.eritlab.jexmon.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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

    fun signUp(
        email: String, 
        password: String, 
        name: String,
        phoneNumber: String,
        birthday: String,
        gender: String
    ) {
        viewModelScope.launch {
            try {
                _signUpState.value = SignUpState.Loading
                Log.d("SignUpViewModel", "Đang tạo tài khoản cho: $email")
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user

                if (user != null) {
                    Log.d("SignUpViewModel", "Tài khoản được tạo: ${user.uid}")
                    saveUserToFirestore(user.uid, email, name, phoneNumber, birthday, gender)
                    user.sendEmailVerification().await()
                    Log.d("SignUpViewModel", "Email xác minh đã được gửi đến: ${user.email}")
                    _signUpState.value = SignUpState.OTPSent
                } else {
                    Log.e("SignUpViewModel", "Lỗi: user null sau khi tạo tài khoản")
                    _signUpState.value = SignUpState.Error("Tạo tài khoản thất bại")
                }
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Lỗi khi tạo tài khoản", e)
                _signUpState.value = SignUpState.Error(e.message ?: "Đăng ký thất bại")
            }
        }
    }

    private fun saveUserToFirestore(
        userId: String,
        email: String,
        name: String,
        phoneNumber: String,
        birthday: String,
        gender: String
    ) {
        val user = hashMapOf(
            "userId" to userId,
            "gmail" to email,
            "name" to name,
            "sdt" to phoneNumber,
            "birthday" to birthday,
            "gender" to gender
        )

        Log.d("SignUpViewModel", "Lưu thông tin người dùng vào Firestore: $userId")

        firestore.collection("user").document(userId)
            .set(user)
            .addOnSuccessListener {
                Log.d("SignUpViewModel", "Dữ liệu người dùng đã được lưu thành công")
            }
            .addOnFailureListener { e ->
                Log.e("SignUpViewModel", "Lỗi khi lưu dữ liệu người dùng", e)
                _signUpState.value = SignUpState.Error(e.message ?: "Lỗi khi lưu thông tin người dùng")
            }
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user != null) {
                    user.reload().await()
                    
                    if (user.isEmailVerified) {
                        Log.d("SignUpViewModel", "Email đã được xác minh")
                        _signUpState.value = SignUpState.Success
                    } else {
                        Log.d("SignUpViewModel", "Email chưa được xác minh")
                        _signUpState.value = SignUpState.Error("Vui lòng xác minh email của bạn trước khi tiếp tục")
                    }
                } else {
                    _signUpState.value = SignUpState.Error("Không tìm thấy thông tin người dùng")
                }
            } catch (e: Exception) {
                Log.e("SignUpViewModel", "Lỗi khi kiểm tra xác minh email", e)
                _signUpState.value = SignUpState.Error(e.message ?: "Lỗi khi kiểm tra xác minh email")
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
