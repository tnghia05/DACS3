package com.eritlab.jexmon.presentation.screens.products.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eritlab.jexmon.domain.model.ProductFilter
import com.eritlab.jexmon.domain.model.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    currentFilter: ProductFilter,
    onFilterApply: (ProductFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var priceRange by remember { mutableStateOf(currentFilter.priceRange) }
    var rating by remember { mutableStateOf(currentFilter.rating) }
    var sortBy by remember { mutableStateOf(currentFilter.sortBy) }
    var selectedCategories by remember { mutableStateOf(currentFilter.categories) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Bộ lọc nâng cao",
            style = MaterialTheme.typography.titleLarge
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Khoảng giá
        Text(
            text = "Khoảng giá",
            style = MaterialTheme.typography.titleMedium
        )
        RangeSlider(
            value = priceRange,
            onValueChange = { priceRange = it },
            valueRange = 0f..10000000f,
            steps = 100
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "${priceRange.start.toInt()}đ",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "${priceRange.endInclusive.toInt()}đ",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Đánh giá tối thiểu
        Text(
            text = "Đánh giá tối thiểu",
            style = MaterialTheme.typography.titleMedium
        )
        Slider(
            value = rating,
            onValueChange = { rating = it },
            valueRange = 0f..5f,
            steps = 4
        )
        Text(
            "${rating.toInt()} sao trở lên",
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sắp xếp theo
        Text(
            text = "Sắp xếp theo",
            style = MaterialTheme.typography.titleMedium
        )
        SortOption.values().forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = sortBy == option,
                    onClick = { sortBy = option }
                )
                Text(
                    text = when (option) {
                        SortOption.RELEVANCE -> "Liên quan"
                        SortOption.PRICE_LOW_TO_HIGH -> "Giá thấp đến cao"
                        SortOption.PRICE_HIGH_TO_LOW -> "Giá cao đến thấp"
                        SortOption.RATING -> "Đánh giá cao nhất"
                        SortOption.NEWEST -> "Mới nhất"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Nút áp dụng
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Áp dụng")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
} 