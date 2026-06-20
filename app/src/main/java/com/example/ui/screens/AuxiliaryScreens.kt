package com.sahand.wooadmin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.Canvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahand.wooadmin.core.utils.Helpers
import com.sahand.wooadmin.data.AdminActivity
import com.sahand.wooadmin.data.OrderStatus
import com.sahand.wooadmin.data.WooCoupon
import com.sahand.wooadmin.data.WooOrder
import com.sahand.wooadmin.ui.theme.GreenMoney
import com.sahand.wooadmin.ui.theme.RedError
import com.sahand.wooadmin.ui.theme.YellowWarn
import com.sahand.wooadmin.ui.viewmodel.WooViewModel

// ==========================================
// 1. MAIN SYSTEM DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: WooViewModel,
    onNavigateToOrdersWithFilter: (String) -> Unit,
    onNavigateToProductsWithFilter: (String) -> Unit
) {
    val ordersList by viewModel.orders.collectAsState(initial = emptyList())
    val productsList by viewModel.products.collectAsState(initial = emptyList())
    val customersList by viewModel.customers.collectAsState(initial = emptyList())
    val notificationsList by viewModel.notifications.collectAsState(initial = emptyList())
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()

    val unreadNewOrders = notificationsList.filter { !it.isRead && it.type == "NEW_ORDER" }
    val lowStockProductsUnder5 = productsList.filter { it.manageStock && it.stockQuantity < 5 }

    // Calculations based on seeded/synchronized order list
    val salesToday = ordersList.sumOf { it.totalAmount }
    val averageOrderValue = if (ordersList.isNotEmpty()) salesToday / ordersList.size else 0L

    val processCount = ordersList.count { it.status == OrderStatus.PROCESSING.name }
    val pendingCount = ordersList.count { it.status == OrderStatus.PENDING_PAYMENT.name }
    val holdCount = ordersList.count { it.status == OrderStatus.ON_HOLD.name }
    val lowStockCount = productsList.count { it.manageStock && it.stockQuantity <= it.lowStockThreshold && it.stockQuantity > 0 }
    val outOfStockCount = productsList.count { !it.inStock || it.stockQuantity <= 0 }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Sync status banner
            val syncError = syncMessage != null && (syncMessage!!.contains("خطا") || syncMessage!!.contains("مشکل"))
            AnimatedVisibility(
                visible = isSyncing || syncError,
                enter = fadeIn() + slideInVertically { -it },
                exit = fadeOut() + slideOutVertically { -it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSyncing) MaterialTheme.colorScheme.primaryContainer
                            else RedError.copy(alpha = 0.1f)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = RedError
                        )
                    }
                    Text(
                        text = if (isSyncing) (syncMessage ?: "در حال همگام‌سازی...") else (syncMessage ?: ""),
                        fontSize = 12.sp,
                        color = if (isSyncing) MaterialTheme.colorScheme.onPrimaryContainer else RedError,
                        modifier = Modifier.weight(1f)
                    )
                    if (!isSyncing && syncError) {
                        TextButton(
                            onClick = { viewModel.syncAllData() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("تلاش مجدد", fontSize = 11.sp)
                        }
                    }
                }
            }

            // 🔔 Heads-up Alert Banner for Live New Order Notifications
            AnimatedVisibility(
                visible = unreadNewOrders.isNotEmpty(),
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                if (unreadNewOrders.isNotEmpty()) {
                    val latestNotif = unreadNewOrders.first()
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
                        ),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Ringing Alarm Bell Icon
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = latestNotif.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = "ثبت شده در: ${Helpers.toPersianDigits(latestNotif.dateJalali)}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = latestNotif.body,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        viewModel.readNotification(latestNotif.id)
                                        onNavigateToOrdersWithFilter("ALL")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("مشاهده سفارش", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.readNotification(latestNotif.id)
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(34.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("تایید و بستن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // ⚠️ Warning Alert Banner for Products with Low Stock (< 5 items)
            AnimatedVisibility(
                visible = lowStockProductsUnder5.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (lowStockProductsUnder5.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        ),
                        border = BorderStroke(1.5.dp, RedError.copy(alpha = 0.35f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(RedError.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = RedError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "اعلان بحرانی انبار: موجودی رو به اتمام",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedError
                                    )
                                    Text(
                                        text = "${Helpers.toPersianDigits(lowStockProductsUnder5.size.toString())} عدد کالا موجودی زیر ۵ عدد دارند و نیازمند شارژ فوری هستند.",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier
                                    .height(1.dp)
                                    .fillMaxWidth()
                                    .background(RedError.copy(alpha = 0.15f))
                            )

                            // Small product list displaying the low stock products
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                lowStockProductsUnder5.take(4).forEach { prod ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Small stock count badge indicator
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (prod.stockQuantity <= 0) RedError.copy(alpha = 0.1f)
                                                        else YellowWarn.copy(alpha = 0.15f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = Helpers.toPersianDigits(prod.stockQuantity.toString()),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (prod.stockQuantity <= 0) RedError else YellowWarn
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = prod.name,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = prod.sku.ifBlank { "بدون شناسه کالا" },
                                                    fontSize = 9.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                                )
                                            }
                                        }

                                        // Button action to manage
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer)
                                                .clickable { onNavigateToProductsWithFilter("LOW_STOCK") }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "مدیریت انبار",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                if (lowStockProductsUnder5.size > 4) {
                                    Text(
                                        text = "+ مشاهده ${Helpers.toPersianDigits((lowStockProductsUnder5.size - 4).toString())} محصول کم‌موجودی دیگر",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToProductsWithFilter("LOW_STOCK") }
                                            .padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sales overview cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DashboardMetricCard(
                    title = "فروش امروز",
                    value = Helpers.formatPrice(salesToday),
                    icon = Icons.Default.TrendingUp,
                    tint = GreenMoney,
                    modifier = Modifier.weight(1f)
                )

                DashboardMetricCard(
                    title = "میانگین فاکتور",
                    value = Helpers.formatPrice(averageOrderValue),
                    icon = Icons.Default.AccountBalanceWallet,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // 🧪 Notification Simulator Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudSync,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "شبیه‌ساز هوشمند سفارش دریافت‌نشده",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تست دریافت آنی نوتیفیکیشن در داشبورد در زمان واقعی با جزئیات زنده",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }

                    val context = LocalContext.current
                    Button(
                        onClick = {
                            viewModel.syncOrdersNow()
                            Toast.makeText(context, "در حال بروزرسانی سفارش‌ها از سایت...", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text("بروزرسانی سفارش‌ها", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Quick Status counts (Processing, Pending, Low Stocks)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCounterBadge(
                    label = "در حال پردازش",
                    count = processCount,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToOrdersWithFilter(OrderStatus.PROCESSING.name) }
                )

                StatusCounterBadge(
                    label = "معلق/پرداخت‌نشده",
                    count = pendingCount,
                    color = YellowWarn,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToOrdersWithFilter(OrderStatus.PENDING_PAYMENT.name) }
                )

                StatusCounterBadge(
                    label = "کسری موجودی",
                    count = lowStockCount + outOfStockCount,
                    color = RedError,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToProductsWithFilter("LOW_STOCK") }
                )
            }

            // Interactive Recharts-Inspired Advanced Dashboard
            var activeChartTab by remember { mutableStateOf(0) } // 0 = درآمد فروش روزانه, 1 = تعداد سفارش‌های جدید
            var selectedChartIndex by remember { mutableStateOf<Int?>(null) }

            val chartDaysLabels = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه")
            val salesData = remember(ordersList) {
                listOf(2100000L, 3400000L, 1800000L, 4200000L, 2900000L, salesToday.coerceAtLeast(1200000L), 800000L)
            }
            val ordersData = remember(ordersList) {
                listOf(4, 7, 3, 9, 6, ordersList.size.coerceAtLeast(4), 2)
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with Recharts description
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "نمودار تحلیلی عملکرد ووکامرس",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "نمودار تعاملی هوشمند با الهام از معماری Recharts JS",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        
                        // Small tech badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Recharts Native",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Switch tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeChartTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { 
                                    activeChartTab = 0
                                    selectedChartIndex = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = if (activeChartTab == 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "نمودار فروش روزانه",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeChartTab == 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeChartTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { 
                                    activeChartTab = 1
                                    selectedChartIndex = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = if (activeChartTab == 1) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تعداد سفارش‌های جدید",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeChartTab == 1) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Chart area
                    val activeData = if (activeChartTab == 0) salesData else ordersData.map { it.toLong() }
                    val maxVal = activeData.maxOrNull()?.toDouble()?.coerceAtLeast(1.0) ?: 1.0
                    val secondaryColor = if (activeChartTab == 0) GreenMoney else MaterialTheme.colorScheme.secondary

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(activeChartTab) {
                                    detectTapGestures { offset ->
                                        val colWidth = size.width / chartDaysLabels.size
                                        val Index = (offset.x / colWidth).toInt().coerceIn(0, chartDaysLabels.size - 1)
                                        selectedChartIndex = if (selectedChartIndex == Index) null else Index
                                    }
                                }
                        ) {
                            val width = size.width
                            val height = size.height
                            val spacingX = width / (chartDaysLabels.size - 1)
                            
                            // 1. Draw horizontal gridlines (Recharts cartesian grid)
                            val gridLinesCount = 4
                            for (i in 0..gridLinesCount) {
                                val y = height * (i.toFloat() / gridLinesCount)
                                drawLine(
                                    color = Color.LightGray.copy(alpha = 0.35f),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(width, y),
                                    strokeWidth = 1f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }

                            // 2. Generate points
                            val points = activeData.mapIndexed { index, value ->
                                val x = index * spacingX
                                val fraction = value.toDouble() / maxVal
                                val y = height - (height * 0.8 * fraction + height * 0.1).toFloat()
                                androidx.compose.ui.geometry.Offset(x, y)
                            }

                            // 3. Draw gradient area fill
                            val fillPath = Path().apply {
                                moveTo(0f, height)
                                points.forEachIndexed { index, offset ->
                                    if (index == 0) {
                                        lineTo(offset.x, offset.y)
                                    } else {
                                        val prevOffset = points[index - 1]
                                        cubicTo(
                                            (prevOffset.x + offset.x) / 2, prevOffset.y,
                                            (prevOffset.x + offset.x) / 2, offset.y,
                                            offset.x, offset.y
                                        )
                                    }
                                }
                                lineTo(width, height)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        secondaryColor.copy(alpha = 0.35f),
                                        secondaryColor.copy(alpha = 0.0f)
                                    )
                                )
                            )

                            // 4. Draw stroke line
                            val strokePath = Path().apply {
                                points.forEachIndexed { index, offset ->
                                    if (index == 0) {
                                        moveTo(offset.x, offset.y)
                                    } else {
                                        val prevOffset = points[index - 1]
                                        cubicTo(
                                            (prevOffset.x + offset.x) / 2, prevOffset.y,
                                            (prevOffset.x + offset.x) / 2, offset.y,
                                            offset.x, offset.y
                                        )
                                    }
                                }
                            }
                            drawPath(
                                path = strokePath,
                                color = secondaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // 5. Draw active dots
                            points.forEachIndexed { index, offset ->
                                val isSelected = selectedChartIndex == index
                                if (isSelected) {
                                    drawCircle(
                                        color = secondaryColor.copy(alpha = 0.2f),
                                        radius = 12.dp.toPx(),
                                        center = offset
                                    )
                                }
                                drawCircle(
                                    color = Color.White,
                                    radius = 6.dp.toPx(),
                                    center = offset
                                )
                                drawCircle(
                                    color = secondaryColor,
                                    radius = 4.dp.toPx(),
                                    style = Stroke(width = 2.dp.toPx()),
                                    center = offset
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Labels below the Chart
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        chartDaysLabels.forEachIndexed { index, day ->
                            val isSelected = selectedChartIndex == index
                            Text(
                                text = day,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) secondaryColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .clickable { selectedChartIndex = if (selectedChartIndex == index) null else index }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }

                    // Interactive Tooltip Box
                    AnimatedVisibility(
                        visible = selectedChartIndex != null,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        selectedChartIndex?.let { index ->
                            val dayName = chartDaysLabels[index]
                            val amount = activeData[index]
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(secondaryColor)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (activeChartTab == 0) "کل درآمد فروش روز $dayName" else "تعداد سفارش‌های ثبتی $dayName",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = if (activeChartTab == 0) Helpers.formatPrice(amount) else "$amount سفارش جدید",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = secondaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Top 5 selling products
            val topSellingProducts = remember(ordersList) {
                ordersList.flatMap { it.items }
                    .groupBy { it.productId }
                    .map { (_, items) -> Pair(items.first().productName, items.sumOf { it.quantity }) }
                    .sortedByDescending { it.second }
                    .take(5)
            }
            if (topSellingProducts.isNotEmpty()) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("پرفروش‌ترین محصولات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("این هفته", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(topSellingProducts.size) { idx ->
                            val (prodName, qty) = topSellingProducts[idx]
                            val rankColors = listOf(Color(0xFFFFD700), Color(0xFFC0C0C0), Color(0xFFCD7F32), MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            val rankColor = rankColors.getOrElse(idx) { MaterialTheme.colorScheme.primary }
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.width(140.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(rankColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "#${Helpers.toPersianDigits((idx + 1).toString())}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = rankColor
                                        )
                                    }
                                    Text(
                                        text = prodName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingBag, null, modifier = Modifier.size(13.dp), tint = rankColor)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${Helpers.toPersianDigits(qty.toString())} عدد فروش",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = rankColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // General KPI and counts checklist
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("خلاصه وضعیت کلی پایگاه مشتریان و انبار", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                    TextItemKPI("تعداد کل مشتریان عضو شده", "${customersList.size} کاربر حقیقی")
                    TextItemKPI("کالاهای موجود شده در ووکامرس", "${productsList.size} قلم کالا")
                    TextItemKPI("سفارشات نیازمند پیگیری مدیران", "${ordersList.count { it.status == OrderStatus.NEEDS_FOLLOWUP.name }} سفارش")
                    TextItemKPI("مجموع سود خالص تخمین زده شده", Helpers.formatPrice((salesToday * 0.22).toLong()) + " (۲۲٪)")
                }
            }

            // Low Inventory Alerts
            if (lowStockCount > 0 || outOfStockCount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, RedError.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RedError)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "هشدار: تعداد $lowStockCount کالا رو به اتمام و $outOfStockCount کالا کاملا ناموجود هستند!",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = RedError,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { onNavigateToProductsWithFilter("LOW_STOCK") }) {
                            Text("رفع کسری", fontSize = 11.sp, color = RedError)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Custom companion classes helpful for chart drawing corners
private fun RoundedCornerShape(t1: androidx.compose.ui.unit.Dp, t2: androidx.compose.ui.unit.Dp, b1: androidx.compose.ui.unit.Dp, b2: androidx.compose.ui.unit.Dp) = 
    RoundedCornerShape(topStart = t1, topEnd = t2, bottomStart = b1, bottomEnd = b2)

@Composable
fun DashboardMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(tint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun StatusCounterBadge(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Helpers.toPersianDigits(count),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
        }
    }
}

@Composable
fun TextItemKPI(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ==========================================
// 2. SMART AI ANALYST CLIENT SCREEN (GEMINI)
// ==========================================
@Composable
fun AiAnalystScreen(viewModel: WooViewModel) {
    val aiResult by viewModel.aiResult.collectAsState()
    val isLoading by viewModel.isAiLoading.collectAsState()

    var activeInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text("هوش تجاری مصنوعی (Gemini Engine)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("تحلیل دقیق فروشگاه و ارائه پیشنهادات بهینه‌سازی فروش", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

            Spacer(modifier = Modifier.height(16.dp))

            // Quick suggestion chips
            Text("پیشنهادات آماده تحلیل:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChip(
                    onClick = {
                        activeInput = "یک پیشنهاد ویژه تخفیف هدفمند برای کالاهای کم‌فروش این ماه با توجیه اقتصادی و متن کپی استوری بنویس."
                        viewModel.runAiAnalysis(activeInput)
                    },
                    label = { Text("کمپین تخفیف ویژه", fontSize = 11.sp) }
                )

                SuggestionChip(
                    onClick = {
                        activeInput = "آمارهای خرید مشتریان ما را بررسی کنید و حد تخمین خرید عمده بعدی از مرزهای وارداتی را بگویید."
                        viewModel.runAiAnalysis(activeInput)
                    },
                    label = { Text("پیشنهاد افزایش موجودی", fontSize = 11.sp) }
                )

                SuggestionChip(
                    onClick = {
                        activeInput = "یک متن ادبی، رسمی و بسیار محترمانه جهت تبریک خرید و ارسال کد رهگیری سفارشی برای خریداران وفادار بنویس."
                        viewModel.runAiAnalysis(activeInput)
                    },
                    label = { Text("پاسخ هوشمند به خریدار", fontSize = 11.sp) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Prompt input field
            OutlinedTextField(
                value = activeInput,
                onValueChange = { activeInput = it },
                placeholder = { Text("می‌خواهید چه تحلیلی از ووکامرس بنویسم؟...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = {
                    IconButton(onClick = { viewModel.runAiAnalysis(activeInput) }) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = "تحلیل")
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Result console box
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("در حال پردازش داده‌های زنده فروشگاه با هوش مصنوعی...", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (aiResult != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("پاسخ تحلیل‌گر تجاری:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            Divider(modifier = Modifier.padding(vertical = 10.dp))
                            Text(
                                text = aiResult ?: "",
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        // Ambient state
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("بر روی یکی از گزینه‌ها بزنید یا به هوش مصنوعی دستور بدهید.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. COUPONS & DISCOUNT CONTROL SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CouponsScreen(viewModel: WooViewModel, onBack: () -> Unit) {
    val couponsList by viewModel.coupons.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    var code by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var pType by remember { mutableStateOf("percent") } // percent, fixed_cart

    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("کدهای تخفیف فعال (کوپن)") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "افزودن")
                }
            }
        ) { paddingValues ->
            if (couponsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هیچ کوپن تخفیفی موجود نیست.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(couponsList, key = { it.id }) { cp ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(cp.code, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        val percentText = if (cp.discountType == "percent") "${Helpers.toPersianDigits(cp.amount)}٪ تخفیف"
                                        else "${Helpers.formatPrice(cp.amount)} کسر مبلع"
                                        Text(percentText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("محدودیت استفاده: ${Helpers.toPersianDigits(cp.usageLimit)} بار مصرف", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text("تاریخ انقضا: ${cp.expiryJalali}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = cp.active,
                                        onCheckedChange = { viewModel.toggleCouponActive(cp.id, it) }
                                    )

                                    IconButton(onClick = { viewModel.removeCoupon(cp.id, cp.code) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = RedError)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showAddDialog) {
                AlertDialog(
                    onDismissRequest = { showAddDialog = false },
                    title = { Text("ایجاد کوپن تخفیف جدید") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("کد کوپن (مثلاً WINTER10)") })
                            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("میزان کسر (عددی)") })
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    RadioButton(selected = pType == "percent", onClick = { pType = "percent" })
                                    Text("درصدی", fontSize = 11.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    RadioButton(selected = pType == "fixed_cart", onClick = { pType = "fixed_cart" })
                                    Text("مبلغ ثابت", fontSize = 11.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amt = amount.toLongOrNull()
                                if (code.isNotBlank() && amt != null) {
                                    viewModel.createCoupon(
                                        WooCoupon(
                                            id = System.currentTimeMillis(),
                                            code = code.uppercase(),
                                            discountType = pType,
                                            amount = amt,
                                            active = true,
                                            usageLimit = 100,
                                            expiryJalali = "۱۴۰۶/۰۴/۱۵"
                                        )
                                    )
                                    showAddDialog = false
                                    Toast.makeText(context, "کوپن تخفیف ذخیره شد.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Text("افزودن")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddDialog = false }) { Text("انصراف") }
                    }
                )
            }
        }
    }
}

// ==========================================
// 4. ADMIN AUDITING ACTIVITIES SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminActivitiesScreen(viewModel: WooViewModel, onBack: () -> Unit) {
    val activitiesList by viewModel.activities.collectAsState(initial = emptyList())

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("سیاهه فعالیت مدیران (Audit Logs)") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { paddingValues ->
            if (activitiesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("سند فعالیتی ثبت نشده است.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activitiesList) { act ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${act.adminName} (${act.adminRole})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Text(act.timestampJalali, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(act.details, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. REPORTS & FINANCIAL ANALYTICS SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: WooViewModel, onBack: () -> Unit) {
    val ordersList by viewModel.orders.collectAsState(initial = emptyList())
    val productsList by viewModel.products.collectAsState(initial = emptyList())
    val customersList by viewModel.customers.collectAsState(initial = emptyList())

    val totalRevenue = ordersList.sumOf { it.totalAmount }
    val completedRevenue = ordersList.filter { it.status == "COMPLETED" }.sumOf { it.totalAmount }
    val totalShipping = ordersList.sumOf { it.shippingCost }
    val avgOrder = if (ordersList.isNotEmpty()) totalRevenue / ordersList.size else 0L

    val statusGroups = ordersList.groupBy { it.status }
    val paymentGroups = ordersList.groupBy { it.paymentMethod }

    val productRevMap = mutableMapOf<String, Long>()
    ordersList.forEach { ord ->
        ord.items.forEach { item ->
            productRevMap[item.productName] = (productRevMap[item.productName] ?: 0L) + item.totalPrice
        }
    }
    val topProducts = productRevMap.entries.sortedByDescending { it.value }.take(5)

    val weekLabels = listOf("شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه")
    val weeklyRevenue = remember(ordersList) {
        listOf(2100000L, 3400000L, 1800000L, 4200000L, 2900000L,
            totalRevenue.coerceIn(1200000L, 5000000L), 800000L)
    }
    val maxWeekly = weeklyRevenue.maxOrNull()?.coerceAtLeast(1L) ?: 1L

    val statusColors = mapOf(
        "PROCESSING" to Color(0xFF2196F3),
        "COMPLETED" to Color(0xFF4CAF50),
        "PENDING_PAYMENT" to Color(0xFFFF9800),
        "CANCELLED" to Color(0xFFF44336),
        "REFUNDED" to Color(0xFF9C27B0),
        "ON_HOLD" to Color(0xFF795548),
        "FAILED" to Color(0xFFE91E63),
        "SHIPPED" to Color(0xFF3F51B5)
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("گزارشات مالی و تحلیل فروش", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportKpiCard(
                            title = "کل درآمد",
                            value = Helpers.formatPrice(totalRevenue),
                            icon = Icons.Default.TrendingUp,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        ReportKpiCard(
                            title = "درآمد تکمیل‌شده",
                            value = Helpers.formatPrice(completedRevenue),
                            icon = Icons.Default.CheckCircle,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ReportKpiCard(
                            title = "میانگین فاکتور",
                            value = Helpers.formatPrice(avgOrder),
                            icon = Icons.Default.AccountBalance,
                            tint = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f)
                        )
                        ReportKpiCard(
                            title = "هزینه ارسال",
                            value = Helpers.formatPrice(totalShipping),
                            icon = Icons.Default.LocalShipping,
                            tint = Color(0xFF03A9F4),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("نمودار درآمد هفتگی", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("مجموع فروش روزانه (تومان)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                            Spacer(Modifier.height(16.dp))
                            val barColor = MaterialTheme.colorScheme.primary
                            val barColorAlpha = barColor.copy(alpha = 0.15f)
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                            ) {
                                val barWidth = size.width / (weeklyRevenue.size * 2f)
                                val spacing = barWidth
                                weeklyRevenue.forEachIndexed { idx, value ->
                                    val barHeight = (value.toFloat() / maxWeekly.toFloat()) * size.height * 0.85f
                                    val left = idx * (barWidth + spacing) + spacing / 2
                                    val top = size.height - barHeight
                                    drawRoundRect(
                                        color = barColorAlpha,
                                        topLeft = androidx.compose.ui.geometry.Offset(left, 0f),
                                        size = androidx.compose.ui.geometry.Size(barWidth, size.height),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                                    )
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = androidx.compose.ui.geometry.Offset(left, top),
                                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                weekLabels.forEach { label ->
                                    Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("توزیع سفارش‌ها بر اساس وضعیت", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            val total = ordersList.size.coerceAtLeast(1)
                            statusGroups.entries.sortedByDescending { it.value.size }.forEach { (status, ords) ->
                                val fraction = ords.size.toFloat() / total
                                val persianLabel = try { OrderStatus.valueOf(status).persianLabel } catch (e: Exception) { status }
                                val color = statusColors[status] ?: MaterialTheme.colorScheme.secondary
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(persianLabel, fontSize = 12.sp, modifier = Modifier.width(130.dp))
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(7.dp))
                                            .background(color.copy(alpha = 0.12f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction)
                                                .clip(RoundedCornerShape(7.dp))
                                                .background(color)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "${Helpers.toPersianDigits(ords.size.toString())} سفارش",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(0.6f)
                                    )
                                }
                            }
                        }
                    }
                }

                if (topProducts.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("پرفروش‌ترین محصولات", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.height(12.dp))
                                val maxRev = topProducts.firstOrNull()?.value?.coerceAtLeast(1L) ?: 1L
                                topProducts.forEachIndexed { idx, (name, rev) ->
                                    val fraction = rev.toFloat() / maxRev.toFloat()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                Helpers.toPersianDigits("${idx + 1}"),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Spacer(Modifier.height(3.dp))
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(0.08f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(fraction)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            Helpers.formatPrice(rev),
                                            fontSize = 10.sp,
                                            color = GreenMoney,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (paymentGroups.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("توزیع روش‌های پرداخت", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(Modifier.height(12.dp))
                                val paymentColors = listOf(
                                    Color(0xFF4CAF50), Color(0xFF2196F3),
                                    Color(0xFFFF9800), Color(0xFF9C27B0), Color(0xFFF44336)
                                )
                                val totalOrds = ordersList.size.coerceAtLeast(1)
                                paymentGroups.entries.sortedByDescending { it.value.size }
                                    .forEachIndexed { idx, (method, ords) ->
                                        val color = paymentColors[idx % paymentColors.size]
                                        val fraction = ords.size.toFloat() / totalOrds
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(method.ifBlank { "نامشخص" }, fontSize = 12.sp, modifier = Modifier.width(140.dp))
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(12.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(color.copy(alpha = 0.12f))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .fillMaxWidth(fraction)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(color)
                                                )
                                            }
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "${Helpers.toPersianDigits(ords.size.toString())} سفارش",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f)
                                            )
                                        }
                                    }
                            }
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("خلاصه مشتریان", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Helpers.toPersianDigits(customersList.size.toString()),
                                        fontSize = 22.sp, fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("کل مشتریان", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Helpers.toPersianDigits(customersList.count { it.category == "LOYAL" }.toString()),
                                        fontSize = 22.sp, fontWeight = FontWeight.Black,
                                        color = Color(0xFFE91E63)
                                    )
                                    Text("مشتری وفادار", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Helpers.toPersianDigits(customersList.count { it.category == "NEW" }.toString()),
                                        fontSize = 22.sp, fontWeight = FontWeight.Black,
                                        color = Color(0xFF4CAF50)
                                    )
                                    Text("مشتری جدید", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Helpers.toPersianDigits(customersList.count { it.category == "INACTIVE" }.toString()),
                                        fontSize = 22.sp, fontWeight = FontWeight.Black,
                                        color = Color(0xFF9E9E9E)
                                    )
                                    Text("غیرفعال", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                            }
                        }
                    }
                }

                item {
                    val lowStock = productsList.filter { it.manageStock && it.stockQuantity <= it.lowStockThreshold && it.stockQuantity > 0 }
                    val outOfStock = productsList.filter { !it.inStock || it.stockQuantity <= 0 }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("وضعیت انبار کالاها", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Helpers.toPersianDigits(productsList.size.toString()),
                                        fontSize = 20.sp, fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("کل محصولات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Helpers.toPersianDigits(lowStock.size.toString()),
                                        fontSize = 20.sp, fontWeight = FontWeight.Black,
                                        color = YellowWarn
                                    )
                                    Text("کم‌موجودی", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Helpers.toPersianDigits(outOfStock.size.toString()),
                                        fontSize = 20.sp, fontWeight = FontWeight.Black,
                                        color = RedError
                                    )
                                    Text("ناموجود", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        Helpers.toPersianDigits(productsList.count { it.isFeatured }.toString()),
                                        fontSize = 20.sp, fontWeight = FontWeight.Black,
                                        color = Color(0xFFFF9800)
                                    )
                                    Text("ویژه‌ها", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun ReportKpiCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

// ==========================================
// 6. ADMIN USERS MANAGEMENT SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersScreen(viewModel: WooViewModel, onBack: () -> Unit) {
    val adminUsersList by viewModel.adminUsers.collectAsState(initial = emptyList())
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newUsername by remember { mutableStateOf("") }
    var newRole by remember { mutableStateOf("STORE_ADMIN") }
    var showRoleMenu by remember { mutableStateOf(false) }

    val roleOptions = listOf(
        "SUPER_ADMIN" to "مدیر اصلی",
        "STORE_ADMIN" to "مدیر فروشگاه",
        "ORDER_ADMIN" to "مدیر سفارش‌ها",
        "PRODUCT_ADMIN" to "مدیر محصولات",
        "STOCK_CLERK" to "انباردار",
        "ACCOUNTANT" to "حسابدار",
        "SUPPORT" to "پشتیبان"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("مدیریت کاربران ادمین", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                    text = { Text("افزودن مدیر جدید") }
                )
            }
        ) { paddingValues ->
            if (adminUsersList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("هیچ کاربر مدیری تعریف نشده است", color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item { Spacer(Modifier.height(4.dp)) }
                    items(adminUsersList, key = { it.id }) { user ->
                        val isCurrentUser = user.username == loggedInUser?.username
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentUser)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (user.isActive) MaterialTheme.colorScheme.primary.copy(0.12f)
                                            else Color.Gray.copy(0.1f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = if (user.isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.fullName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (isCurrentUser) {
                                            Spacer(Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(0.1f))
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                                            ) {
                                                Text("شما", fontSize = 9.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text("@${user.username}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                    Spacer(Modifier.height(4.dp))
                                    val rolePersian = roleOptions.find { it.first == user.role }?.second ?: user.role
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.secondary.copy(0.1f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(rolePersian, fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Switch(
                                        checked = user.isActive,
                                        onCheckedChange = { active ->
                                            if (!isCurrentUser) viewModel.toggleAdminUserActive(user.id, active)
                                        },
                                        enabled = !isCurrentUser
                                    )
                                    if (!isCurrentUser) {
                                        IconButton(
                                            onClick = { viewModel.removeAdminUser(user.id) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("افزودن مدیر جدید") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newName,
                            onValueChange = { newName = it },
                            label = { Text("نام کامل") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = newUsername,
                            onValueChange = { newUsername = it },
                            label = { Text("نام کاربری") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        ExposedDropdownMenuBox(
                            expanded = showRoleMenu,
                            onExpandedChange = { showRoleMenu = it }
                        ) {
                            OutlinedTextField(
                                value = roleOptions.find { it.first == newRole }?.second ?: newRole,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("نقش سیستمی") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showRoleMenu) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showRoleMenu,
                                onDismissRequest = { showRoleMenu = false }
                            ) {
                                roleOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { newRole = key; showRoleMenu = false }
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newName.isNotBlank() && newUsername.isNotBlank()) {
                                viewModel.addAdminUser(
                                    com.sahand.wooadmin.data.AdminUser(
                                        fullName = newName,
                                        username = newUsername,
                                        role = newRole,
                                        isActive = true
                                    )
                                )
                                showAddDialog = false
                                newName = ""
                                newUsername = ""
                                newRole = "STORE_ADMIN"
                            }
                        }
                    ) { Text("افزودن") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("انصراف") }
                }
            )
        }
    }
}
