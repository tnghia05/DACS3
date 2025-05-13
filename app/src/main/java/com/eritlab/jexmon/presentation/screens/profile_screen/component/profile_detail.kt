package com.eritlab.jexmon.presentation.screens.profile_screen.component

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

// Data model cho user profile
data class UserProfile(
    val name: String = "",
    val gender: String = "",
    val birthday: String = "",
    val sdt: String = "",
    val gmail: String = "",
    val avatarUrl: String = ""
)

suspend fun uploadAvatarToFirebase(uid: String, uri: Uri): String? {
    val storageRef = FirebaseStorage.getInstance().reference.child("avatars/$uid.jpg")
    storageRef.putFile(uri).await()
    return storageRef.downloadUrl.await().toString()
}

fun validateProfile(name: String, sdt: String, birthday: String): String? {
    if (name.isBlank()) return "Tên không được để trống"
    if (!sdt.matches(Regex("^\\d{9,11}$"))) return "Số điện thoại không hợp lệ"
    if (!birthday.matches(Regex("^\\d{2}/\\d{2}/\\d{4}$"))) return "Ngày sinh phải đúng định dạng dd/MM/yyyy"
    return null
}

@Composable
fun EditProfileScreen(uid: String) {
    val db = FirebaseFirestore.getInstance()
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }

    // Lấy dữ liệu user từ Firestore
    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            Log.d("EditProfileScreen", "Fetching user data for UID: $uid")
            try {
                val doc = db.collection("user").document(uid).get().await()
                userProfile = doc.toObject(UserProfile::class.java)
                Log.d("EditProfileScreen", "User data fetched: $userProfile")
                isLoading = false
            } catch (e: Exception) {
                error = "Lỗi tải dữ liệu: ${e.message}"
                isLoading = false
            }
        } else {
            error = "Không tìm thấy người dùng."
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (error.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(error, color = MaterialTheme.colors.error)
        }
        return
    }
    userProfile?.let { profile ->
        EditProfileForm(
            initialProfile = profile,
            onSave = { updatedProfile ->
                if (uid.isNotEmpty()) {
                    db.collection("user").document(uid).set(updatedProfile)
                }
            },
            uid = uid
        )
    }
}

@Composable
fun AvatarPicker(
    avatarUrl: String?,
    avatarUri: Uri?,
    onImageSelected: (Uri) -> Unit
) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (avatarUri != null) {
            // Hiển thị ảnh vừa chọn (preview)
            Image(
                painter = rememberAsyncImagePainter(avatarUri),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
        } else if (!avatarUrl.isNullOrEmpty()) {
            // Hiển thị ảnh từ server
            Image(
                painter = rememberAsyncImagePainter(avatarUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
        } else {
            // Icon mặc định
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
            )
        }
        Button(onClick = { launcher.launch("image/*") }) {
            Text("Chọn ảnh đại diện")
        }
    }
}

@Composable
fun EditProfileForm(
    initialProfile: UserProfile,
    onSave: (UserProfile) -> Unit,
    uid: String
) {
    var name by remember { mutableStateOf(TextFieldValue(initialProfile.name)) }
    var gender by remember { mutableStateOf(TextFieldValue(initialProfile.gender)) }
    var birthday by remember { mutableStateOf(TextFieldValue(initialProfile.birthday)) }
    var sdt by remember { mutableStateOf(TextFieldValue(initialProfile.sdt)) }
    var gmail by remember { mutableStateOf(TextFieldValue(initialProfile.gmail)) }
    var avatarUrl by remember { mutableStateOf(initialProfile.avatarUrl) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var saveMessage by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var shouldUploadAvatar by remember { mutableStateOf(false) }

    // Xử lý upload avatar và lưu thông tin trong LaunchedEffect
    LaunchedEffect(shouldUploadAvatar) {
        if (shouldUploadAvatar) {
            isSaving = true
            if (avatarUri != null) {
                val url = uploadAvatarToFirebase(uid, avatarUri!!)
                avatarUrl = url ?: ""
            }
            onSave(
                UserProfile(
                    name = name.text,
                    gender = gender.text,
                    birthday = birthday.text,
                    sdt = sdt.text,
                    gmail = gmail.text,
                    avatarUrl = avatarUrl
                )
            )
            saveMessage = "Đã lưu thành công!"
            isSaving = false
            shouldUploadAvatar = false
        }
    }

    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Sửa hồ sơ", style = MaterialTheme.typography.h6)
        Spacer(modifier = Modifier.height(16.dp))
        AvatarPicker(
            avatarUrl = avatarUrl,
            avatarUri = avatarUri,
            onImageSelected = { uri -> avatarUri = uri }
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Tên") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = gender,
            onValueChange = { gender = it },
            label = { Text("Giới tính") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = birthday,
            onValueChange = { birthday = it },
            label = { Text("Ngày sinh (dd/MM/yyyy)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = sdt,
            onValueChange = { sdt = it },
            label = { Text("Số điện thoại") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = gmail,
            onValueChange = { gmail = it },
            label = { Text("gmail") },
            modifier = Modifier.fillMaxWidth(),
            enabled = false // Không cho sửa gmail
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                val error = validateProfile(name.text, sdt.text, birthday.text)
                if (error != null) {
                    errorMsg = error
                    saveMessage = ""
                    return@Button
                }
                errorMsg = ""
                saveMessage = ""
                shouldUploadAvatar = true // Trigger upload và lưu
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Lưu")
        }
        if (errorMsg.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(errorMsg, color = MaterialTheme.colors.error)
        }
        if (saveMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(saveMessage, color = MaterialTheme.colors.primary)
        }
    }
}
