package com.eritlab.jexmon.presentation.screens.checkout_screen

import android.util.Log
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.eritlab.jexmon.domain.model.VoucherModel
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.ui.theme.TextColor
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun VoucherScreen(
    navController: NavController,
    viewModel: CheckoutViewModel,
    onBackClick: () -> Unit,

) {
    val db = FirebaseFirestore.getInstance()
    val vouchers by viewModel.vouchers
    val selectedVoucher = remember { mutableStateOf<VoucherModel?>(null) } // Voucher được chọn
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchVouchers()

    }


    Column(modifier = Modifier.fillMaxSize()) {
        // Phần header
        Row(
            modifier = Modifier
                .padding(top = 15.dp, start = 15.dp, end = 15.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(0.5f)) {
                DefaultBackArrow {
                    onBackClick()
                }
            }
            Box(modifier = Modifier.weight(0.7f)) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Chọn Voucher",
                        color = MaterialTheme.colors.TextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp)
                    .height(600.dp),
            ) {
                items(vouchers.size) { index ->
                    val voucher = vouchers[index]
                    VoucherItemScreen(
                        voucher = voucher,
                        onClick = {
                            Log.d("VoucherScreen", "Selected voucher: ${voucher.code}")
                            viewModel.selectVoucher(
                                code = voucher.code,
                                onSuccess = {
                                    // Sau khi chọn thành công, bạn có thể:
                                    viewModel.fetchVouchers() // Reload lại danh sách voucher nếu muốn UI cập nhật
                                    navController.popBackStack()
                                },
                                onError = { errorMessage ->
                                    Log.e("VoucherScreen", "Lỗi chọn voucher: $errorMessage")
                                }
                            )
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

            }
        }

    }

@Composable
fun VoucherItemScreen(
    voucher: VoucherModel,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = RoundedCornerShape(8.dp))
            .border(1.dp, Color.LightGray, shape = RoundedCornerShape(8.dp)) // <-- Thêm border ở đây

            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon voucher
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color(0xFFE0F7FA), shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🎁",
                    fontSize = 24.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = voucher.code,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = voucher.detail,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Giảm ${voucher.discount}% - Hết hạn: ${voucher.expiryDate?.time}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }


            Spacer(modifier = Modifier.width(8.dp))

            // Ô tròn để chọn
            // Ô tròn để chọn
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, Color.Gray, shape = CircleShape)
            ) {
                if (voucher.chon) {
                    Box(
                        modifier = Modifier
                            .size(16.dp) // nhỏ hơn viền ngoài
                            .background(Color(0xFFEB5757), shape = CircleShape) // màu gạch (đỏ đỏ)
                    )
                }
            }

        }
    }
}