package com.eritlab.jexmon.presentation.dashboard_screen.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.NavController
import com.eritlab.jexmon.R
import com.eritlab.jexmon.domain.model.BrandModel
import com.eritlab.jexmon.presentation.ui.theme.PrimaryColor
import com.eritlab.jexmon.presentation.ui.theme.PrimaryLightColor
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AppBar(
    navController: NavController,
    isVisible: Boolean,
    searchCharSequence: (String) -> Unit,
    onNotificationIconClick: () -> Unit,
    onCartIconClick: () -> Unit,
    onFilterApply:  (String, String) -> Unit // brand, sort
) {
    var typedText by remember { mutableStateOf(TextFieldValue()) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var selectedBrand by remember { mutableStateOf(BrandModel()) }
    var selectedSort by remember { mutableStateOf("Mặc định") }
    var expandedBrand by remember { mutableStateOf(false) }
    var expandedSort by remember { mutableStateOf(false) }
    var brands by remember { mutableStateOf(listOf<BrandModel>()) }
    val sortOptions = listOf("Mặc định", "Giá thấp đến cao", "Giá cao đến thấp")
    
    // Lấy danh sách brands từ Firestore
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("brands")
            .get()
            .addOnSuccessListener { documents ->
                brands = documents.map { doc ->
                    BrandModel(
                        id = doc.id,
                        name = doc.getString("name") ?: ""
                    )
                }.toMutableList().apply {
                    add(0, BrandModel(id = "all", name = "Tất cả")) // Thêm option "Tất cả" vào đầu danh sách
                }
                selectedBrand = brands.first() // Mặc định chọn "Tất cả"
            }
            .addOnFailureListener { e ->
                Log.e("AppBar", "Error getting brands: ", e)
            }
    }
    val scope = rememberCoroutineScope()

    if (isVisible) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp, top = 30.dp, bottom = 30.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            TextField(
                value = typedText,
                onValueChange = { newText ->
                    typedText = newText
                    searchCharSequence(newText.text)
                },
                singleLine = true,
                placeholder = { Text(text = "Search product") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.search_icon),
                        contentDescription = "Product Search Icon"
                    )
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Text),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colors.PrimaryColor
                ),
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colors.PrimaryLightColor,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .weight(1f),
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.PrimaryLightColor)
                    .clickable {
                        onCartIconClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cart_icon),
                    contentDescription = "Cart Icon"
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colors.PrimaryLightColor)
                    .clickable { showFilterDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.flash_icon),
                    contentDescription = "Filter Icon",
                    tint = MaterialTheme.colors.PrimaryColor
                )
            }

            ConstraintLayout {
                val (notification, notificationCounter) = createRefs()

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.PrimaryLightColor)
                        .constrainAs(notification) {}
                        .clickable {
                            onNotificationIconClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.bell),
                        contentDescription = "Notification Icon"
                    )
                }
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(color = Color.Red, shape = CircleShape)
                        .padding(3.dp)
                        .constrainAs(notificationCounter) {
                            top.linkTo(notification.top)
                            end.linkTo(notification.end)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "3", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Lọc sản phẩm") },
                text = {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(text = "Chọn thương hiệu:")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.surface, RoundedCornerShape(8.dp))
                                .clickable { expandedBrand = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = selectedBrand.name)
                                Icon(
                                    painter = painterResource(id = if (expandedBrand) R.drawable.back_icon else R.drawable.arrow_right),
                                    contentDescription = "Dropdown Arrow"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expandedBrand,
                            onDismissRequest = { expandedBrand = false }
                        ) {
                            brands.forEach { brand ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedBrand = brand
                                        expandedBrand = false
                                    }
                                ) {
                                    Text(text = brand.name)
                                }
                            }
                        }

                        Text(text = "Sắp xếp theo giá:")
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colors.surface, RoundedCornerShape(8.dp))
                                .clickable { expandedSort = true }
                                .padding(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(text = selectedSort)
                                Icon(
                                    painter = painterResource(id = if (expandedSort) R.drawable.back_icon else R.drawable.arrow_right),
                                    contentDescription = "Dropdown Arrow"
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = expandedSort,
                            onDismissRequest = { expandedSort = false }
                        ) {
                            sortOptions.forEach { sort ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedSort = sort
                                        expandedSort = false
                                    }
                                ) {
                                    Text(text = sort)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // Gọi suspend function trong coroutine
                            scope.launch {
                                // Gọi hàm suspend filterProductsByBrand trong ViewModel
                                onFilterApply(selectedBrand.id, selectedSort)
                            }
                            showFilterDialog = false
                        }
                    ) {
                        Text("Áp dụng")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showFilterDialog = false }
                    ) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}
