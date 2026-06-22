package com.sahand.wooadmin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import coil.compose.AsyncImage
import com.sahand.wooadmin.core.utils.Helpers
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import com.sahand.wooadmin.data.WooProduct
import com.sahand.wooadmin.ui.theme.GreenMoney
import com.sahand.wooadmin.ui.theme.RedError
import com.sahand.wooadmin.ui.theme.YellowWarn
import com.sahand.wooadmin.ui.viewmodel.WooViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: WooViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToInventory: () -> Unit = {}
) {
    val productsList by viewModel.filteredProducts.collectAsState(initial = emptyList())
    val searchInput by viewModel.productSearchQuery.collectAsState()
    val filterType by viewModel.productFilter.collectAsState()
    val activeCategory by viewModel.productCategoryFilter.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var showFilters by remember { mutableStateOf(false) }

    var showQuickStockDialog by remember { mutableStateOf<WooProduct?>(null) }
    var stockInput by remember { mutableStateOf("") }

    var showQuickPriceDialog by remember { mutableStateOf<WooProduct?>(null) }
    var regularPriceInput by remember { mutableStateOf("") }
    var salePriceInput by remember { mutableStateOf("") }

    // Bulk price mode
    var bulkPriceMode by remember { mutableStateOf(false) }
    var selectedProducts by remember { mutableStateOf(setOf<Long>()) }
    var bulkPercentInput by remember { mutableStateOf("") }
    var showBulkPriceConfirm by remember { mutableStateOf(false) }

    // Barcode scan
    var showBarcodeFallback by remember { mutableStateOf(false) }
    var barcodeManualInput by remember { mutableStateOf("") }
    val barcodeLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.getStringExtra("SCAN_RESULT")?.let { viewModel.updateProductSearch(it) }
    }

    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { viewModel.syncAllData() },
            modifier = Modifier.fillMaxSize()
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Block
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { viewModel.updateProductSearch(it) },
                    placeholder = { Text("جستجوی کالا (نام کالا، SKU)...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("product_search_bar"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Barcode scan icon
                IconButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent("com.google.zxing.client.android.SCAN")
                            barcodeLauncher.launch(intent)
                        } catch (e: Exception) {
                            showBarcodeFallback = true
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = "اسکن بارکد", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                }

                // Bulk price mode toggle
                IconButton(
                    onClick = {
                        bulkPriceMode = !bulkPriceMode
                        if (!bulkPriceMode) selectedProducts = emptySet()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (bulkPriceMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = if (bulkPriceMode) Icons.Default.PriceChange else Icons.Default.Percent,
                        contentDescription = "مدیریت انبوه قیمت",
                        tint = if (bulkPriceMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Advanced Filter icon button
                IconButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (showFilters || activeCategory != "ALL" || filterType != "ALL")
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (showFilters || activeCategory != "ALL" || filterType != "ALL")
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "فیلتر پیشرفته",
                        tint = if (showFilters || activeCategory != "ALL" || filterType != "ALL")
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Advanced Filters Expandable Card Panel
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FilterAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "فیلتر پیشرفته محصولات",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            
                            if (activeCategory != "ALL" || filterType != "ALL") {
                                Text(
                                    text = "پاکسازی فیلترها",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.clickable {
                                        viewModel.setProductFilter("ALL")
                                        viewModel.setProductCategoryFilter("ALL")
                                    }
                                )
                            }
                        }

                        Spacer(
                            modifier = Modifier
                                .height(1.dp)
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        )

                        // Category Filter Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "دسته بندی کالا:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            
                            val allProductsList by viewModel.products.collectAsState(initial = emptyList())
                            val dynamicCategories = remember(allProductsList) {
                                listOf("ALL") + allProductsList.flatMap { it.categories.split(",") }
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                    .distinct()
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                dynamicCategories.forEach { cat ->
                                    val isCatSelected = activeCategory == cat
                                    val catLabel = if (cat == "ALL") "همه دسته‌ها" else cat
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isCatSelected) MaterialTheme.colorScheme.primary 
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isCatSelected) MaterialTheme.colorScheme.primary 
                                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.setProductCategoryFilter(cat) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = catLabel,
                                            fontSize = 11.sp,
                                            fontWeight = if (isCatSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isCatSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Stock Availability Filter Row
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "موجودی و وضعیت انبار:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val stockFilters = listOf(
                                    "ALL" to "همه محصولات",
                                    "LOW_STOCK" to "کم‌موجودی‌ها ⚠️",
                                    "OUT_OF_STOCK" to "فقط ناموجودها ❌",
                                    "FEATURED" to "پیشنهاد ویژه ⭐",
                                    "DRAFT" to "پیش‌نویس‌ها 📄"
                                )
                                
                                stockFilters.forEach { (type, label) ->
                                    val isSelected = filterType == type
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.secondary 
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.secondary 
                                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.setProductFilter(type) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick Horizontal Filter Toggles (Active indicators of selection)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val activeTypeLabel = when (filterType) {
                    "LOW_STOCK" -> "کم‌موجودی"
                    "OUT_OF_STOCK" -> "فقط ناموجود"
                    "FEATURED" -> "پیشنهاد ویژه"
                    "DRAFT" -> "پیش‌نویس"
                    else -> "همه کالاها"
                }
                
                val activeCatLabel = if (activeCategory == "ALL") "همه دسته‌ها" else "دسته: $activeCategory"

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { showFilters = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.FilterAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Text("موجودی: $activeTypeLabel", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { showFilters = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                        Text(activeCatLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }

                if (activeCategory != "ALL" || filterType != "ALL") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable {
                                viewModel.setProductFilter("ALL")
                                viewModel.setProductCategoryFilter("ALL")
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                            Text("حذف فیلترها", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Products Catalog
            when {
                isSyncing && productsList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Text("در حال بارگذاری محصولات...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
                productsList.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(
                                imageVector = Icons.Default.ProductionQuantityLimits,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(72.dp)
                            )
                            Text(
                                if (searchInput.isNotBlank() || filterType != "ALL" || activeCategory != "ALL")
                                    "کالایی با این فیلتر یافت نشد"
                                else
                                    "هیچ محصولی وجود ندارد",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            if (searchInput.isBlank() && filterType == "ALL" && activeCategory == "ALL") {
                                Button(onClick = { viewModel.syncAllData() }) {
                                    Icon(Icons.Default.Sync, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("همگام‌سازی با فروشگاه", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = if (selectedProducts.isNotEmpty()) 88.dp else 12.dp)
                    ) {
                        items(productsList, key = { it.id }) { product ->
                            ProductItemCard(
                                product = product,
                                onEdit = { onNavigateToEdit(product.id) },
                                onPriceChange = {
                                    showQuickPriceDialog = product
                                    regularPriceInput = product.regularPrice.toString()
                                    salePriceInput = product.salePrice.toString()
                                },
                                onStockChange = {
                                    showQuickStockDialog = product
                                    stockInput = product.stockQuantity.toString()
                                },
                                onClone = { viewModel.cloneProduct(product); Toast.makeText(context, "پیش‌نویس کپی ایجاد شد", Toast.LENGTH_SHORT).show() },
                                onIncrementStock = { viewModel.updateProductStock(product.id, product.stockQuantity + 1) },
                                onDecrementStock = { viewModel.updateProductStock(product.id, (product.stockQuantity - 1).coerceAtLeast(0)) },
                                isSelected = product.id in selectedProducts,
                                showCheckbox = bulkPriceMode,
                                onToggleSelect = {
                                    selectedProducts = if (product.id in selectedProducts)
                                        selectedProducts - product.id else selectedProducts + product.id
                                }
                            )
                        }
                    }
                }
            }
        } // end inner Column
        } // end PullToRefreshBox

        // Bulk price bottom bar
        AnimatedVisibility(
            visible = selectedProducts.isNotEmpty(),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(tonalElevation = 12.dp, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.tertiary) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        "${Helpers.toPersianDigits(selectedProducts.size.toString())} محصول انتخاب شده",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = bulkPercentInput,
                            onValueChange = { bulkPercentInput = it },
                            placeholder = { Text("درصد تغییر قیمت (مثلاً ۱۵ یا -۱۰)", fontSize = 11.sp, color = Color.White.copy(alpha = 0.6f)) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            )
                        )
                        Button(
                            onClick = {
                                val pct = bulkPercentInput.toIntOrNull()
                                if (pct != null) {
                                    viewModel.bulkUpdateProductPrice(selectedProducts, pct)
                                    val sign = if (pct >= 0) "+" else ""
                                    Toast.makeText(context, "$sign$pct٪ قیمت برای ${selectedProducts.size} محصول اعمال شد", Toast.LENGTH_SHORT).show()
                                    selectedProducts = emptySet()
                                    bulkPriceMode = false
                                    bulkPercentInput = ""
                                } else {
                                    Toast.makeText(context, "درصد معتبری وارد کنید", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("اعمال", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        IconButton(onClick = { selectedProducts = emptySet(); bulkPriceMode = false; bulkPercentInput = "" }) {
                            Icon(Icons.Default.Close, null, tint = Color.White)
                        }
                    }
                }
            }
        }
        } // end Box

        // Barcode manual input fallback dialog
        if (showBarcodeFallback) {
            AlertDialog(
                onDismissRequest = { showBarcodeFallback = false },
                title = { Text("جستجوی بارکد / SKU") },
                text = {
                    Column {
                        Text("نرم‌افزار بارکدخوان نصب نیست. SKU یا کد محصول را وارد کنید:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = barcodeManualInput,
                            onValueChange = { barcodeManualInput = it },
                            placeholder = { Text("SKU یا بارکد محصول") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.updateProductSearch(barcodeManualInput)
                        barcodeManualInput = ""
                        showBarcodeFallback = false
                    }) { Text("جستجو") }
                },
                dismissButton = { TextButton(onClick = { showBarcodeFallback = false }) { Text("انصراف") } }
            )
        }

        // Quick Stock adjust Dialog
        if (showQuickStockDialog != null) {
            val prod = showQuickStockDialog!!
            AlertDialog(
                onDismissRequest = { showQuickStockDialog = null },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "به‌روزرسانی سریع انبار",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = prod.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Increment / Decrement Counter Layout
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = {
                                    val currentVal = stockInput.toIntOrNull() ?: 0
                                    if (currentVal > 0) {
                                        stockInput = (currentVal - 1).toString()
                                    }
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "کاهش کم", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            OutlinedTextField(
                                value = stockInput,
                                onValueChange = { stockInput = it },
                                label = { Text("موجودی فعلی", fontSize = 11.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 12.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            IconButton(
                                onClick = {
                                    val currentVal = stockInput.toIntOrNull() ?: 0
                                    stockInput = (currentVal + 1).toString()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "افزایش کم", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Presets Buttons Row
                        Text("میانبرهای انبارداری:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val presetsList = listOf(
                                "+5" to 5,
                                "+10" to 10,
                                "+20" to 20,
                                "صفر" to -9999
                            )
                            presetsList.forEach { (label, diff) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .clickable {
                                            if (diff == -9999) {
                                                stockInput = "0"
                                            } else {
                                                val currentVal = stockInput.toIntOrNull() ?: 0
                                                stockInput = (currentVal + diff).coerceAtLeast(0).toString()
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Low stock threshold visualization indicator
                        val qty = stockInput.toIntOrNull()
                        if (qty != null) {
                            val threshold = prod.lowStockThreshold
                            val isLowStock = qty <= threshold
                            val infoColor = if (qty == 0) RedError else if (isLowStock) YellowWarn else GreenMoney
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(infoColor.copy(alpha = 0.08f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (qty == 0) Icons.Default.Cancel else if (isLowStock) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = infoColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (qty == 0) "کالا ناموجود خواهد شد" else if (isLowStock) "هشدار کم بودن موجودی (آستانه: $threshold)" else "موجودی انبار کافی و سالم",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = infoColor
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                        val qty = stockInput.toIntOrNull()
                            if (qty != null && qty >= 0) {
                                viewModel.updateProductStock(prod.id, qty)
                                showQuickStockDialog = null
                                Toast.makeText(context, "موجودی انبار بروز شد", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "فارغ از خطا، مقدار عددی معتبری وارد کنید", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ذخیره تغییرات")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuickStockDialog = null }) { Text("انصراف") }
                }
            )
        }

        // Quick Price adjust Dialog
        if (showQuickPriceDialog != null) {
            val prod = showQuickPriceDialog!!
            AlertDialog(
                onDismissRequest = { showQuickPriceDialog = null },
                shape = RoundedCornerShape(24.dp),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachMoney,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "تنظیم و قیمت‌گذاری کالا",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = prod.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        OutlinedTextField(
                            value = regularPriceInput,
                            onValueChange = { regularPriceInput = it },
                            label = { Text("قیمت عادی خرید (تومان)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        OutlinedTextField(
                            value = salePriceInput,
                            onValueChange = { salePriceInput = it },
                            label = { Text("قیمت ویژه تخفیف‌خورده (تومان)", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("اختیاری / جهت اعمال تخفیف") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Real-time calculation visualizer
                        val regVal = regularPriceInput.toLongOrNull()
                        val saleVal = salePriceInput.toLongOrNull() ?: 0L
                        if (regVal != null && regVal > 0) {
                            if (saleVal in 1 until regVal) {
                                val discountPercent = ((regVal - saleVal).toDouble() * 100 / regVal).toInt()
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GreenMoney.copy(alpha = 0.08f))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "تخفیف اعمال شده:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GreenMoney
                                    )
                                    Text(
                                        text = "$discountPercent% کاهش قیمت",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GreenMoney
                                    )
                                }
                            } else if (saleVal >= regVal) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(RedError.copy(alpha = 0.08f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Error, contentDescription = null, tint = RedError, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "قیمت تخفیف نباید مایل یا مساوی قیمت عادی باشد",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedError
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val regular = regularPriceInput.toLongOrNull()
                            val sale = salePriceInput.toLongOrNull() ?: 0L
                            if (regular != null && regular > 0) {
                                viewModel.updateProductPrice(prod.id, regular, sale)
                                showQuickPriceDialog = null
                                Toast.makeText(context, "قیمت جدید با موفقیت ثبت شد", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "وارد کردن قیمت اصلی الزامی است", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("ثبت قیمت")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showQuickPriceDialog = null }) { Text("انصراف") }
                }
            )
        }
    }
}

@Composable
fun FilterTabItem(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
            .border(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ProductItemCard(
    product: WooProduct,
    onEdit: () -> Unit,
    onPriceChange: () -> Unit,
    onStockChange: () -> Unit,
    onClone: () -> Unit = {},
    onIncrementStock: () -> Unit = {},
    onDecrementStock: () -> Unit = {},
    isSelected: Boolean = false,
    showCheckbox: Boolean = false,
    onToggleSelect: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = showCheckbox) { onToggleSelect() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                 else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bulk checkbox
            if (showCheckbox) {
                Checkbox(checked = isSelected, onCheckedChange = { onToggleSelect() }, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Product image with elegant corner clips and thin border representation
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AsyncImage(
                    model = product.mainImage,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Overlay for out-of-stock overlay text
                if (product.stockQuantity == 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "اتمام موجودی",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "SKU: ${product.sku}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Price display tags with percent discounts
                // For variable products, derive effective price from variants
                val isVariable = product.variants.isNotEmpty()
                val effectiveRegular = if (isVariable && product.regularPrice == 0L)
                    product.variants.minOfOrNull { it.regularPrice } ?: 0L
                else product.regularPrice
                val effectiveSale = if (isVariable && product.salePrice == 0L)
                    product.variants.filter { it.salePrice > 0 }.minOfOrNull { it.salePrice } ?: 0L
                else product.salePrice
                val maxVariantPrice = if (isVariable) product.variants.maxOfOrNull { it.regularPrice } ?: 0L else 0L
                val hasPriceRange = isVariable && maxVariantPrice > effectiveRegular && effectiveRegular > 0L

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (hasPriceRange) {
                        Column {
                            Text(
                                text = "از ${Helpers.formatPrice(effectiveRegular)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "تا ${Helpers.formatPrice(maxVariantPrice)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    } else if (effectiveSale > 0 && effectiveRegular > 0) {
                        Text(
                            text = Helpers.formatPrice(effectiveSale),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = GreenMoney
                        )
                        Text(
                            text = Helpers.formatPrice(effectiveRegular),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.offset(y = 1.dp),
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                        val pct = ((effectiveRegular - effectiveSale).toDouble() * 100 / effectiveRegular).toInt()
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(RedError.copy(alpha = 0.1f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "٪$pct-",
                                color = RedError,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Text(
                            text = Helpers.formatPrice(effectiveRegular),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (effectiveRegular == 0L) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Variant count + attribute chips (only for variable products)
                if (isVariable) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // تعداد تنوع
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${Helpers.toPersianDigits(product.variants.size.toString())} تنوع",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        // موجودی کل واریانت‌ها
                        val totalVariantStock = product.variants.sumOf { it.stockQty }
                        val variantStockColor = if (totalVariantStock == 0) RedError
                            else if (totalVariantStock <= product.variants.size * 2) YellowWarn
                            else GreenMoney
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(variantStockColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "موجودی: ${Helpers.toPersianDigits(totalVariantStock.toString())}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = variantStockColor
                            )
                        }
                    }
                    // نام ویژگی‌ها
                    if (product.colors.isNotEmpty() || product.sizes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        val attrSummary = buildString {
                            if (product.colors.isNotEmpty()) append("${product.colorAttributeName}: ${product.colors.take(3).joinToString("، ")}${if (product.colors.size > 3) " ..." else ""}")
                            if (product.sizes.isNotEmpty()) {
                                if (product.colors.isNotEmpty()) append(" | ")
                                append("${product.sizeAttributeName}: ${product.sizes.take(3).joinToString("، ")}${if (product.sizes.size > 3) " ..." else ""}")
                            }
                        }
                        Text(
                            text = attrSummary,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stock status indicator
                val stockColor = if (product.stockQuantity == 0) RedError 
                    else if (product.stockQuantity <= product.lowStockThreshold) YellowWarn 
                    else GreenMoney
                
                val stockText = if (product.stockQuantity == 0) "ناموجود در انبار"
                    else if (product.stockQuantity <= product.lowStockThreshold) "رو به اتمام (${Helpers.toPersianDigits(product.stockQuantity)} عدد)"
                    else "موجود در انبار (${Helpers.toPersianDigits(product.stockQuantity)} عدد)"

                Column(modifier = Modifier.fillMaxWidth()) {
                    // Inline stock editor row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(stockColor))
                        Text(
                            text = if (product.stockQuantity == 0) "ناموجود" else if (product.stockQuantity <= product.lowStockThreshold) "رو به اتمام" else "موجود",
                            color = stockColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        // Inline -/qty/+ controls
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape)
                                .background(stockColor.copy(alpha = 0.12f))
                                .clickable { onDecrementStock() },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp), tint = stockColor) }
                        Text(
                            text = Helpers.toPersianDigits(product.stockQuantity.toString()),
                            fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = stockColor,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Box(
                            modifier = Modifier.size(20.dp).clip(CircleShape)
                                .background(stockColor.copy(alpha = 0.12f))
                                .clickable { onIncrementStock() },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp), tint = stockColor) }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    val progressFraction = if (product.stockQuantity == 0) 0f
                        else if (product.stockQuantity <= product.lowStockThreshold) 0.35f else 1.0f
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth(0.9f).height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = stockColor, trackColor = stockColor.copy(alpha = 0.12f)
                    )
                }
            }

            // Quick Operations Column (Pricing, Stock-adjust, and Detailed Editing)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(horizontal = 4.dp, vertical = 6.dp)
            ) {
                // Price adjustment quick button
                IconButton(
                    onClick = onPriceChange,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = "ویرایش سریع قیمت",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Inventory adjustment quick button
                IconButton(
                    onClick = onStockChange,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = "ویرایش سریع انبار",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Clone product
                IconButton(
                    onClick = onClone,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "کپی محصول",
                        tint = GreenMoney,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Complete full-form edit
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "ویرایش کامل مشخصات",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

// --- FULL DETAILED ADD / EDIT PRODUCT FORM ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(
    productId: Long?,
    viewModel: WooViewModel,
    onBack: () -> Unit
) {
    val productsList by viewModel.products.collectAsState()
    val product = productId?.let { pId -> productsList.find { it.id == pId } }

    // Product type: 0=Simple, 1=Variable, 2=External
    var productType by remember { mutableStateOf(
        when {
            product?.externalUrl?.isNotBlank() == true -> 2
            (product?.colors?.isNotEmpty() == true) || (product?.sizes?.isNotEmpty() == true) -> 1
            else -> 0
        }
    ) }

    // Basic info
    var name by remember { mutableStateOf(product?.name ?: "") }
    var sku by remember { mutableStateOf(product?.sku ?: "") }
    var shortDesc by remember { mutableStateOf(product?.shortDescription ?: "") }
    var desc by remember { mutableStateOf(product?.description ?: "") }

    // Categories & Tags as chip lists
    var categoryInput by remember { mutableStateOf("") }
    var categoryList by remember { mutableStateOf(
        product?.categories?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    ) }
    var tagInput by remember { mutableStateOf("") }
    var tagList by remember { mutableStateOf(
        product?.tags?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
    ) }

    // Pricing
    var regularPrice by remember { mutableStateOf(product?.regularPrice?.toString() ?: "") }
    var hasSalePrice by remember { mutableStateOf((product?.salePrice ?: 0L) > 0L) }
    var salePrice by remember { mutableStateOf(product?.salePrice?.takeIf { it > 0 }?.toString() ?: "") }

    // Inventory
    var stockQuantity by remember { mutableStateOf(product?.stockQuantity?.toString() ?: "0") }
    var lowThreshold by remember { mutableStateOf(product?.lowStockThreshold?.toString() ?: "3") }
    var warehouseNote by remember { mutableStateOf(product?.warehouseNote ?: "") }

    // Shipping
    var length by remember { mutableStateOf(product?.length?.takeIf { it > 0 }?.toString() ?: "") }
    var width by remember { mutableStateOf(product?.width?.takeIf { it > 0 }?.toString() ?: "") }
    var height by remember { mutableStateOf(product?.height?.takeIf { it > 0 }?.toString() ?: "") }
    var weight by remember { mutableStateOf(product?.weight?.takeIf { it > 0 }?.toString() ?: "") }

    // Toggles
    var isVirtual by remember { mutableStateOf(product?.isVirtual ?: false) }
    var isDownloadable by remember { mutableStateOf(product?.isDownloadable ?: false) }
    var isFeatured by remember { mutableStateOf(product?.isFeatured ?: false) }
    var isPublish by remember { mutableStateOf(product?.status != "draft") }

    // Images
    var mainImageUrl by remember { mutableStateOf(product?.mainImage ?: "") }
    var mainImageUri by remember { mutableStateOf<Uri?>(null) }
    var showMainUrlInput by remember { mutableStateOf(false) }
    var galleryUrls by remember { mutableStateOf<List<String>>(product?.galleryImages ?: emptyList()) }
    var galleryUrlInput by remember { mutableStateOf("") }
    var showGalleryUrlInput by remember { mutableStateOf(false) }

    // Variable attributes
    var attr1Name by remember { mutableStateOf(product?.colorAttributeName ?: "رنگ") }
    var colorInput by remember { mutableStateOf("") }
    var colorList by remember { mutableStateOf<List<String>>(product?.colors ?: emptyList()) }
    var attr2Name by remember { mutableStateOf(product?.sizeAttributeName ?: "سایز") }
    var sizeInput by remember { mutableStateOf("") }
    var sizeList by remember { mutableStateOf<List<String>>(product?.sizes ?: emptyList()) }

    // Per-variant pricing/stock/sku
    val initVariants = product?.variants ?: emptyList()
    var variantPrices by remember { mutableStateOf(initVariants.associate { it.combo to it.regularPrice.toString() }) }
    var variantSaleP by remember { mutableStateOf(initVariants.associate { it.combo to if (it.salePrice > 0) it.salePrice.toString() else "" }) }
    var variantStocks by remember { mutableStateOf(initVariants.associate { it.combo to it.stockQty.toString() }) }
    var variantSkus by remember { mutableStateOf(initVariants.associate { it.combo to it.sku }) }
    var bulkVariantPrice by remember { mutableStateOf("") }
    var bulkVariantStock by remember { mutableStateOf("") }

    // Sale schedule
    var isPromoActive by remember { mutableStateOf(product?.isPromoActive ?: false) }
    var promoStart by remember { mutableStateOf(product?.promoStartJalali ?: "") }
    var promoEnd by remember { mutableStateOf(product?.promoEndJalali ?: "") }

    // Stock policy
    var backorders by remember { mutableStateOf(product?.backorders ?: "no") }
    var soldIndividually by remember { mutableStateOf(product?.soldIndividually ?: false) }
    var minQuantity by remember { mutableStateOf(product?.minQuantity?.toString() ?: "1") }
    var maxQuantity by remember { mutableStateOf(product?.maxQuantity?.takeIf { it > 0 }?.toString() ?: "") }

    // External/Affiliate
    var externalUrl by remember { mutableStateOf(product?.externalUrl ?: "") }
    var buttonText by remember { mutableStateOf(product?.buttonText ?: "") }

    // Purchase note & SEO
    var purchaseNote by remember { mutableStateOf(product?.purchaseNote ?: "") }
    var seoTitle by remember { mutableStateOf(product?.seoTitle ?: "") }
    var seoDescription by remember { mutableStateOf(product?.seoDescription ?: "") }

    // Linked products
    var upsellsInput by remember { mutableStateOf(product?.linkedUpsells ?: "") }
    var crossSellsInput by remember { mutableStateOf(product?.linkedCrossSells ?: "") }

    val context = LocalContext.current
    val cachedCategories by viewModel.categories.collectAsState()
    val displayMainImage: Any? = mainImageUri ?: mainImageUrl.takeIf { it.isNotBlank() }

    val mainImagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { mainImageUri = it; mainImageUrl = it.toString(); showMainUrlInput = false }
    }
    val galleryPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { galleryUrls = galleryUrls + it.toString() }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (product != null) "ویرایش محصول" else "محصول جدید",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                Toast.makeText(context, "نام محصول الزامی است", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (regularPrice.toLongOrNull() == null) {
                                Toast.makeText(context, "قیمت معتبر وارد کنید", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            val finalProduct = WooProduct(
                                id = product?.id ?: System.currentTimeMillis(),
                                name = name.trim(),
                                slug = name.trim().lowercase().replace(" ", "-"),
                                shortDescription = shortDesc,
                                description = desc,
                                regularPrice = regularPrice.toLongOrNull() ?: 0,
                                salePrice = if (hasSalePrice) salePrice.toLongOrNull() ?: 0 else 0,
                                isPromoActive = hasSalePrice && isPromoActive,
                                promoStartJalali = if (hasSalePrice && isPromoActive) promoStart else "",
                                promoEndJalali = if (hasSalePrice && isPromoActive) promoEnd else "",
                                sku = sku,
                                manageStock = true,
                                stockQuantity = stockQuantity.toIntOrNull() ?: 0,
                                inStock = (stockQuantity.toIntOrNull() ?: 0) > 0,
                                lowStockThreshold = lowThreshold.toIntOrNull() ?: 3,
                                backorders = backorders,
                                soldIndividually = soldIndividually,
                                minQuantity = minQuantity.toIntOrNull() ?: 1,
                                maxQuantity = maxQuantity.toIntOrNull() ?: 0,
                                weight = weight.toDoubleOrNull() ?: 0.0,
                                length = length.toDoubleOrNull() ?: 0.0,
                                width = width.toDoubleOrNull() ?: 0.0,
                                height = height.toDoubleOrNull() ?: 0.0,
                                categories = categoryList.joinToString(", "),
                                tags = tagList.joinToString(", "),
                                mainImage = mainImageUrl,
                                galleryImages = galleryUrls,
                                status = if (isPublish) "publish" else "draft",
                                isVirtual = isVirtual,
                                isDownloadable = isDownloadable,
                                isFeatured = isFeatured,
                                colors = colorList,
                                sizes = sizeList,
                                colorAttributeName = attr1Name,
                                sizeAttributeName = attr2Name,
                                variants = if (productType == 1) {
                                    val combos = when {
                                        colorList.isNotEmpty() && sizeList.isNotEmpty() ->
                                            colorList.flatMap { c -> sizeList.map { s -> "$c | $s" } }
                                        colorList.isNotEmpty() -> colorList
                                        sizeList.isNotEmpty() -> sizeList
                                        else -> emptyList()
                                    }
                                    combos.map { combo ->
                                        com.sahand.wooadmin.data.ProductVariant(
                                            combo = combo,
                                            regularPrice = variantPrices[combo]?.toLongOrNull() ?: (regularPrice.toLongOrNull() ?: 0),
                                            salePrice = variantSaleP[combo]?.toLongOrNull() ?: 0,
                                            stockQty = variantStocks[combo]?.toIntOrNull() ?: 0,
                                            sku = variantSkus[combo] ?: ""
                                        )
                                    }
                                } else emptyList(),
                                warehouseNote = warehouseNote,
                                externalUrl = if (productType == 2) externalUrl else "",
                                buttonText = if (productType == 2) buttonText else "",
                                purchaseNote = purchaseNote,
                                seoTitle = seoTitle,
                                seoDescription = seoDescription,
                                linkedUpsells = upsellsInput.trim(),
                                linkedCrossSells = crossSellsInput.trim()
                            )
                            if (product != null) {
                                viewModel.editProduct(finalProduct)
                                Toast.makeText(context, "تغییرات ذخیره شد", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.addProduct(finalProduct)
                                Toast.makeText(context, "محصول اضافه شد", Toast.LENGTH_SHORT).show()
                            }
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(if (product != null) Icons.Default.Save else Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (product != null) "ذخیره تغییرات" else "انتشار محصول",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {

                // ── PRODUCT TYPE SELECTOR ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("محصول ساده" to 0, "محصول متغیر" to 1, "خارجی/پورسانتی" to 2).forEach { (label, idx) ->
                        val selected = productType == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { productType = idx }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                label,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // ── IMAGES ──
                ProductSectionHeader("تصاویر محصول", Icons.Default.PhotoLibrary)

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Main image
                    Text("تصویر اصلی", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        if (displayMainImage != null) {
                            AsyncImage(
                                model = displayMainImage,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(52.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.38f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            TextButton(
                                onClick = { mainImagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("گالری", fontSize = 12.sp)
                            }
                            TextButton(
                                onClick = { showMainUrlInput = !showMainUrlInput },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Link, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("URL", fontSize = 12.sp)
                            }
                            if (displayMainImage != null) {
                                TextButton(
                                    onClick = { mainImageUrl = ""; mainImageUri = null },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
                                ) {
                                    Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(15.dp))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = showMainUrlInput) {
                        OutlinedTextField(
                            value = mainImageUrl,
                            onValueChange = { mainImageUrl = it; mainImageUri = null },
                            label = { Text("URL تصویر اصلی") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    // Gallery
                    Text("گالری تصاویر", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(galleryUrls) { index, url ->
                            Box(modifier = Modifier.size(80.dp)) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .clickable { galleryUrls = galleryUrls.filterIndexed { i, _ -> i != index } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { galleryPickerLauncher.launch("image/*") },
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                Text("گالری", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { showGalleryUrlInput = !showGalleryUrlInput },
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Link, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
                                Text("URL", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }

                    AnimatedVisibility(visible = showGalleryUrlInput) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = galleryUrlInput,
                                onValueChange = { galleryUrlInput = it },
                                label = { Text("URL تصویر گالری") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    if (galleryUrlInput.isNotBlank()) {
                                        galleryUrls = galleryUrls + galleryUrlInput.trim()
                                        galleryUrlInput = ""
                                        showGalleryUrlInput = false
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            ) {
                                Icon(Icons.Default.Add, null, tint = Color.White)
                            }
                        }
                    }
                }

                // ── BASIC INFO ──
                ProductSectionHeader("اطلاعات اصلی", Icons.Default.Edit)

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("نام محصول *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("کد SKU / کد انبار") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(18.dp)) }
                    )
                    OutlinedTextField(
                        value = shortDesc,
                        onValueChange = { shortDesc = it },
                        label = { Text("توضیح کوتاه") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // ── CATEGORIES ──
                ProductSectionHeader("دسته‌بندی", Icons.Default.Category)

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = categoryInput,
                            onValueChange = { categoryInput = it },
                            label = { Text("نام دسته‌بندی") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        IconButton(
                            onClick = {
                                val v = categoryInput.trim()
                                if (v.isNotBlank() && !categoryList.contains(v)) {
                                    categoryList = categoryList + v
                                    categoryInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }
                    // Autocomplete suggestions from WooCommerce categories
                    val catSuggestions = remember(categoryInput, cachedCategories) {
                        if (categoryInput.length < 1) emptyList()
                        else cachedCategories.filter {
                            it.name.contains(categoryInput, ignoreCase = true) && !categoryList.contains(it.name)
                        }.take(6)
                    }
                    AnimatedVisibility(visible = catSuggestions.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(0.dp, 0.dp, 10.dp, 10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            catSuggestions.forEachIndexed { idx, cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            categoryList = categoryList + cat.name
                                            categoryInput = ""
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat.name, fontSize = 13.sp)
                                    if (cat.count > 0) {
                                        Text("${cat.count} محصول", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                if (idx < catSuggestions.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            }
                        }
                    }
                    if (categoryList.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categoryList.forEach { cat ->
                                InputChip(
                                    selected = false,
                                    onClick = {},
                                    label = { Text(cat, fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { categoryList = categoryList.filter { it != cat } }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // ── TAGS ──
                ProductSectionHeader("برچسب‌ها", Icons.Default.LocalOffer)

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tagInput,
                            onValueChange = { tagInput = it },
                            label = { Text("برچسب جدید") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(16.dp)) }
                        )
                        IconButton(
                            onClick = {
                                val v = tagInput.trim()
                                if (v.isNotBlank() && !tagList.contains(v)) {
                                    tagList = tagList + v
                                    tagInput = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }
                    if (tagList.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            tagList.forEach { tag ->
                                InputChip(
                                    selected = false,
                                    onClick = {},
                                    label = { Text(tag, fontSize = 12.sp) },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.Close,
                                            null,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { tagList = tagList.filter { it != tag } }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                // ── PRICING ──
                ProductSectionHeader("قیمت‌گذاری", Icons.Default.AttachMoney)

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = regularPrice,
                        onValueChange = { regularPrice = it.filter { c -> c.isDigit() } },
                        label = { Text("قیمت اصلی (تومان) *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.AttachMoney, null, modifier = Modifier.size(18.dp)) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("قیمت حراجی (تخفیف)", fontSize = 13.sp)
                        Switch(checked = hasSalePrice, onCheckedChange = { hasSalePrice = it })
                    }
                    AnimatedVisibility(visible = hasSalePrice) {
                        OutlinedTextField(
                            value = salePrice,
                            onValueChange = { salePrice = it.filter { c -> c.isDigit() } },
                            label = { Text("قیمت با تخفیف (تومان)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.LocalOffer, null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }

                // ── SALE SCHEDULE ──
                AnimatedVisibility(visible = hasSalePrice) {
                    Column {
                        ProductSectionHeader("زمان‌بندی حراج", Icons.Default.Schedule)
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("تعیین بازه زمانی حراج", fontSize = 13.sp)
                                Switch(checked = isPromoActive, onCheckedChange = { isPromoActive = it })
                            }
                            AnimatedVisibility(visible = isPromoActive) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = promoStart,
                                        onValueChange = { promoStart = it },
                                        label = { Text("از تاریخ (شمسی)", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) }
                                    )
                                    OutlinedTextField(
                                        value = promoEnd,
                                        onValueChange = { promoEnd = it },
                                        label = { Text("تا تاریخ (شمسی)", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp)) }
                                    )
                                }
                            }
                        }
                    }
                }

                // ── INVENTORY ──
                ProductSectionHeader("موجودی انبار", Icons.Default.Inventory)

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = stockQuantity,
                            onValueChange = { stockQuantity = it.filter { c -> c.isDigit() } },
                            label = { Text("موجودی فعلی") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = lowThreshold,
                            onValueChange = { lowThreshold = it.filter { c -> c.isDigit() } },
                            label = { Text("حد هشدار کمبود") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    OutlinedTextField(
                        value = warehouseNote,
                        onValueChange = { warehouseNote = it },
                        label = { Text("محل انبار (مثلاً قفسه B ردیف ۳)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp)) }
                    )
                }

                // ── STOCK POLICY ──
                ProductSectionHeader("سیاست انبار", Icons.Default.Policy)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("سفارش در صورت اتمام موجودی", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    listOf("no" to "غیرمجاز", "notify" to "با اطلاع‌رسانی", "yes" to "مجاز").forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (backorders == value) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent)
                                .clickable { backorders = value }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(selected = backorders == value, onClick = { backorders = value })
                            Text(label, fontSize = 13.sp)
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("فروش به صورت تکی (بدون امکان چندتایی)", fontSize = 13.sp)
                        Switch(checked = soldIndividually, onCheckedChange = { soldIndividually = it })
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = minQuantity,
                            onValueChange = { minQuantity = it.filter { c -> c.isDigit() } },
                            label = { Text("حداقل تعداد سفارش") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxQuantity,
                            onValueChange = { maxQuantity = it.filter { c -> c.isDigit() } },
                            label = { Text("حداکثر (خالی=نامحدود)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }

                // ── SHIPPING DIMENSIONS (hidden for virtual) ──
                AnimatedVisibility(visible = !isVirtual) {
                    Column {
                        ProductSectionHeader("ابعاد و وزن (ارسال)", Icons.Default.LocalShipping)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = length,
                                onValueChange = { length = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("طول cm", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = width,
                                onValueChange = { width = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("عرض cm", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = height,
                                onValueChange = { height = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("ارتفاع cm", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = weight,
                                onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                                label = { Text("وزن g", fontSize = 10.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )
                        }
                    }
                }

                // ── PRODUCT OPTIONS ──
                ProductSectionHeader("ویژگی‌ها و وضعیت", Icons.Default.Settings)

                Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                    listOf(
                        Pair(isVirtual, "محصول مجازی (بدون ارسال فیزیکی)"),
                        Pair(isDownloadable, "محصول دانلودی"),
                        Pair(isFeatured, "محصول ویژه (Featured)"),
                        Pair(isPublish, "انتشار فوری")
                    ).forEachIndexed { idx, (checked, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, fontSize = 13.sp)
                            Switch(
                                checked = checked,
                                onCheckedChange = { v ->
                                    when (idx) {
                                        0 -> isVirtual = v
                                        1 -> isDownloadable = v
                                        2 -> isFeatured = v
                                        3 -> isPublish = v
                                    }
                                }
                            )
                        }
                    }
                }

                // ── VARIABLE ATTRIBUTES ──
                AnimatedVisibility(visible = productType == 1) {
                    Column {
                        ProductSectionHeader("تعریف ویژگی‌ها", Icons.Default.Tune)

                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Attribute 1 (color-type)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = attr1Name,
                                    onValueChange = { attr1Name = it },
                                    label = { Text("نام ویژگی ۱", fontSize = 11.sp) },
                                    modifier = Modifier.width(110.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = colorInput,
                                    onValueChange = { colorInput = it },
                                    label = { Text("مقدار جدید $attr1Name") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        val v = colorInput.trim()
                                        if (v.isNotBlank() && !colorList.contains(v)) {
                                            colorList = colorList + v; colorInput = ""
                                        }
                                    },
                                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                ) { Icon(Icons.Default.Add, null, tint = Color.White) }
                            }
                            if (colorList.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    colorList.forEach { c ->
                                        InputChip(
                                            selected = false, onClick = {},
                                            label = { Text(c, fontSize = 12.sp) },
                                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp).clickable { colorList = colorList.filter { it != c } }) }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Attribute 2 (size-type) — optional
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = attr2Name,
                                    onValueChange = { attr2Name = it },
                                    label = { Text("نام ویژگی ۲", fontSize = 11.sp) },
                                    modifier = Modifier.width(110.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = sizeInput,
                                    onValueChange = { sizeInput = it },
                                    label = { Text("مقدار جدید $attr2Name (اختیاری)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        val v = sizeInput.trim()
                                        if (v.isNotBlank() && !sizeList.contains(v)) {
                                            sizeList = sizeList + v; sizeInput = ""
                                        }
                                    },
                                    modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                ) { Icon(Icons.Default.Add, null, tint = Color.White) }
                            }
                            if (sizeList.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    sizeList.forEach { s ->
                                        InputChip(
                                            selected = false, onClick = {},
                                            label = { Text(s, fontSize = 12.sp) },
                                            trailingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp).clickable { sizeList = sizeList.filter { it != s } }) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── VARIANT MATRIX (price/stock per variant) ──
                val combos = if (productType == 1) {
                    when {
                        colorList.isNotEmpty() && sizeList.isNotEmpty() ->
                            colorList.flatMap { c -> sizeList.map { s -> "$c | $s" } }
                        colorList.isNotEmpty() -> colorList
                        sizeList.isNotEmpty() -> sizeList
                        else -> emptyList()
                    }
                } else emptyList()

                AnimatedVisibility(visible = combos.isNotEmpty()) {
                    Column {
                        ProductSectionHeader("جدول تنوع‌ها (${combos.size} ترکیب)", Icons.Default.TableRows)

                        // Bulk fill row
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = bulkVariantPrice,
                                onValueChange = { bulkVariantPrice = it.filter { c -> c.isDigit() } },
                                label = { Text("قیمت یکسان همه", fontSize = 11.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = bulkVariantStock,
                                onValueChange = { bulkVariantStock = it.filter { c -> c.isDigit() } },
                                label = { Text("موجودی یکسان", fontSize = 11.sp) },
                                modifier = Modifier.weight(0.8f),
                                shape = RoundedCornerShape(10.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    if (bulkVariantPrice.isNotBlank()) variantPrices = combos.associate { it to bulkVariantPrice }
                                    if (bulkVariantStock.isNotBlank()) variantStocks = combos.associate { it to bulkVariantStock }
                                },
                                modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                            ) { Icon(Icons.Default.DoneAll, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                        }

                        Spacer(Modifier.height(4.dp))

                        // Per-variant rows
                        combos.forEach { combo ->
                            VariantRow(
                                combo = combo,
                                price = variantPrices[combo] ?: "",
                                salePrice = variantSaleP[combo] ?: "",
                                stock = variantStocks[combo] ?: "0",
                                sku = variantSkus[combo] ?: "",
                                onPriceChange = { variantPrices = variantPrices + (combo to it) },
                                onSalePriceChange = { variantSaleP = variantSaleP + (combo to it) },
                                onStockChange = { variantStocks = variantStocks + (combo to it) },
                                onSkuChange = { variantSkus = variantSkus + (combo to it) }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                // ── EXTERNAL/AFFILIATE ──
                AnimatedVisibility(visible = productType == 2) {
                    Column {
                        ProductSectionHeader("محصول خارجی / پورسانتی", Icons.Default.OpenInNew)
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = externalUrl,
                                onValueChange = { externalUrl = it },
                                label = { Text("URL محصول خارجی") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Link, null, modifier = Modifier.size(18.dp)) }
                            )
                            OutlinedTextField(
                                value = buttonText,
                                onValueChange = { buttonText = it },
                                label = { Text("متن دکمه خرید (مثلاً: خرید از سایت)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.TouchApp, null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
                }

                // ── FULL DESCRIPTION ──
                ProductSectionHeader("توضیحات کامل", Icons.Default.Description)

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("توضیحات محصول") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    minLines = 4,
                    maxLines = 12,
                    shape = RoundedCornerShape(10.dp)
                )

                // ── PURCHASE NOTE ──
                ProductSectionHeader("یادداشت پس از خرید", Icons.Default.StickyNote2)
                OutlinedTextField(
                    value = purchaseNote,
                    onValueChange = { purchaseNote = it },
                    label = { Text("پیام به مشتری پس از خرید") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp)
                )

                // ── SEO ──
                ProductSectionHeader("سئو و اسلاگ", Icons.Default.TravelExplore)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = seoTitle,
                        onValueChange = { seoTitle = it },
                        label = { Text("عنوان سئو (SEO Title)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Title, null, modifier = Modifier.size(18.dp)) },
                        supportingText = { Text("${seoTitle.length}/60", fontSize = 10.sp) }
                    )
                    OutlinedTextField(
                        value = seoDescription,
                        onValueChange = { seoDescription = it },
                        label = { Text("توضیح متا (Meta Description)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        minLines = 2,
                        maxLines = 3,
                        supportingText = { Text("${seoDescription.length}/160", fontSize = 10.sp) }
                    )
                }

                // ── LINKED PRODUCTS ──
                ProductSectionHeader("محصولات مرتبط", Icons.Default.Link)
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = upsellsInput,
                        onValueChange = { upsellsInput = it },
                        label = { Text("Upsells (شناسه‌ها با کاما جدا شوند)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.TrendingUp, null, modifier = Modifier.size(18.dp)) },
                        supportingText = { Text("مثال: 12, 34, 56", fontSize = 10.sp) }
                    )
                    OutlinedTextField(
                        value = crossSellsInput,
                        onValueChange = { crossSellsInput = it },
                        label = { Text("Cross-sells (شناسه‌ها با کاما)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.SwapHoriz, null, modifier = Modifier.size(18.dp)) },
                        supportingText = { Text("مثال: 78, 90", fontSize = 10.sp) }
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun VariantRow(
    combo: String,
    price: String,
    salePrice: String,
    stock: String,
    sku: String,
    onPriceChange: (String) -> Unit,
    onSalePriceChange: (String) -> Unit,
    onStockChange: (String) -> Unit,
    onSkuChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    combo,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(90.dp),
                    maxLines = 2
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { onPriceChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("قیمت", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
                OutlinedTextField(
                    value = stock,
                    onValueChange = { onStockChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("موجودی", fontSize = 10.sp) },
                    modifier = Modifier.weight(0.75f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(8.dp)
                )
                IconButton(onClick = { expanded = !expanded }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, modifier = Modifier.size(18.dp)
                    )
                }
            }
            AnimatedVisibility(visible = expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = salePrice,
                        onValueChange = { onSalePriceChange(it.filter { c -> c.isDigit() }) },
                        label = { Text("قیمت حراجی", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = sku,
                        onValueChange = onSkuChange,
                        label = { Text("SKU", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    }
}
