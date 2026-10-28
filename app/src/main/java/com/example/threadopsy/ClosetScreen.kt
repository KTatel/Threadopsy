package com.example.threadopsy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private enum class ClosetFilter {
    ALL, PLASTIC_FREE, HIGH_RISK, GOAL_MET
}

@Composable
fun ClosetScreen(
    closetItems: List<ClothingItem>,
    onSelectItem: (ClothingItem) -> Unit,
    onLogWear: (ClothingItem) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf(ClosetFilter.ALL) }

    // --- Wardrobe Analytics Math ---
    val totalItems = closetItems.size
    val goalReached = closetItems.count { it.isCostSustainable }
    val avgShedScore = if (totalItems > 0) {
        closetItems.sumOf { it.shedDangerScore } / totalItems
    } else 0

    val wardrobeGrade = when {
        totalItems == 0 -> "---"
        avgShedScore == 0 -> "A"
        avgShedScore <= 30 -> "B"
        avgShedScore <= 65 -> "C"
        else -> "D"
    }

    val gradeColor = when (wardrobeGrade) {
        "A" -> Color(0xFF2E7D32)
        "B" -> Color(0xFF558B2F)
        "C" -> Color(0xFFEF6C00)
        "D" -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    // --- Filter Logic ---
    val filteredItems = remember(closetItems, selectedFilter) {
        when (selectedFilter) {
            ClosetFilter.ALL -> closetItems
            ClosetFilter.PLASTIC_FREE -> closetItems.filter { it.shedDangerScore == 0 }
            ClosetFilter.HIGH_RISK -> closetItems.filter { it.shedDangerScore > 30 }
            ClosetFilter.GOAL_MET -> closetItems.filter { it.isCostSustainable }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. BIG CENTERED HEADER ---
        Text(
            text = "My Cloud Closet",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Real-time Firebase sync & wardrobe analytics",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. WARDROBE ECO-GRADE BANNER ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = wardrobeGrade,
                        color = gradeColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Eco Grade",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                VerticalDivider(
                    modifier = Modifier.height(36.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                SummaryStat(label = "Saved Items", value = "$totalItems")
                SummaryStat(label = "30-Wears Met", value = "$goalReached")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 3. FILTER CHIPS ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == ClosetFilter.ALL,
                onClick = { selectedFilter = ClosetFilter.ALL },
                label = { Text("All ($totalItems)") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = selectedFilter == ClosetFilter.PLASTIC_FREE,
                onClick = { selectedFilter = ClosetFilter.PLASTIC_FREE },
                label = { Text("🌱 Plastic-Free") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = selectedFilter == ClosetFilter.HIGH_RISK,
                onClick = { selectedFilter = ClosetFilter.HIGH_RISK },
                label = { Text("🌊 High Risk") },
                shape = RoundedCornerShape(12.dp)
            )
            FilterChip(
                selected = selectedFilter == ClosetFilter.GOAL_MET,
                onClick = { selectedFilter = ClosetFilter.GOAL_MET },
                label = { Text("🏆 30-Wears Goal") },
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 4. CLOSET LIST OR EMPTY STATE ---
        if (closetItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Checkroom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Your closet is empty",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Scan a garment and save it from the Report tab.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (filteredItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No items match this filter filter.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    ClosetGarmentCard(
                        item = item,
                        onClick = { onSelectItem(item) },
                        onWearClick = { onLogWear(item) },
                        onDeleteClick = { onDeleteItem(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ClosetGarmentCard(
    item: ClothingItem,
    onClick: () -> Unit,
    onWearClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isUnclear = item.fabricBreakdown == "---" || item.landfillYears < 0
    val cpwFormatted = String.format(Locale.US, "%.0f", item.costPerWear)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isUnclear) "---" else item.garmentName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isUnclear) "---" else item.fabricBreakdown,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isUnclear) "---" else "₱$cpwFormatted per wear",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Worn ${item.timesWorn} of 30 times",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }

                FilledTonalButton(
                    onClick = onWearClick,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Wear +1", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}