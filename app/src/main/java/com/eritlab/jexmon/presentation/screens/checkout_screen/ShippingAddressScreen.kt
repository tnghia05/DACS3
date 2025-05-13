package com.eritlab.jexmon.presentation.screens.checkout_screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AlertDialog
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.eritlab.jexmon.domain.model.AddressModel
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ShippingAddressScreen(
    navController: NavController,
    onBackClick: () -> Unit,
) {
    val db = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: ""
    var addresses by remember { mutableStateOf(listOf<AddressModel>()) }
    var selectedAddressId by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var editingAddress by remember { mutableStateOf<AddressModel?>(null) }

    // Lấy danh sách địa chỉ của user
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            db.collection("address")
                .whereEqualTo("iduser", userId)
                .get()
                .addOnSuccessListener { result ->
                    addresses = result.documents.mapNotNull { doc ->
                        doc.toObject(AddressModel::class.java)?.copy(id = doc.id)
                    }
                }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .padding(top = 15.dp, start = 15.dp, end = 15.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(0.5f)) {
                DefaultBackArrow { onBackClick() }
            }
            Box(modifier = Modifier.weight(0.7f)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Chọn địa chỉ giao hàng",
                        color = MaterialTheme.colors.TextColor,
                        fontSize = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .height(600.dp)
        ) {
            items(addresses.size) { index ->
                val address = addresses[index]
                AddressItemScreen(
                    address = address,
                    isSelected = address.isDefault,
                    onClick = {
                        // Khi chọn, cập nhật isDefault cho tất cả địa chỉ của user hiện tại
                        addresses.filter { it.iduser == userId }.forEach { addr ->
                            val isDefault = addr.id == address.id
                            db.collection("address").document(addr.id)
                                .update("isDefault", isDefault)
                        }
                        selectedAddressId = address.id
                        navController.popBackStack()
                    },
                    onEdit = { addr ->
                        editingAddress = addr
                        showDialog = true
                    },
                    onDelete = { addr ->
                        db.collection("address").document(addr.id).delete()
                        db.collection("address")
                            .whereEqualTo("iduser", userId)
                            .get()
                            .addOnSuccessListener { result ->
                                addresses = result.documents.mapNotNull { doc ->
                                    doc.toObject(AddressModel::class.java)?.copy(id = doc.id)
                                }
                            }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        FloatingActionButton(
            onClick = {
                editingAddress = null // Thêm mới
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.End)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm địa chỉ")
        }

        if (showDialog) {
            AddressDialog(
                initial = editingAddress,
                onDismiss = { showDialog = false },
                onSave = { address ->
                    val db = FirebaseFirestore.getInstance()
                    if (address.id.isEmpty()) {
                        // Thêm mới
                        db.collection("address").add(address.copy(iduser = userId)).addOnSuccessListener {
                            // Reload lại danh sách sau khi thêm
                            db.collection("address")
                                .whereEqualTo("iduser", userId)
                                .get()
                                .addOnSuccessListener { result ->
                                    addresses = result.documents.mapNotNull { doc ->
                                        doc.toObject(AddressModel::class.java)?.copy(id = doc.id)
                                    }
                                }
                        }
                    } else {
                        // Sửa
                        db.collection("address").document(address.id).set(address).addOnSuccessListener {
                            // Reload lại danh sách sau khi sửa
                            db.collection("address")
                                .whereEqualTo("iduser", userId)
                                .get()
                                .addOnSuccessListener { result ->
                                    addresses = result.documents.mapNotNull { doc ->
                                        doc.toObject(AddressModel::class.java)?.copy(id = doc.id)
                                    }
                                }
                        }
                    }
                    showDialog = false
                }
            )
        }
    }
}

@Composable
fun AddressItemScreen(
    address: AddressModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: (AddressModel) -> Unit,
    onDelete: (AddressModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .border(
                1.dp,
                if (isSelected) Color(0xFFEB5757) else Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon hoặc avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE0F7FA), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🏠", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(address.name, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(address.sdt, fontSize = 14.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(address.diachi, fontSize = 14.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Ô tròn chọn
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, Color.Gray, shape = CircleShape)
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(Color(0xFFEB5757), shape = CircleShape)
                    )
                }
            }
            IconButton(onClick = { onEdit(address) }) {
                Icon(Icons.Default.Edit, contentDescription = "Sửa")
            }
            IconButton(onClick = { onDelete(address) }) {
                Icon(Icons.Default.Delete, contentDescription = "Xóa")
            }
        }
    }
}

@Composable
fun AddressDialog(
    initial: AddressModel?,
    onDismiss: () -> Unit,
    onSave: (AddressModel) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var sdt by remember { mutableStateOf(initial?.sdt ?: "") }
    var diachi by remember { mutableStateOf(initial?.diachi ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Thêm địa chỉ" else "Sửa địa chỉ") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên") })
                OutlinedTextField(value = sdt, onValueChange = { sdt = it }, label = { Text("Số điện thoại") })
                OutlinedTextField(value = diachi, onValueChange = { diachi = it }, label = { Text("Địa chỉ") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(AddressModel(
                    id = initial?.id ?: "",
                    name = name,
                    sdt = sdt,
                    diachi = diachi,
                    iduser = initial?.iduser ?: ""
                ))
            }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
