package com.example.threadopsy

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private data class LaundrySymbol(
    val iconText: String,
    val shortLabel: String,
    val fullRule: String
)

private val commonLaundrySymbols = listOf(
    LaundrySymbol(
        iconText = "🪣 30°",
        shortLabel = "Cold 30°C",
        fullRule = "🪣 Symbol [30° Tub]: Machine wash cold (max 30°C) to save energy and prevent shrinking."
    ),
    LaundrySymbol(
        iconText = "🤲",
        shortLabel = "Hand Wash",
        fullRule = "🤲 Symbol [Hand in Tub]: Hand wash gently in cool water; do not machine spin."
    ),
    LaundrySymbol(
        iconText = "△✕",
        shortLabel = "No Bleach",
        fullRule = "🚫 Symbol [Crossed Triangle]: Do not bleach; harsh chlorine weakens synthetic and natural threads."
    ),
    LaundrySymbol(
        iconText = "⬜✕",
        shortLabel = "No Dryer",
        fullRule = "🌬️ Symbol [Crossed Circle in Square]: Do not tumble dry; hang or lay flat to stop microfiber shedding."
    ),
    LaundrySymbol(
        iconText = "♨️•",
        shortLabel = "Low Iron",
        fullRule = "🌡️ Symbol [1-Dot Iron]: Iron on low heat (max 110°C) with a pressing cloth so plastic fibers don't melt."
    ),
    LaundrySymbol(
        iconText = "◯✕",
        shortLabel = "No Dry Clean",
        fullRule = "💧 Symbol [Crossed Circle]: Do not dry clean; avoid harsh chemical solvents."
    )
)

@Composable
fun ForensicAutopsyScreen(
    item: ClothingItem?,
    isGuest: Boolean,
    onUpdateDetails: (String, Double) -> Unit,
    onToggleSymbolTip: (String) -> Unit,
    onSaveToCloset: () -> Unit,
    onRequireSignIn: () -> Unit,
    onLoadSample: () -> Unit
) {
    if (item == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text("---", color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp)
        }
        return
    }

    val isUnclear = item.fabricBreakdown == "---" || item.landfillYears < 0

    var nameInput by remember(item.id) { mutableStateOf(item.garmentName) }
    var priceInput by remember(item.id) { mutableStateOf(item.purchasePrice.toInt().toString()) }

    // Both sections start collapsed whenever a new fabric is scanned
    var isCareExpanded by remember(item.id) { mutableStateOf(false) }
    var isCpwExpanded by remember(item.id) { mutableStateOf(false) }

    val statusColor = when {
        isUnclear -> MaterialTheme.colorScheme.outline
        item.shedDangerScore == 0 -> Color(0xFF2E7D32)
        item.shedDangerScore <= 30 -> Color(0xFFEF6C00)
        else -> Color(0xFFC62828)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. BIG CENTERED HEADER + "SAMPLE" BUTTON ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Fabric Report",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Material breakdown & environmental impact",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onLoadSample,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Sample", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // --- 2. MAIN FABRIC REPORT CARD (Always Visible) ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isUnclear) "---" else item.garmentName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isUnclear) "---" else item.armorClassTitle,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 14.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "DETECTED COMPOSITION",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isUnclear) "---" else item.fabricBreakdown,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isUnclear) "---" else "${item.landfillYears} yrs",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Time in Landfill",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isUnclear) "---" else item.ecoAlignment,
                            color = statusColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Eco Rating",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                SimpleStatBar(
                    label = "Microplastic Risk",
                    valueText = if (isUnclear) "---" else item.microplasticShedRate,
                    score = if (isUnclear) 0 else item.shedDangerScore,
                    barColor = statusColor
                )

                Spacer(modifier = Modifier.height(14.dp))

                SimpleStatBar(
                    label = "Comfort & Breathability",
                    valueText = if (isUnclear) "---" else "${item.breathabilityBuff}%",
                    score = if (isUnclear) 0 else item.breathabilityBuff,
                    barColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- 3. EXPANDABLE CARE GUIDE + LAUNDRY SYMBOL DECODER ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { isCareExpanded = !isCareExpanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "✨ Care Guide & Symbols",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCareExpanded) "Tap to hide tips" else "Tap to view washing tips & decode tag symbols",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isCareExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Care Guide",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = isCareExpanded) {
                    Column(modifier = Modifier.padding(top = 14.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        if (isUnclear) {
                            Text(
                                text = "---",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 15.sp,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            // Interactive Symbol Decoder Bar
                            Text(
                                text = "TAP SYMBOLS ON YOUR TAG TO DECODE:",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                commonLaundrySymbols.forEach { symbol ->
                                    val isSelected = item.washProtocol.contains(symbol.fullRule)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onToggleSymbolTip(symbol.fullRule) },
                                        label = {
                                            Text(
                                                text = "${symbol.iconText} ${symbol.shortLabel}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Care Tips List (Fabric rules + any tapped symbol rules)
                            item.washProtocol.forEach { tip ->
                                Text(
                                    text = tip,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- 4. EXPANDABLE COST PER WEAR CARD (Click to Open) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .clickable { isCpwExpanded = !isCpwExpanded },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🏷️ Cost Per Wear",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isCpwExpanded) "Tap to hide calculator" else "Tap to calculate 30-wear value",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = if (isCpwExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand Cost Per Wear",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = isCpwExpanded) {
                    Column(
                        modifier = Modifier.padding(top = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = {
                                    nameInput = it
                                    onUpdateDetails(it, priceInput.toDoubleOrNull() ?: item.purchasePrice)
                                },
                                label = { Text("Item Name", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = priceInput,
                                onValueChange = {
                                    priceInput = it
                                    val parsed = it.toDoubleOrNull() ?: 0.0
                                    onUpdateDetails(nameInput, parsed)
                                },
                                label = { Text("Price (₱)", fontSize = 12.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.9f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val formattedCpw = String.format(Locale.US, "%.0f", item.costPerWear)
                        val target30Cpw = String.format(Locale.US, "%.0f", item.purchasePrice / 30.0)

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isUnclear) "---" else "₱$formattedCpw",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "Current (${item.timesWorn}x worn)",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isUnclear) "---" else "₱$target30Cpw",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "30-Wear Goal",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 5. SAVE BUTTON ---
        if (isGuest) {
            OutlinedButton(
                onClick = onRequireSignIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Log In to Save to Closet",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        } else {
            Button(
                onClick = onSaveToCloset,
                enabled = !isUnclear,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isUnclear) "Scan a Clear % Tag to Save" else "Save to Closet",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun SimpleStatBar(
    label: String,
    valueText: String,
    score: Int,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = valueText,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (score.coerceIn(0, 100) / 100f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}