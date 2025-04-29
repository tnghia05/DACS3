package com.eritlab.jexmon.domain.model

import com.google.firebase.Timestamp

data class UserModel(
    val userId: String = "",  // Lấy từ Firebase Auth
    val email: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val createdAt: Timestamp = Timestamp.now()

)
