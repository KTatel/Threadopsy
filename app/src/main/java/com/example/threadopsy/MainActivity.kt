package com.example.threadopsy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F6E50),
    onPrimary = Color.White,
    secondaryContainer = Color(0xFFD8EFE4),
    onSecondaryContainer = Color(0xFF0E3B29),
    background = Color(0xFFF7F9F8),
    onBackground = Color(0xFF191C1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1B),
    surfaceVariant = Color(0xFFEEF2F0),
    onSurfaceVariant = Color(0xFF5C6662),
    outline = Color(0xFF8C9692),
    outlineVariant = Color(0xFFE0E6E3)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5DD4A3),
    onPrimary = Color(0xFF003824),
    secondaryContainer = Color(0xFF1E3A2F),
    onSecondaryContainer = Color(0xFFB9F0D6),
    background = Color(0xFF111413),
    onBackground = Color(0xFFE2E6E4),
    surface = Color(0xFF1A1E1D),
    onSurface = Color(0xFFE2E6E4),
    surfaceVariant = Color(0xFF242A28),
    onSurfaceVariant = Color(0xFF9BA5A1),
    outline = Color(0xFF65706C),
    outlineVariant = Color(0xFF2B3230)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemDark) }

            MaterialTheme(
                colorScheme = if (isDarkMode) DarkColors else LightColors
            ) {
                ThreadopsyMainApp(
                    isDarkMode = isDarkMode,
                    onToggleTheme = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@Composable
fun ThreadopsyMainApp(
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: ThreadopsyViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSdgDialog by remember { mutableStateOf(false) }

    val authState by viewModel.authState.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val currentItem by viewModel.currentAutopsyItem.collectAsState()
    val closetItems by viewModel.closetItems.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val isGuest = authState is AuthState.Guest

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 12.dp)
                ) {
                    // Left side: Logout or Info button
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (authState !is AuthState.Unauthenticated) {
                            TextButton(
                                onClick = {
                                    selectedTab = 0
                                    viewModel.signOutOrExitGuest()
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = if (isGuest) "Log In" else "Log Out",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        IconButton(
                            onClick = { showSdgDialog = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "SDG 12 & 14 Info",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Center: Brand Title
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Threadopsy",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isGuest) {
                            Text(
                                text = "Guest Mode",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Right side: Light / Dark Mode Toggle
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (authState !is AuthState.Unauthenticated) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Scan") },
                        label = { Text("Scan", fontSize = 12.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab == 1 },
                        icon = { Icon(Icons.Default.Description, contentDescription = "Report") },
                        label = { Text("Report", fontSize = 12.sp) }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = {
                            if (isGuest) {
                                viewModel.continueAsGuest()
                            } else {
                                selectedTab = 2
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isGuest) Icons.Default.Lock else Icons.Default.Checkroom,
                                contentDescription = "Closet"
                            )
                        },
                        label = {
                            Text(
                                text = if (isGuest) "Closet (Locked)" else "Closet (${closetItems.size})",
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (authState is AuthState.Unauthenticated) {
                AuthScreen(
                    isLoading = isAuthLoading,
                    onSignIn = { email, pass -> viewModel.signIn(email, pass) },
                    onSignUp = { email, pass -> viewModel.signUp(email, pass) },
                    onContinueAsGuest = {
                        selectedTab = 0
                        viewModel.continueAsGuest()
                    }
                )
            } else {
                when (selectedTab) {
                    0 -> TagScannerScreen(
                        onTextCaptured = { scannedText ->
                            viewModel.onTagTextRecognized(scannedText)
                            selectedTab = 1
                        },
                        onSimulateSample = {
                            viewModel.loadNextSampleTag()
                            selectedTab = 1
                        }
                    )

                    1 -> ForensicAutopsyScreen(
                        item = currentItem,
                        isGuest = isGuest,
                        onUpdateDetails = { newName, newPrice ->
                            viewModel.updateCurrentItemDetails(newName, newPrice)
                        },
                        onToggleSymbolTip = { symbolRule ->
                            viewModel.toggleSymbolTip(symbolRule)
                        },
                        onSaveToCloset = {
                            viewModel.saveCurrentToCloset()
                        },
                        onRequireSignIn = {
                            viewModel.signOutOrExitGuest()
                        },
                        onLoadSample = {
                            viewModel.loadNextSampleTag()
                        }
                    )

                    2 -> ClosetScreen(
                        closetItems = closetItems,
                        onSelectItem = { chosenItem ->
                            viewModel.selectClosetItemForInspection(chosenItem)
                            selectedTab = 1
                        },
                        onLogWear = { itemToIncrement ->
                            viewModel.logWear(itemToIncrement)
                        },
                        onDeleteItem = { idToDelete ->
                            viewModel.deleteFromCloset(idToDelete)
                        }
                    )
                }
            }
        }

        // --- SDG 12 & 14 PITCH INFO DIALOG ---
        if (showSdgDialog) {
            AlertDialog(
                onDismissRequest = { showSdgDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                title = {
                    Text(
                        text = "🌱 UN Sustainability Goals",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "• SDG 12 (Responsible Consumption):\nFast fashion clothes are discarded after only 7–10 wears. Threadopsy's 30-Wear tracker helps reduce fashion waste and resource depletion.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Text(
                            text = "• SDG 14 (Life Below Water):\nUp to 35% of primary microplastics in ocean waters come from synthetic garments shedding in washing machines. Our care guide highlights microplastic filters and cold wash protocols.",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showSdgDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Got it", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    }
}