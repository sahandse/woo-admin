package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenMoney
import com.example.ui.theme.RedError
import com.example.ui.theme.YellowWarn
import com.example.ui.viewmodel.WooViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    viewModel: WooViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToActivities: () -> Unit,
    onNavigateToCoupons: () -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    isDarkTheme: Boolean
) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val storesList by viewModel.stores.collectAsState(initial = emptyList())
    val activeStore = storesList.find { it.isActive }

    var showSyncDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile header
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = loggedInUser?.fullName ?: "مدیر فروشگاه",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val roleText = when (loggedInUser?.role) {
                                "SUPER_ADMIN" -> "مدیر اصلی"
                                "STORE_ADMIN" -> "مدیر فروشگاه"
                                "STOCK_CLERK" -> "انباردار"
                                else -> "کاربر"
                            }
                            Text(
                                text = roleText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            if (activeStore != null) {
                                Text(
                                    text = "فروشگاه فعال: ${activeStore.name}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Quick actions
            item {
                Text(
                    text = "ابزارهای سریع",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickActionCard(
                        icon = Icons.Default.Sync,
                        label = "همگام‌سازی",
                        tint = MaterialTheme.colorScheme.primary,
                        onClick = { showSyncDialog = true }
                    )
                    QuickActionCard(
                        icon = Icons.Default.ConfirmationNumber,
                        label = "کوپن‌ها",
                        tint = GreenMoney,
                        onClick = onNavigateToCoupons
                    )
                    QuickActionCard(
                        icon = Icons.Default.History,
                        label = "گزارشات",
                        tint = YellowWarn,
                        onClick = onNavigateToActivities
                    )
                }
            }

            // Settings section
            item {
                Text(
                    text = "تنظیمات",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        SettingsRow(
                            icon = Icons.Default.Settings,
                            label = "تنظیمات کلی",
                            onClick = onNavigateToSettings
                        )
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        SettingsRow(
                            icon = Icons.Default.DarkMode,
                            label = "حالت تاریک",
                            trailing = {
                                Switch(
                                    checked = isDarkTheme,
                                    onCheckedChange = onToggleDarkTheme
                                )
                            }
                        )
                    }
                }
            }

            // Danger zone
            item {
                Text(
                    text = "خطر",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedError,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsRow(
                        icon = Icons.Default.ExitToApp,
                        label = "خروج از حساب کاربری",
                        labelColor = RedError,
                        onClick = { viewModel.logout() }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showSyncDialog) {
        AlertDialog(
            onDismissRequest = { showSyncDialog = false },
            title = { Text("همگام‌سازی با سرور") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("همه داده‌های سفارشات، محصولات، مشتریان و کوپن‌ها با سرور ووکامرس به‌روز می‌شود.")

                }
            },
            confirmButton = {
                    Button(
                        onClick = {
                            showSyncDialog = false
                            scope.launch {
                                viewModel.repository.syncAllData()
                                Toast.makeText(context, "همگام‌سازی با موفقیت انجام شد", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("شروع همگام‌سازی")
                    }
            },
            dismissButton = {
                TextButton(onClick = { showSyncDialog = false }) {
                    Text("انصراف")
                }
            }
        )
    }
}

@Composable
fun QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    labelColor: Color = MaterialTheme.colorScheme.onSurface,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            trailing()
        } else if (onClick != null) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
