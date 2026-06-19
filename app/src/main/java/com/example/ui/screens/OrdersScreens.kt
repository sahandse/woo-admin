package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.Helpers
import com.example.data.OrderStatus
import com.example.data.WooOrder
import com.example.ui.theme.GreenMoney
import com.example.ui.theme.RedError
import com.example.ui.theme.YellowWarn
import com.example.ui.viewmodel.WooViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: WooViewModel,
    onNavigateToDetails: (Long) -> Unit
) {
    val ordersList by viewModel.filteredOrders.collectAsState(initial = emptyList())
    val searchInput by viewModel.orderSearchQuery.collectAsState()
    val statusFilter by viewModel.orderStatusFilter.collectAsState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search & Filter Header block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { viewModel.updateOrderSearch(it) },
                    placeholder = { Text("جستجوی سفارش (نام، شماره، تلفن)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("order_search_bar"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Quick Status filter triggers
                var showFilterDialog by remember { mutableStateOf(false) }
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "فیلتر",
                        tint = Color.White
                    )
                }

                if (showFilterDialog) {
                    AlertDialog(
                        onDismissRequest = { showFilterDialog = false },
                        title = { Text("فیلتر وضعیت سفارش") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.setOrderStatusFilter("ALL"); showFilterDialog = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = if (statusFilter == "ALL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text("نمایش همه", color = if (statusFilter == "ALL") Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                OrderStatus.values().forEach { os ->
                                    Button(
                                        onClick = { viewModel.setOrderStatusFilter(os.name); showFilterDialog = false },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (statusFilter == os.name) Color(android.graphics.Color.parseColor(os.colorHex)) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Text(os.persianLabel, color = if (statusFilter == os.name) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showFilterDialog = false }) { Text("بستن") }
                        }
                    )
                }
            }

            // Quick Info banner
            AnimatedVisibility(visible = statusFilter != "ALL" && statusFilter != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        val label = OrderStatus.values().find { it.name == statusFilter }?.persianLabel ?: ""
                        Text("در حال فیلتر بر اساس: $label", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { viewModel.setOrderStatusFilter("ALL") }) {
                            Text("پاک کردن", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Empty State
            if (ordersList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("سفارشی یافت نشد!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(ordersList, key = { it.id }) { item ->
                        OrderCard(order = item, onClick = { onNavigateToDetails(item.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun OrderCard(order: WooOrder, onClick: () -> Unit) {
    val osEnum = OrderStatus.values().find { it.name == order.status } ?: OrderStatus.NEEDS_FOLLOWUP
    val statusColor = Color(android.graphics.Color.parseColor(osEnum.colorHex))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.orderNumber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${order.createdAtJalali})",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                // Colorized status label
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.1f))
                        .border(1.dp, statusColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = osEnum.persianLabel,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            // Body info
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Person, contentDescription = null, size14Modifier, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = order.customerName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.LocalMall, contentDescription = null, size14Modifier, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.width(4.dp))
                        val itemsSummary = order.items.joinToString { "${it.productName} (x${it.quantity})" }
                        Text(
                            text = itemsSummary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.widthIn(max = 200.dp)
                        )
                    }
                }

                // Total price
                Column(horizontalAlignment = Alignment.End) {
                    Text("مبلغ نهایی", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text(
                        text = Helpers.formatPrice(order.totalAmount),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenMoney
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment and Shipping status row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Payment Status
                val paymentLabel = if (order.isPaid) "پرداخت شده" else "در انتظار پرداخت"
                val paymentBadgeColor = if (order.isPaid) GreenMoney else RedError
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (order.isPaid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = paymentBadgeColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "پرداخت: ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = paymentLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = paymentBadgeColor
                    )
                }

                // Shipping Status
                val (shippingLabel, shippingBadgeColor) = when (order.shippingStatus) {
                    "READY_TO_PACK" -> "آماده بسته‌بندی" to Color(0xFFFF9800)
                    "PACKED" -> "بسته‌بندی شده" to Color(0xFF00BCD4)
                    "READY_TO_SHIP" -> "آماده ارسال" to Color(0xFF9C27B0)
                    "HANDED_TO_POST" -> "تحویل به پست" to Color(0xFF3F51B5)
                    "HANDED_TO_COURIER" -> "تحویل به پیک" to Color(0xFF673AB7)
                    "SHIPPED" -> "ارسال شده" to Color(0xFF2196F3)
                    "DELIVERED" -> "تحویل شده" to GreenMoney
                    "RETURNED" -> "مرجوع شده" to RedError
                    else -> "آماده بسته‌بندی" to Color(0xFFFF9800)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = shippingBadgeColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ارسال: ",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = shippingLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = shippingBadgeColor
                    )
                }
            }
        }
    }
}

val size14Modifier = Modifier.size(14.dp)

// --- DETAIL VIEW WITH PRINTING & EXPORTS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    orderId: Long,
    viewModel: WooViewModel,
    onBack: () -> Unit
) {
    val ordersList by viewModel.orders.collectAsState()
    val order = ordersList.find { it.id == orderId }
    val context = LocalContext.current

    var showTrackingDialog by remember { mutableStateOf(false) }
    var trackingCodeInput by remember { mutableStateOf(order?.trackingCode ?: "") }
    var trackingCompanyInput by remember { mutableStateOf(order?.shippingCompany ?: "پست جمهوری اسلامی") }

    var adminNoteInput by remember { mutableStateOf(order?.adminNotes ?: "") }

    var showInvoiceView by remember { mutableStateOf(false) }
    var showLabelView by remember { mutableStateOf(false) }

    if (order == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("سفارش یافت نشد.")
        }
        return
    }

    val osEnum = OrderStatus.values().find { it.name == order.status } ?: OrderStatus.NEEDS_FOLLOWUP

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showInvoiceView) {
            InvoiceView(order = order, onBack = { showInvoiceView = false })
        } else if (showLabelView) {
            ShippingLabelView(order = order, onBack = { showLabelView = false })
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("جزئیات سفارش ${order.orderNumber}") },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 8.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showInvoiceView = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("فاکتور فارسی", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showLabelView = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("برچسب ارسال", fontSize = 12.sp)
                            }
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    // Status and Action Row
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("وضعیت فعلی:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(android.graphics.Color.parseColor(osEnum.colorHex)).copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        osEnum.persianLabel,
                                        color = Color(android.graphics.Color.parseColor(osEnum.colorHex)),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("تغییر سریع وضعیت سفارش:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Three prominent action buttons as requested (در حال انجام، تکمیل شده، لغو شده)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // 1. در حال انجام (PROCESSING)
                                val isProcessing = order.status == "PROCESSING"
                                Button(
                                    onClick = { 
                                        viewModel.updateOrderStatus(order.id, OrderStatus.PROCESSING)
                                        Toast.makeText(context, "وضعیت سفارش به در حال انجام تغییر یافت.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isProcessing) Color(0xFF2196F3) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        contentColor = if (isProcessing) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Autorenew,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("در حال انجام", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 2. تکمیل شده (COMPLETED)
                                val isCompleted = order.status == "COMPLETED"
                                Button(
                                    onClick = { 
                                        viewModel.updateOrderStatus(order.id, OrderStatus.COMPLETED)
                                        Toast.makeText(context, "وضعیت سفارش به تکمیل‌نشده تغییر یافت.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCompleted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        contentColor = if (isCompleted) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("تکمیل‌شده", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 3. لغوشده (CANCELLED)
                                val isCancelled = order.status == "CANCELLED"
                                Button(
                                    onClick = { 
                                        viewModel.updateOrderStatus(order.id, OrderStatus.CANCELLED)
                                        Toast.makeText(context, "وضعیت سفارش به لغوشده تغییر یافت.", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCancelled) Color(0xFFF44336) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        contentColor = if (isCancelled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text("لغوشده", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text("سایر وضعیت‌ها:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))

                            // Horizontal scroll of all other OrderStatus options for maximum flexibility
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OrderStatus.values().filter { 
                                    it.name != "PROCESSING" && it.name != "COMPLETED" && it.name != "CANCELLED" 
                                }.forEach { os ->
                                    val isSelected = order.status == os.name
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) Color(android.graphics.Color.parseColor(os.colorHex))
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .clickable { 
                                                viewModel.updateOrderStatus(order.id, os)
                                                Toast.makeText(context, "وضعیت به ${os.persianLabel} تغییر یافت.", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            os.persianLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Detailed Payment & Shipping Status Manager Card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("مدیریت پرداخت و ارسال", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. Payment toggle Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (order.isPaid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (order.isPaid) GreenMoney else RedError,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "وضعیت پرداخت: " + (if (order.isPaid) "پرداخت شده" else "در انتظار پرداخت"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Switch(
                                    checked = order.isPaid,
                                    onCheckedChange = { viewModel.updateOrderPaymentStatus(order.id, it) },
                                    thumbContent = if (order.isPaid) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                    } else null
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 2. Shipping status selector
                            Text("وضعیت ارسال مرسوله:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(6.dp))

                            val shippingStatuses = listOf(
                                "READY_TO_PACK" to "بسته‌بندی",
                                "PACKED" to "بسته‌شده",
                                "READY_TO_SHIP" to "آماده ارسال",
                                "HANDED_TO_POST" to "پست",
                                "HANDED_TO_COURIER" to "پیک",
                                "SHIPPED" to "ارسال‌شده",
                                "DELIVERED" to "تحویل‌شده",
                                "RETURNED" to "مرجوعی"
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                shippingStatuses.slice(0..3).forEach { (code, farsi) ->
                                    val isSelected = order.shippingStatus == code
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewModel.updateOrderShippingStatus(order.id, code) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            farsi,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                shippingStatuses.slice(4..7).forEach { (code, farsi) ->
                                    val isSelected = order.shippingStatus == code
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { viewModel.updateOrderShippingStatus(order.id, code) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            farsi,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Contact Shortcuts Row
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("ارتباط با مشتری:", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Call Phone
                                Button(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerPhone}"))
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تماس مستقیم", fontSize = 10.sp)
                                }

                                // SMS Send
                                Button(
                                    onClick = {
                                        val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${order.customerPhone}")).apply {
                                            putExtra("sms_body", "مشتری عزیز ${order.customerName}، سفارش شما به شماره ${order.orderNumber} در حال بررسی است.")
                                        }
                                        context.startActivity(smsIntent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GreenMoney.copy(alpha = 0.1f), contentColor = GreenMoney),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("ارسال پیامک", fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // Tracking Code Section
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("رهگیری و مرسولات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                IconButton(onClick = { showTrackingDialog = true }) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = "ویرایش")
                                }
                            }

                            if (order.trackingCode.isNotBlank()) {
                                Text("خط رهگیری: ${order.trackingCode}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("فرستنده: ${order.shippingCompany}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            } else {
                                Text("هیچ کد رهگیری ثبت نشده است.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                            }
                        }
                    }

                    // Products list table
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("کالاهای خریداری شده", fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))

                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                        Text("تعداد: ${Helpers.toPersianDigits(item.quantity)} عدد × ${Helpers.formatPrice(item.unitPrice)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                    Text(Helpers.formatPrice(item.totalPrice), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Divider(modifier = Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            }

                            // Math prices
                            Spacer(modifier = Modifier.height(8.dp))
                            PriceRow("مجموع بدون تخفیف", order.subtotal)
                            PriceRow("میزان تخفیف", -order.discount, isDiscount = true)
                            PriceRow("هزینه ارسال", order.shippingCost)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("جمع کل پرداختی", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(Helpers.formatPrice(order.totalAmount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = GreenMoney)
                            }
                        }
                    }

                    // Admin Internal notes
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("یادداشت‌های داخلی (مخصوص مدیران):", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = adminNoteInput,
                                onValueChange = {
                                    adminNoteInput = it
                                    viewModel.updateOrderAdminNotes(order.id, it)
                                },
                                placeholder = { Text("ثبت یادداشت خصوصی درباره این سفارش...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Tracking dialog overlay
            if (showTrackingDialog) {
                AlertDialog(
                    onDismissRequest = { showTrackingDialog = false },
                    title = { Text("ثبت مرسوله و کد رهگیری") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = trackingCodeInput,
                                onValueChange = { trackingCodeInput = it },
                                label = { Text("کد رهگیری پستی (۲۴ رقمی)") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = trackingCompanyInput,
                                onValueChange = { trackingCompanyInput = it },
                                label = { Text("شرکت حمل و نقل یا باربری") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.addOrderTracking(order.id, trackingCodeInput, trackingCompanyInput)
                                showTrackingDialog = false
                            }
                        ) {
                            Text("ذخیره")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showTrackingDialog = false }) { Text("انصراف") }
                    }
                )
            }
        }
    }
}

@Composable
fun PriceRow(label: String, value: Long, isDiscount: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(
            text = Helpers.formatPrice(value),
            fontSize = 12.sp,
            color = if (isDiscount) RedError else MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- DESIGNS EXTRAS (INVOICES & LABELS) ---

@Composable
fun InvoiceView(order: WooOrder, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.Black)
                }
                Text("فاکتور رسمی فروش محصولات", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Box(modifier = Modifier.size(24.dp))
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray)

            // Header log details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("فروشگاه: مد و پوشاک ووکامرس", color = Color.DarkGray, fontSize = 12.sp)
                    Text("کد سفارش: ${order.orderNumber}", color = Color.DarkGray, fontSize = 12.sp)
                    Text("تاریخ ثبت: ${order.createdAtJalali}", color = Color.DarkGray, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("گیرنده: ${order.customerName}", color = Color.DarkGray, fontSize = 12.sp)
                    Text("تماس: ${order.customerPhone}", color = Color.DarkGray, fontSize = 12.sp)
                    Text("استان/شهر: ${order.province} / ${order.city}", color = Color.DarkGray, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("آدرس ارسال: ${order.shippingAddress}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

            Spacer(modifier = Modifier.height(12.dp))

            // Products Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color.LightGray)
                    .padding(8.dp)
            ) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("نام محصول", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
                        Text("تعداد", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
                        Text("قیمت کل", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    order.items.forEach { item ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(item.productName, fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(2f))
                            Text(Helpers.toPersianDigits(item.quantity) + " عدد", fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
                            Text(Helpers.formatPrice(item.totalPrice), fontSize = 11.sp, color = Color.Black, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                Text("مبلغ کل خرید: ${Helpers.formatPrice(order.subtotal)}", fontSize = 12.sp, color = Color.Black)
                Text("تخفیف: ${Helpers.formatPrice(order.discount)}", fontSize = 12.sp, color = Color.Red)
                Text("هزینه ارسال پستی: ${Helpers.formatPrice(order.shippingCost)}", fontSize = 12.sp, color = Color.Black)
                Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray)
                Text("مبلغ خالص پرداختی: ${Helpers.formatPrice(order.totalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("محل امضا و مهر فروشگاه", fontSize = 11.sp, color = Color.Gray)
                Text("تحویل دهنده مرسوله و امضا", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ShippingLabelView(order: WooOrder, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, Color.Black, RoundedCornerShape(12.dp))
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.Black)
                }
                Text("برچسب استاندارد پستی", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Box(modifier = Modifier.size(24.dp))
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Black)

            // Sender Information
            Text("فرستنده:", fontWeight = FontWeight.Bold, color = Color.DarkGray, fontSize = 11.sp)
            Text("فروشگاه مرکزی زنجیره‌ای مد و دیجیتال ووکامرس", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("تهران، بزرگراه صدر، ورودی کاوه شمالی، ساختمان باران، طبقه اول", color = Color.Black, fontSize = 12.sp)
            Text("تلفن دفتر: ۰۲۱-۲۲۴۴۶۶۸۸   کد پستی: ۱۹۳۷۵۴۳۲۱", color = Color.Black, fontSize = 11.sp)

            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)

            // Receiver Information
            Text("گیرنده مرسوله:", fontWeight = FontWeight.Bold, color = Color.Blue, fontSize = 11.sp)
            Text(order.customerName, color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(order.shippingAddress, color = Color.Black, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("تلفن همراه: ${order.customerPhone}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                Text("کد پستی: ${order.postalCode}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.Black)

            // Barcode illustration
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("سفارش شماره: ${order.orderNumber}", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("نحوه ارسال: ${order.shippingCompany}", color = Color.Black, fontSize = 11.sp)
                }
                
                // Draw mock postal barcode
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier
                            .height(28.dp)
                            .background(Color.White),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf(2, 4, 1, 3, 2, 4, 1, 2, 3, 1, 4, 2, 3).forEach { w ->
                            Box(
                                modifier = Modifier
                                    .width(w.dp)
                                    .fillMaxHeight()
                                    .background(Color.Black)
                            )
                        }
                    }
                    Text(order.orderNumber, color = Color.Black, fontSize = 10.sp)
                }
            }
        }
    }
}
