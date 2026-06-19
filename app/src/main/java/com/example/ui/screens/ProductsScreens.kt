package com.example.ui.screens

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
import com.example.core.utils.Helpers
import com.example.data.WooProduct
import com.example.ui.theme.GreenMoney
import com.example.ui.theme.RedError
import com.example.ui.theme.YellowWarn
import com.example.ui.viewmodel.WooViewModel

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

    var showFilters by remember { mutableStateOf(false) }

    var showQuickStockDialog by remember { mutableStateOf<WooProduct?>(null) }
    var stockInput by remember { mutableStateOf("") }

    var showQuickPriceDialog by remember { mutableStateOf<WooProduct?>(null) }
    var regularPriceInput by remember { mutableStateOf("") }
    var salePriceInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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

                // Advanced Filter icon button
                IconButton(
                    onClick = { showFilters = !showFilters },
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            if (showFilters || activeCategory != "ALL" || filterType != "ALL") 
                                MaterialTheme.colorScheme.primaryContainer 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (showFilters || activeCategory != "ALL" || filterType != "ALL") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "فیلتر پیشرفته",
                        tint = if (showFilters || activeCategory != "ALL" || filterType != "ALL") 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Inventory management shortcut
                IconButton(
                    onClick = onNavigateToInventory,
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(12.dp))
                ) {
                    Icon(imageVector = Icons.Default.Inventory2, contentDescription = "مدیریت انبار", tint = Color.White)
                }

                // Add FAB shortcut
                IconButton(
                    onClick = onNavigateToAdd,
                    modifier = Modifier
                        .size(52.dp)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "کالای جدید", tint = Color.White)
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
            if (productsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ProductionQuantityLimits,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("کالایی یافت نشد!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
                            }
                        )
                    }
                }
            }
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
    onStockChange: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (product.salePrice > 0) {
                        Text(
                            text = Helpers.formatPrice(product.salePrice),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = GreenMoney
                        )
                        Text(
                            text = Helpers.formatPrice(product.regularPrice),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.offset(y = 1.dp),
                            style = androidx.compose.ui.text.TextStyle(
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            )
                        )
                        
                        // Small computed discount badge
                        val pct = ((product.regularPrice - product.salePrice).toDouble() * 100 / product.regularPrice).toInt()
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
                            text = Helpers.formatPrice(product.regularPrice),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Tiny dot
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(stockColor)
                        )
                        Text(
                            text = stockText,
                            color = stockColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (product.warehouseNote.isNotBlank()) {
                            Text(
                                text = "• ${product.warehouseNote}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Miniature linear resource-meter
                    val progressFraction = remember(product.stockQuantity) {
                        if (product.stockQuantity == 0) 0f
                        else if (product.stockQuantity <= product.lowStockThreshold) 0.35f
                        else 1.0f
                    }
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = stockColor,
                        trackColor = stockColor.copy(alpha = 0.12f)
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

    // Product type: 0=Simple, 1=Variable
    var productType by remember { mutableStateOf(if ((product?.colors?.isNotEmpty() == true) || (product?.sizes?.isNotEmpty() == true)) 1 else 0) }

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
    var colorInput by remember { mutableStateOf("") }
    var colorList by remember { mutableStateOf<List<String>>(product?.colors ?: emptyList()) }
    var sizeInput by remember { mutableStateOf("") }
    var sizeList by remember { mutableStateOf<List<String>>(product?.sizes ?: emptyList()) }

    val context = LocalContext.current
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
                                slug = name.trim().replace(" ", "-"),
                                shortDescription = shortDesc,
                                description = desc,
                                regularPrice = regularPrice.toLongOrNull() ?: 0,
                                salePrice = if (hasSalePrice) salePrice.toLongOrNull() ?: 0 else 0,
                                sku = sku,
                                manageStock = true,
                                stockQuantity = stockQuantity.toIntOrNull() ?: 0,
                                inStock = (stockQuantity.toIntOrNull() ?: 0) > 0,
                                lowStockThreshold = lowThreshold.toIntOrNull() ?: 3,
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
                                warehouseNote = warehouseNote
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
                    listOf("محصول ساده" to 0, "محصول متغیر" to 1).forEach { (label, idx) ->
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
                        ProductSectionHeader("ویژگی‌ها و تنوع", Icons.Default.Tune)

                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Colors attribute
                            Text("رنگ‌ها", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = colorInput,
                                    onValueChange = { colorInput = it },
                                    label = { Text("رنگ جدید (مثلاً: قرمز)") },
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
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.White)
                                }
                            }
                            if (colorList.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    colorList.forEach { c ->
                                        InputChip(
                                            selected = false,
                                            onClick = {},
                                            label = { Text(c, fontSize = 12.sp) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { colorList = colorList.filter { it != c } }
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            HorizontalDivider()

                            // Sizes attribute
                            Text("سایزها", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = sizeInput,
                                    onValueChange = { sizeInput = it },
                                    label = { Text("سایز جدید (مثلاً: XL)") },
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
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                ) {
                                    Icon(Icons.Default.Add, null, tint = Color.White)
                                }
                            }
                            if (sizeList.isNotEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    sizeList.forEach { s ->
                                        InputChip(
                                            selected = false,
                                            onClick = {},
                                            label = { Text(s, fontSize = 12.sp) },
                                            trailingIcon = {
                                                Icon(
                                                    Icons.Default.Close,
                                                    null,
                                                    modifier = Modifier
                                                        .size(14.dp)
                                                        .clickable { sizeList = sizeList.filter { it != s } }
                                                )
                                            }
                                        )
                                    }
                                }
                            }

                            if (colorList.isNotEmpty() || sizeList.isNotEmpty()) {
                                val combinations = when {
                                    colorList.isNotEmpty() && sizeList.isNotEmpty() -> colorList.size * sizeList.size
                                    else -> colorList.size + sizeList.size
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "$combinations ترکیب (variant) ایجاد خواهد شد",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
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

                Spacer(Modifier.height(24.dp))
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
