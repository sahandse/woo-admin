package com.sahand.wooadmin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sahand.wooadmin.core.utils.Helpers
import com.sahand.wooadmin.data.WooProduct
import com.sahand.wooadmin.ui.viewmodel.WooViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    viewModel: WooViewModel,
    onBack: () -> Unit
) {
    var cartItems by remember { mutableStateOf<List<Pair<WooProduct, Int>>>(emptyList()) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var discountAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("نقد") }
    var productSearch by remember { mutableStateOf("") }
    var showProductSearch by remember { mutableStateOf(false) }
    var showPaymentDropdown by remember { mutableStateOf(false) }
    var orderNote by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val products by viewModel.products.collectAsState()

    val filteredProductsForSearch = remember(productSearch, products) {
        if (productSearch.isBlank()) emptyList()
        else products.filter {
            it.name.contains(productSearch, ignoreCase = true) ||
            it.sku.contains(productSearch, ignoreCase = true)
        }.take(8)
    }

    val subtotal = cartItems.sumOf { (prod, qty) ->
        (prod.salePrice.takeIf { it > 0 } ?: prod.regularPrice) * qty
    }
    val discount = discountAmount.toLongOrNull() ?: 0L
    val total = (subtotal - discount).coerceAtLeast(0)
    val paymentMethods = listOf("نقد", "کارت‌خوان", "کارت به کارت", "چک", "قسطی")

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false; onBack() },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp)) },
            title = { Text("سفارش ثبت شد", fontWeight = FontWeight.Bold) },
            text = { Text("سفارش داخلی با موفقیت ثبت شد و موجودی محصولات بروزرسانی گردید.") },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false; onBack() }) { Text("تأیید") }
            }
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ثبت سفارش داخلی", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // --- Product search & cart ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("محصولات سبد خرید", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        OutlinedTextField(
                            value = productSearch,
                            onValueChange = {
                                productSearch = it
                                showProductSearch = it.isNotBlank()
                            },
                            label = { Text("جستجوی محصول (نام یا کد)") },
                            leadingIcon = { Icon(Icons.Default.Search, null) },
                            trailingIcon = {
                                if (productSearch.isNotBlank()) {
                                    IconButton(onClick = { productSearch = ""; showProductSearch = false }) {
                                        Icon(Icons.Default.Clear, null)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        AnimatedVisibility(visible = showProductSearch && filteredProductsForSearch.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                elevation = CardDefaults.cardElevation(6.dp)
                            ) {
                                Column {
                                    filteredProductsForSearch.forEachIndexed { index, prod ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val existing = cartItems.indexOfFirst { it.first.id == prod.id }
                                                    cartItems = if (existing >= 0) {
                                                        cartItems.toMutableList().also { list ->
                                                            list[existing] = list[existing].copy(second = list[existing].second + 1)
                                                        }
                                                    } else {
                                                        cartItems + Pair(prod, 1)
                                                    }
                                                    productSearch = ""
                                                    showProductSearch = false
                                                }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AsyncImage(
                                                model = prod.mainImage,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(prod.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                                Text(
                                                    "موجودی: ${prod.stockQuantity}  |  ${prod.sku}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Text(
                                                Helpers.formatPrice(prod.regularPrice),
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        if (index < filteredProductsForSearch.lastIndex) HorizontalDivider()
                                    }
                                }
                            }
                        }

                        if (cartItems.isNotEmpty()) {
                            cartItems.forEach { (prod, qty) ->
                                val unitPrice = prod.salePrice.takeIf { it > 0 } ?: prod.regularPrice
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                        Text(Helpers.formatPrice(unitPrice), fontSize = 11.sp, color = Color.Gray)
                                    }
                                    IconButton(
                                        onClick = {
                                            val idx = cartItems.indexOfFirst { it.first.id == prod.id }
                                            cartItems = if (qty <= 1) {
                                                cartItems.filterNot { it.first.id == prod.id }
                                            } else {
                                                cartItems.toMutableList().also { it[idx] = it[idx].copy(second = qty - 1) }
                                            }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, null, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        "$qty",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        fontSize = 15.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            val idx = cartItems.indexOfFirst { it.first.id == prod.id }
                                            cartItems = cartItems.toMutableList().also { it[idx] = it[idx].copy(second = qty + 1) }
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        Helpers.formatPrice(unitPrice * qty),
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { cartItems = cartItems.filterNot { it.first.id == prod.id } },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFFF44336), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("هیچ محصولی انتخاب نشده", fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // --- Customer info ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("اطلاعات مشتری", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("نام و نام خانوادگی") },
                            leadingIcon = { Icon(Icons.Default.Badge, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("شماره تلفن") },
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(10.dp)
                        )
                        OutlinedTextField(
                            value = customerAddress,
                            onValueChange = { customerAddress = it },
                            label = { Text("آدرس (اختیاری)") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // --- Payment & discount ---
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("پرداخت و تخفیف", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }

                        ExposedDropdownMenuBox(
                            expanded = showPaymentDropdown,
                            onExpandedChange = { showPaymentDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = paymentMethod,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("روش پرداخت") },
                                leadingIcon = { Icon(Icons.Default.AccountBalance, null) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaymentDropdown) },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                shape = RoundedCornerShape(10.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = showPaymentDropdown,
                                onDismissRequest = { showPaymentDropdown = false }
                            ) {
                                paymentMethods.forEach { method ->
                                    DropdownMenuItem(
                                        text = { Text(method) },
                                        onClick = { paymentMethod = method; showPaymentDropdown = false }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = discountAmount,
                            onValueChange = { discountAmount = it.filter { c -> c.isDigit() } },
                            label = { Text("مبلغ تخفیف (تومان)") },
                            leadingIcon = { Icon(Icons.Default.LocalOffer, null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp)
                        )

                        OutlinedTextField(
                            value = orderNote,
                            onValueChange = { orderNote = it },
                            label = { Text("یادداشت سفارش (اختیاری)") },
                            leadingIcon = { Icon(Icons.Default.EditNote, null) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                // --- Order summary ---
                if (cartItems.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("خلاصه سفارش", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("جمع کل:", fontSize = 13.sp)
                                Text(Helpers.formatPrice(subtotal), fontSize = 13.sp)
                            }
                            if (discount > 0) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("تخفیف:", fontSize = 13.sp, color = Color(0xFFF44336))
                                    Text("- ${Helpers.formatPrice(discount)}", fontSize = 13.sp, color = Color(0xFFF44336))
                                }
                            }
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("مبلغ قابل پرداخت:", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    Helpers.formatPrice(total),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("روش پرداخت:", fontSize = 12.sp, color = Color.Gray)
                                Text(paymentMethod, fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // --- Submit button ---
                Button(
                    onClick = {
                        viewModel.createManualOrder(
                            items = cartItems,
                            customerName = customerName.ifBlank { "مشتری حضوری" },
                            customerPhone = customerPhone,
                            customerAddress = customerAddress,
                            discount = discount,
                            paymentMethod = paymentMethod,
                            note = orderNote
                        )
                        showSuccessDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = cartItems.isNotEmpty(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت سفارش داخلی", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
