package com.example.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class PlaylistSortOption(val displayName: String) {
    CUSTOM_ORDER("Custom order"),
    TITLE("Title"),
    ARTIST("Artist"),
    ALBUM("Album"),
    RECENTLY_ADDED("Recently added")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistSortBottomSheet(
    currentSort: PlaylistSortOption,
    onSortSelected: (PlaylistSortOption) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = Color(0xFF1E1E1E),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color.DarkGray,
                width = 36.dp,
                height = 4.dp
            )
        },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp, top = 4.dp)
        ) {
            Text(
                text = "Sort by",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp, top = 4.dp)
            )

            PlaylistSortOption.values().forEach { option ->
                val isSelected = currentSort == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            onSortSelected(option)
                            onDismissRequest()
                        }
                        .padding(vertical = 14.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.displayName,
                        color = if (isSelected) Color(0xFF1ED760) else Color.White,
                        fontSize = 16.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )

                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF1ED760),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
