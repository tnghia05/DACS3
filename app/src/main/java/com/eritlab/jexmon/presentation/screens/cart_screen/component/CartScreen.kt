package com.eritlab.jexmon.presentation.screens.cart_screen.component

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.eritlab.jexmon.R
import com.eritlab.jexmon.presentation.common.CustomDefaultBtn
import com.eritlab.jexmon.presentation.common.component.DefaultBackArrow
import com.eritlab.jexmon.presentation.screens.cart_screen.CartViewModel
import com.eritlab.jexmon.presentation.ui.theme.PrimaryColor
import com.eritlab.jexmon.presentation.ui.theme.PrimaryLightColor
import com.eritlab.jexmon.presentation.ui.theme.TextColor


@Composable
fun CartScreen(
    viewModel: CartViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    var itemDrag by remember { mutableStateOf(0f) }
    val cartItems by viewModel.cartItems
    val totalAmount by viewModel.totalAmount

    Column(modifier = Modifier.fillMaxSize()) {

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
                        text = "Your Cart",
                        color = MaterialTheme.colors.TextColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${cartItems.size} items",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.TextColor,
                    )

                }
            }

        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(630.dp)
        ) {
            Log.d("CartScreen", "Cart items: ${cartItems.size}")
            fun convertToHex(color: String): String {
                val colorMap = mapOf(
                    "red" to "#FF0000",
                    "blue" to "#0000FF",
                    "green" to "#008000",
                    "yellow" to "#FFFF00",
                    "black" to "#000000",
                    "white" to "#FFFFFF",
                    "gray" to "#808080",
                    "purple" to "#800080",
                    "orange" to "#FFA500",
                    "pink" to "#FFC0CB"
                )

                return colorMap[color.lowercase()] ?: color // Nếu không tìm thấy, giả định nó là mã hex
            }
            fun convertToColorName(hex: String): String {
                val colorMap = mapOf(
                    "#FF0000" to "Red",
                    "#0000FF" to "Blue",
                    "#008000" to "Green",
                    "#FFFF00" to "Yellow",
                    "#000000" to "Black",
                    "#FFFFFF" to "White",
                    "#808080" to "Gray",
                    "#800080" to "Purple",
                    "#FFA500" to "Orange",
                    "#FFC0CB" to "Pink"
                )

                return colorMap[hex.uppercase()] ?: hex // Nếu không tìm thấy, giữ nguyên mã hex
            }

            items(cartItems) { cartItem ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures { change, dragAmount ->
                                itemDrag = dragAmount
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Log.d("CartScreen", "Cart item: ${cartItem.name}")
                    Log.d("CartScreen", "Item img: ${cartItem.imageUrl}")
                    Image(
                        painter = rememberAsyncImagePainter(cartItem.imageUrl),
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color(0x8DB3B0B0), shape = RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column {
                        Text(
                            text = cartItem.name,

                            fontWeight = FontWeight(700),
                            fontSize = 16.sp,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val colorName = convertToColorName(cartItem.color)
                        Row {
                            Text(
                                text = "Size: ${cartItem.size} " ,
                                fontSize = 16.sp,

                                )
                            Text(
                            text = "Color: $colorName",
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row {
                            val discountedPrice = cartItem.price
                            Text(
                                text = "$${String.format("%.2f", discountedPrice)}",
                                color = MaterialTheme.colors.PrimaryColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "  x${cartItem.quantity}",
                                color = MaterialTheme.colors.TextColor
                            )
                        }
                    }
                    
                    // Nút điều chỉnh số lượng
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.updateQuantity(cartItem.id!!, cartItem.quantity - 1) }
                        ) {
                            Text("-", fontSize = 20.sp)
                        }
                        
                        Text(
                            text = cartItem.quantity.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        IconButton(
                            onClick = { viewModel.updateQuantity(cartItem.id!!, cartItem.quantity + 1) }
                        ) {
                            Text("+", fontSize = 20.sp)
                        }
                    IconButton(
                            onClick = { viewModel.removeItem(cartItem) }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.trash),
                                contentDescription = null,
                                tint = MaterialTheme.colors.PrimaryColor
                            )
                        }
                    }
                }
            }
        }




        var isVoucherVisible by remember { mutableStateOf(false) }
        var voucherCode by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colors.PrimaryLightColor,
                    shape = RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp)
                )
                .clip(RoundedCornerShape(topStart = 15.dp, topEnd = 15.dp))
                .padding(15.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isVoucherVisible = !isVoucherVisible }, // 👈 Toggle
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.receipt),
                    contentDescription = null,
                    tint = MaterialTheme.colors.PrimaryColor,
                    modifier = Modifier
                        .size(45.dp)
                        .background(Color(0x8DB3B0B0), shape = RoundedCornerShape(15.dp))
                        .padding(10.dp)
                        .clip(RoundedCornerShape(15.dp))
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("Add voucher code", style = MaterialTheme.typography.body1)
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_right),
                        contentDescription = null,
                    )
                }
            }

            // 👇 Hiển thị trượt TextField
            AnimatedVisibility(visible = isVoucherVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextField(
                        value = voucherCode,
                        onValueChange = { voucherCode = it },
                        placeholder = { Text("Enter voucher code") },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White, RoundedCornerShape(10.dp))
                            .clip(RoundedCornerShape(10.dp)),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            viewModel.checkVoucher(
                                voucherCode,
                                onSuccess = {
                                    Log.d("CartViewModel", "Thanh Cong")
                                    isVoucherVisible = false
                                    voucherCode = ""
                                },
                                onError = { error ->
                                    Log.d("CartViewModel", "Lỗi: $error")

                                    // TODO: Hiển thị thông báo lỗi
                                }
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        enabled = voucherCode.isNotEmpty()
                    ) {
                        Text("Sử dụng")
                    }
                }
            }


        //btn
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column() {
                    Text(text = "Total")
                    val discountAmountValue by viewModel.discountAmount
                    val appliedVoucherValue by viewModel.appliedVoucher
                    
                    if (discountAmountValue > 0) {
                        Text(
                            text = "Giảm giá: ${String.format("%.2f", discountAmountValue)}%",
                            fontSize = 14.sp,
                            color = MaterialTheme.colors.PrimaryColor
                        )
                    }
                    
                    Text(
                        text = "$${String.format("%.2f", totalAmount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.PrimaryColor
                    )
                    
                    if (appliedVoucherValue != null) {
                        Row(
                            modifier = Modifier.clickable { viewModel.removeVoucher() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = appliedVoucherValue!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colors.PrimaryColor
                            )
                            Icon(
                                painter = painterResource(id = R.drawable.trash),
                                contentDescription = "Remove voucher",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colors.PrimaryColor
                            )
                        }
                    }

                }
                Box(
                    modifier = Modifier
                        .width(150.dp)
                ) {
                    CustomDefaultBtn(shapeSize = 15f, btnText = "Check Out") {

                    }
                }

            }


        }

    }
}