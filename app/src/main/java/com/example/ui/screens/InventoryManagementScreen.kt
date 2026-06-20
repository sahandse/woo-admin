package com.sahand.wooadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
fun InventoryManagementScreen(
    viewModel: WooViewModel,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var stockFilter by remember { mutableStateOf("ALL") }
    var editingProductId by remember { mutableStateOf<Long?>(null) }
    var editingQtyText by remember { mutableStateOf("") }

    val products by viewModel.products.collectAsState()

    val stockFilters = listOf(
        "ALL" to "همه",
        "OUT_OF_STOCK" to "ناموجود",
        "LOW_STOCK" to "کم‌موجود",
        "IN_STOCK" to "موجود"
    )

    val filteredProducts = remember(searchQuery, stockFilter, products) {
        products.filter { prod ->
            val matchesQuery = searchQuery.isBlank() ||
                prod.name.contains(searchQuery, ignoreCase = true) ||
                prod.sku.contains(searchQuery, ignoreCase = true)
            val matchesFilter = when (stockFilter) {
                "OUT_OF_STOCK" -> !prod.inStock || prod.stockQuantity <= 0
                "LOW_STOCK" -> prod.manageStock && prod.stockQuantity > 0 && prod.stockQuantity <= prod.lowStockThreshold
                "IN_STOCK" -> prod.inStock && prod.stockQuantity > prod.lowStockThreshold
                else -> true
            }
            matchesQuery && matchesFilter
        }.sortedWith(Comparator { a, b ->
            val aPriority = when {
                !a.inStock || a.stockQuantity <= 0 -> 0
                a.manageStock && a.stockQuantity <= a.lowStockThreshold -> 1
                else -> 2
            }
            val bPriority = when {
                !b.inStock || b.stockQuantity <= 0 -> 0
                b.manageStock && b.stockQuantity <= b.lowStockThreshold -> 1
                else -> 2
            }
            if (aPriority != bPriority) aPriority.compareTo(bPriority) else a.name.compareTo(b.name)
        })
    }

    val outOfStockCount = products.count { !it.inStock || it.stockQuantity <= 0 }
    val lowStockCount = products.count { it.manageStock && it.stockQuantity > 0 && it.stockQuantity <= it.lowStockThreshold }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("مدیریت انبار هوشمند", fontWeight = FontWeight.Bold) },
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
            ) {
                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InventoryStatCard(
                        label = "ناموجود",
                        count = outOfStockCount,
                        color = Color(0xFFF44336),
                        modifier = Modifier.weight(1f)
                    )
                    InventoryStatCard(
                        label = "کم‌موجود",
                        count = lowStockCount,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                    InventoryStatCard(
                        label = "کل محصولات",
                        count = products.size,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Barcode / Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("اسکن بارکد یا جستجو (SKU / نام)") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* filter already reactive */ })
                )

                // Filter Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(stockFilters) { (key, label) ->
                        FilterChip(
                            selected = stockFilter == key,
                            onClick = { stockFilter = key },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                // Product List
                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory2,
                                null,
                                modifier = Modifier.size(56.dp),
                                tint = Color.LightGray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("محصولی یافت نشد", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredProducts, key = { it.id }) { product ->
                            InventoryProductCard(
                                product = product,
                                isEditing = editingProductId == product.id,
                                editingQtyText = editingQtyText,
                                onEditQty = {
                                    editingProductId = product.id
                                    editingQtyText = product.stockQuantity.toString()
                                },
                                onQtyTextChange = { editingQtyText = it },
                                onSaveQty = {
                                    val newQty = editingQtyText.toIntOrNull() ?: 0
                                    viewModel.updateProductStock(product.id, newQty)
                                    editingProductId = null
                                    editingQtyText = ""
                                },
                                onCancelEdit = {
                                    editingProductId = null
                                    editingQtyText = ""
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun InventoryStatCard(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = color.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun InventoryProductCard(
    product: WooProduct,
    isEditing: Boolean,
    editingQtyText: String,
    onEditQty: () -> Unit,
    onQtyTextChange: (String) -> Unit,
    onSaveQty: () -> Unit,
    onCancelEdit: () -> Unit
) {
    val stockColor = when {
        !product.inStock || product.stockQuantity <= 0 -> Color(0xFFF44336)
        product.manageStock && product.stockQuantity <= product.lowStockThreshold -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
    val stockLabel = when {
        !product.inStock || product.stockQuantity <= 0 -> "ناموجود"
        product.manageStock && product.stockQuantity <= product.lowStockThreshold -> "کم‌موجود"
        else -> "موجود"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(stockColor)
            )
            Spacer(modifier = Modifier.width(10.dp))

            AsyncImage(
                model = product.mainImage,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Gray.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                if (product.sku.isNotBlank()) {
                    Text("SKU: ${product.sku}", fontSize = 11.sp, color = Color.Gray)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(stockColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(stockLabel, fontSize = 10.sp, color = stockColor, fontWeight = FontWeight.Bold)
                    }
                    Text(Helpers.formatPrice(product.regularPrice), fontSize = 11.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isEditing) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    OutlinedTextField(
                        value = editingQtyText,
                        onValueChange = { onQtyTextChange(it.filter { c -> c.isDigit() }) },
                        modifier = Modifier.width(72.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onSaveQty() }),
                        textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        label = { Text("تعداد", fontSize = 10.sp) },
                        shape = RoundedCornerShape(8.dp)
                    )
                    Column {
                        IconButton(onClick = onSaveQty, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onCancelEdit, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = product.stockQuantity.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = stockColor
                    )
                    Text("عدد", fontSize = 10.sp, color = Color.Gray)
                    TextButton(
                        onClick = onEditQty,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Text("ویرایش", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
