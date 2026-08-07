package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.RedError
import com.example.ui.theme.YellowWarn
import com.example.ui.viewmodel.WooViewModel

// Sealed representations of routes
sealed class Screen {
    object Dashboard : Screen()
    object Orders : Screen()
    object Products : Screen()
    object Customers : Screen()
    object More : Screen() // Settings + extras
    data class OrderDetails(val orderId: Long) : Screen()
    data class AddEditProduct(val productId: Long?) : Screen()
    data class CustomerDetails(val customerId: Long) : Screen()
    object Activities : Screen()
    object Coupons : Screen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer(
    viewModel: WooViewModel,
    onLogout: () -> Unit,
    onToggleDarkTheme: (Boolean) -> Unit,
    isDarkTheme: Boolean
) {
    var activeScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }
    var showNotifSheet by remember { mutableStateOf(false) }

    val notificationsList by viewModel.notifications.collectAsState(initial = emptyList())
    val unreadNotifs = notificationsList.count { !it.isRead }

    val context = LocalContext.current

    // Observe login state for logs out
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            onLogout()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                // Top App bar only on primary tabs, secondary views draw their own return top-bars
                val isTab = activeScreen is Screen.Dashboard ||
                            activeScreen is Screen.Orders ||
                            activeScreen is Screen.Products ||
                            activeScreen is Screen.Customers ||
                            activeScreen is Screen.AiAnalyst ||
                            activeScreen is Screen.Settings

                if (isTab) {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "مدیریت ووکامرس",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        actions = {
                            // Notification bell with unread badge count
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable { showNotifSheet = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "اعلان‌ها",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                if (unreadNotifs > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = (-4).dp, y = 4.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(RedError),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = unreadNotifs.toString(),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
            },
            bottomBar = {
                // Persistent bottom navigator ONLY shown on primary tabs
                val isTab = activeScreen is Screen.Dashboard ||
                            activeScreen is Screen.Orders ||
                            activeScreen is Screen.Products ||
                            activeScreen is Screen.Customers ||
                            activeScreen is Screen.More

                if (isTab) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        NavigationBarItem(
                            selected = activeScreen is Screen.Dashboard,
                            onClick = { activeScreen = Screen.Dashboard },
                            icon = { Icon(imageVector = Icons.Default.SpaceDashboard, contentDescription = "داشبورد") },
                            label = { Text("داشبورد", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )

                        NavigationBarItem(
                            selected = activeScreen is Screen.Orders,
                            onClick = { activeScreen = Screen.Orders },
                            icon = { Icon(imageVector = Icons.Default.ReceiptLong, contentDescription = "سفارش‌ها") },
                            label = { Text("سفارش‌ها", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )

                        NavigationBarItem(
                            selected = activeScreen is Screen.Products,
                            onClick = { activeScreen = Screen.Products },
                            icon = { Icon(imageVector = Icons.Default.LocalMall, contentDescription = "کالاها") },
                            label = { Text("محصولات", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )

                        NavigationBarItem(
                            selected = activeScreen is Screen.Customers,
                            onClick = { activeScreen = Screen.Customers },
                            icon = { Icon(imageVector = Icons.Default.People, contentDescription = "مشتریان") },
                            label = { Text("مشتریان", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )

                        NavigationBarItem(
                            selected = activeScreen is Screen.More,
                            onClick = { activeScreen = Screen.More },
                            icon = { Icon(imageVector = Icons.Default.MoreVert, contentDescription = "بیشتر") },
                            label = { Text("بیشتر", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Render view based on state
                when (val current = activeScreen) {
                    is Screen.Dashboard -> {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateToOrdersWithFilter = { filter ->
                                viewModel.setOrderStatusFilter(filter)
                                activeScreen = Screen.Orders
                            },
                            onNavigateToProductsWithFilter = { filter ->
                                viewModel.setProductFilter(filter)
                                activeScreen = Screen.Products
                            }
                        )
                    }

                    is Screen.Orders -> {
                        OrdersScreen(
                            viewModel = viewModel,
                            onNavigateToDetails = { orderId ->
                                activeScreen = Screen.OrderDetails(orderId)
                            }
                        )
                    }

                    is Screen.OrderDetails -> {
                        OrderDetailsScreen(
                            orderId = current.orderId,
                            viewModel = viewModel,
                            onBack = { activeScreen = Screen.Orders }
                        )
                    }

                    is Screen.Products -> {
                        ProductsScreen(
                            viewModel = viewModel,
                            onNavigateToAdd = { activeScreen = Screen.AddEditProduct(null) },
                            onNavigateToEdit = { pId -> activeScreen = Screen.AddEditProduct(pId) }
                        )
                    }

                    is Screen.AddEditProduct -> {
                        AddEditProductScreen(
                            productId = current.productId,
                            viewModel = viewModel,
                            onBack = { activeScreen = Screen.Products }
                        )
                    }

                    is Screen.Customers -> {
                        CustomersScreen(
                            viewModel = viewModel,
                            onNavigateToDetails = { cId -> activeScreen = Screen.CustomerDetails(cId) }
                        )
                    }

                    is Screen.CustomerDetails -> {
                        CustomerDetailsScreen(
                            customerId = current.customerId,
                            viewModel = viewModel,
                            onBack = { activeScreen = Screen.Customers }
                        )
                    }

                    is Screen.More -> {
                        MoreScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = { activeScreen = Screen.Settings },
                            onNavigateToActivities = { activeScreen = Screen.Activities },
                            onNavigateToCoupons = { activeScreen = Screen.Coupons },
                            onToggleDarkTheme = onToggleDarkTheme,
                            isDarkTheme = isDarkTheme
                        )
                    }

                    is Screen.Settings -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { activeScreen = Screen.More },
                            onNavigateToActivities = { activeScreen = Screen.Activities },
                            onNavigateToCoupons = { activeScreen = Screen.Coupons },
                            onToggleDarkTheme = onToggleDarkTheme,
                            isDarkTheme = isDarkTheme
                        )
                    }

                    is Screen.Activities -> {
                        AdminActivitiesScreen(viewModel = viewModel, onBack = { activeScreen = Screen.Settings })
                    }

                    is Screen.Coupons -> {
                        CouponsScreen(viewModel = viewModel, onBack = { activeScreen = Screen.Settings })
                    }
                }
            }
        }

        // Realtime Notifications Bottom Sheet list
        if (showNotifSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNotifSheet = false },
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "صندوق اعلانات فروشگاه (سفارش و انبار)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        
                        if (notificationsList.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    notificationsList.forEach { viewModel.readNotification(it.id) }
                                    Toast.makeText(context, "تمام موارد خوانده شد", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("علامت کاربری به خوانده شده")
                            }
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    if (notificationsList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("هیچ اعلانی درباره ووکامرس وجود ندارد.", color = Color.Gray)
                        }
                    } else {
                        Box(modifier = Modifier.heightIn(max = 400.dp)) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(notificationsList, key = { it.id }) { nt ->
                                    val notifColor = when (nt.type) {
                                        "LOW_STOCK" -> YellowWarn
                                        "NEW_CUSTOMER" -> MaterialTheme.colorScheme.primary
                                        else -> RedError
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (nt.isRead) Color.Transparent else notifColor.copy(alpha = 0.05f))
                                            .border(1.dp, if (nt.isRead) Color.LightGray.copy(alpha = 0.3f) else notifColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                            .clickable { viewModel.readNotification(nt.id) }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(if (nt.isRead) Color.LightGray else notifColor)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = nt.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (nt.isRead) Color.Gray else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(text = nt.body, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
                                            Text(text = nt.dateJalali, fontSize = 9.sp, color = Color.LightGray, modifier = Modifier.padding(top = 2.dp))
                                        }

                                        IconButton(onClick = { viewModel.removeNotification(nt.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
