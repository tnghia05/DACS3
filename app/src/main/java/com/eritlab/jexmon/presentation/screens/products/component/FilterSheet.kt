package com.eritlab.jexmon.presentation.screens.products.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eritlab.jexmon.domain.model.ProductFilter
import com.eritlab.jexmon.domain.model.SortOption
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    currentFilter: ProductFilter,
    onFilterApply: (ProductFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val ShopeeOrange = Color(0xFFEE4D2D)
    val ShopeeSearchBackground = Color(0xFFF5F5F5)
    var priceRange by remember { mutableStateOf(currentFilter.priceRange) }
    var rating by remember { mutableStateOf(currentFilter.rating) }
    var sortBy by remember { mutableStateOf(currentFilter.sortBy) }
    var selectedCategories by remember { mutableStateOf(currentFilter.categories) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .background(Color.White)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        ) {
            Text(
                text = "Bộ lọc",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.Black
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Divider()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Sắp xếp theo
            Text(
                text = "Sắp xếp theo",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SortOption.values().forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = sortBy == option,
                        onClick = { sortBy = option },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = ShopeeOrange
                        )
                    )
                    Text(
                        text = when (option) {
                            SortOption.RELEVANCE -> "Liên quan"
                            SortOption.PRICE_LOW_TO_HIGH -> "Giá thấp đến cao"
                            SortOption.PRICE_HIGH_TO_LOW -> "Giá cao đến thấp"
                            SortOption.RATING -> "Đánh giá cao nhất"
                            SortOption.NEWEST -> "Mới nhất"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()

            // Khoảng giá
            Text(
                text = "Khoảng giá",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            RangeSlider(
                value = priceRange,
                onValueChange = { priceRange = it },
                valueRange = 0f..50000000f,
                steps = 99,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = ShopeeOrange,
                    activeTrackColor = ShopeeOrange,
                    inactiveTrackColor = ShopeeSearchBackground
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatPrice(priceRange.start.toDouble()),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatPrice(priceRange.endInclusive.toDouble()),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()

            // Đánh giá
            Text(
                text = "Đánh giá",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 0f..5f,
                steps = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = SliderDefaults.colors(
                    thumbColor = ShopeeOrange,
                    activeTrackColor = ShopeeOrange,
                    inactiveTrackColor = ShopeeSearchBackground
                )
            )

            Text(
                text = "${rating.toInt()} sao trở lên",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bottom buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { onDismiss() },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = ShopeeOrange
                ),
                border = BorderStroke(1.dp, ShopeeOrange)
            ) {
                Text("Đặt lại")
            }

            Button(
                onClick = {
                    onFilterApply(
                        ProductFilter(
                            priceRange = priceRange,
                            rating = rating,
                            sortBy = sortBy,
                            categories = selectedCategories
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ShopeeOrange,
                    contentColor = Color.White
                )
            ) {
                Text("Áp dụng")
            }
        }
    }
}

private fun formatPrice(price: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
    return format.format(price)
} 