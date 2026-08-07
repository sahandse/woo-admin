package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.Helpers
import com.example.data.AdminActivity
import com.example.data.OrderStatus
import com.example.data.WooCoupon
import com.example.data.WooOrder
import com.example.ui.theme.GreenMoney
import com.example.ui.theme.RedError
import com.example.ui.theme.YellowWarn
import com.example.ui.viewmodel.WooViewModel

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
    val notificationsList by viewModel.notifications.collectAsState(initial = emptyList())

    val unreadNewOrders = notificationsList.filter { !it.isRead && it.type == "NEW_ORDER" }
    val lowStockProducts = productsList.filter { it.manageStock && it.stockQuantity <= it.lowStockThreshold }

    val salesToday = ordersList.sumOf { it.totalAmount }
    val averageOrderValue = if (ordersList.isNotEmpty()) salesToday / ordersList.size else 0L
    val processingCount = ordersList.count { it.status == OrderStatus.PROCESSING.name }
    val pendingCount = ordersList.count { it.status == OrderStatus.PENDING_PAYMENT.name }
    val lowStockCount = productsList.count { it.manageStock && it.stockQuantity <= it.lowStockThreshold && it.stockQuantity > 0 }
    val outOfStockCount = productsList.count { !it.inStock || it.stockQuantity <= 0 }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            if (unreadNewOrders.isNotEmpty()) {
                val latest = unreadNewOrders.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(latest.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(latest.body, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                        TextButton(onClick = { viewModel.readNotification(latest.id); onNavigateToOrdersWithFilter("ALL") }) {
                            Text("مشاهده", fontSize = 11.sp)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardStatCard(
                    title = "فروش امروز",
                    value = Helpers.formatPrice(salesToday),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                DashboardStatCard(
                    title = "میانگین فاکتور",
                    value = Helpers.formatPrice(averageOrderValue),
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardStatCard(
                    title = "در حال پردازش",
                    value = processingCount.toString(),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToOrdersWithFilter(OrderStatus.PROCESSING.name) }
                )
                DashboardStatCard(
                    title = "معلق/پرداخت‌نشده",
                    value = pendingCount.toString(),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToOrdersWithFilter(OrderStatus.PENDING_PAYMENT.name) }
                )
                DashboardStatCard(
                    title = "کسری موجودی",
                    value = (lowStockCount + outOfStockCount).toString(),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToProductsWithFilter("LOW_STOCK") }
                )
            }

            if (lowStockProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("اعلان موجودی", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        lowStockProducts.take(3).forEach { prod ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(prod.name, modifier = Modifier.weight(1f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${prod.stockQuantity} عدد", fontSize = 11.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (lowStockProducts.size > 3) {
                            TextButton(onClick = { onNavigateToProductsWithFilter("LOW_STOCK") }) {
                                Text("+ مشاهده ${lowStockProducts.size - 3} محصول دیگر", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

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
