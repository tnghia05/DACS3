package com.eritlab.jexmon.presentation.screens.products.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eritlab.jexmon.domain.model.SearchHistoryModel

val ShopeeOrange = Color(0xFFEE4D2D)
val ShopeeSearchBackground = Color(0xFFF5F5F5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    suggestions: List<String>,
    searchHistory: List<SearchHistoryModel>,
    onFilterClick: () -> Unit,
    onDeleteSearchHistory: (String) -> Unit,
    modifier: Modifier = Modifier,
    onBackBtnClick: () -> Unit,


    ) {
    var isExpanded by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button
        IconButton(
            onClick = onBackBtnClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = ShopeeOrange
            )
        }

        // Search field
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .background(Color.White, RoundedCornerShape(2.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
             
                
                TextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        showHistory = false
                        isExpanded = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(
                            width = 0.5.dp,
                            color = ShopeeOrange,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                showHistory = query.isEmpty()
                                isExpanded = query.isNotEmpty()
                            }
                        },
                    placeholder = {
                        Text(
                            "Tìm sản phẩm.",
                            color = Color.Gray
                        )


                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = ShopeeOrange,
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedPlaceholderColor = Color.Gray.copy(alpha = 0.7f),
                        unfocusedPlaceholderColor = Color.Gray.copy(alpha = 0.7f)
                    ),
                    singleLine = true

                )


                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onQueryChange("")
                            showHistory = true
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.Gray
                        )
                    }
                }
            }
        }

        // Filter button
        IconButton(
            onClick = onFilterClick,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(40.dp)
        ) {
            Icon(
                Icons.Default.Create,
                contentDescription = "Filter",
                tint = ShopeeOrange
            )
        }
    }

    // Suggestions and history
    if (isExpanded || showHistory) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            LazyColumn {
                if (showHistory && searchHistory.isNotEmpty()) {
                    items(searchHistory.sortedByDescending { it.timestamp }) { history ->
                        SearchItem(
                            text = history.query,
                            icon = Icons.Default.Search,
                            onClick = {
                                onQueryChange(history.query)
                                onSearch(history.query)
                                isExpanded = false
                                showHistory = false
                            },
                            onDelete = { onDeleteSearchHistory(history.query) }
                        )
                    }
                } else if (!showHistory && suggestions.isNotEmpty()) {
                    items(suggestions) { suggestion ->
                        SearchItem(
                            text = suggestion,
                            icon = Icons.Default.Search,
                            onClick = {
                                onQueryChange(suggestion)
                                onSearch(suggestion)
                                isExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (onDelete != null) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Delete",
                    tint = Color.Gray
                )
            }
        }
    }
} 