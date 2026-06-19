package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.Helpers
import com.example.data.CustomerCategory
import com.example.data.WooCustomer
import com.example.ui.theme.GreenMoney
import com.example.ui.viewmodel.WooViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: WooViewModel,
    onNavigateToDetails: (Long) -> Unit
) {
    val customersList by viewModel.filteredCustomers.collectAsState(initial = emptyList())
    val searchInput by viewModel.customerSearchQuery.collectAsState()
    val activeFilter by viewModel.customerCategoryFilter.collectAsState()

    // Retrieve all customers for bulk SMS calculations
    val allCustomersList by viewModel.customers.collectAsState(initial = emptyList())

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // SMS wizard state management
    var showQuickSmsDialog by remember { mutableStateOf(false) }
    var smsTargetType by remember { mutableStateOf("CUSTOM") } // "CUSTOM" or "BULK"
    var customPhoneInput by remember { mutableStateOf("") }
    var bulkTargetCategory by remember { mutableStateOf("ALL") } // "ALL", "ROYAL", "VIP", etc.
    var smsBodyText by remember { mutableStateOf("مشتری عزیز، از همراهی صمیمانه شما با ما بر روی فروشگاه سپاسگزاریم.") }
    var isSendingQuickSms by remember { mutableStateOf(false) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showQuickSmsDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("fab_quick_sms")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = "ارسال پیامک آزاد و گروهی",
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "ارسال پیامک عمومی",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { viewModel.updateCustomerSearch(it) },
                    placeholder = { Text("جستجوی نام مشتری یا شماره تماس...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .testTag("customer_search_bar"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Category Tab triggers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterTabItem("همه مشتریان", activeFilter == "ALL") { viewModel.setCustomerFilter("ALL") }
                    CustomerCategory.values().forEach { cat ->
                        FilterTabItem(cat.persianLabel, activeFilter == cat.name) { viewModel.setCustomerFilter(cat.name) }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Body list
                if (customersList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("مشتری یافت نشد!", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(customersList, key = { it.id }) { customer ->
                            CustomerItemCard(customer = customer, onClick = { onNavigateToDetails(customer.id) })
                        }
                    }
                }
            }
        }

        // --- CUSTOM MULTI-FUNCTIONAL SMS WIZARD DIALOG ---
        if (showQuickSmsDialog) {
            val repo = viewModel.repository
            val isMeliConfigured = repo.melipayamakUsername.isNotBlank() && repo.melipayamakPassword.isNotBlank()

            AlertDialog(
                onDismissRequest = { if (!isSendingQuickSms) showQuickSmsDialog = false },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContactMail,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "ارسال پیامک ملی‌پیامک (سریع / گروهی)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Switch Target Type (Custom Number vs Bulk Customers)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (smsTargetType == "CUSTOM") MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { smsTargetType = "CUSTOM" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "شماره دلخواه",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (smsTargetType == "CUSTOM") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (smsTargetType == "BULK") MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { smsTargetType = "BULK" }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ارسال به مشتریان (گروهی)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (smsTargetType == "BULK") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Target Configuration Panel
                        if (smsTargetType == "CUSTOM") {
                            OutlinedTextField(
                                value = customPhoneInput,
                                onValueChange = { customPhoneInput = it },
                                label = { Text("شماره همراه گیرنده") },
                                placeholder = { Text("مثال: 09123456789") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = { Icon(Icons.Default.PhoneIphone, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                singleLine = true,
                                enabled = !isSendingQuickSms
                            )
                        } else {
                            // Bulk targets selection
                            Text(
                                text = "انتخاب گروه مخاطبین:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            // Horizontally scrollable recipient category filters
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val allCount = allCustomersList.size
                                // option 1: همه مشتریان
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (bulkTargetCategory == "ALL") MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                        )
                                        .clickable { bulkTargetCategory = "ALL" }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "همه مشتریان (${Helpers.toPersianDigits(allCount.toString())})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (bulkTargetCategory == "ALL") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // other categories
                                CustomerCategory.values().forEach { cat ->
                                    val catCount = allCustomersList.count { it.category == cat.name }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (bulkTargetCategory == cat.name) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .clickable { bulkTargetCategory = cat.name }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "${cat.persianLabel} (${Helpers.toPersianDigits(catCount.toString())})",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (bulkTargetCategory == cat.name) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            val matchedRecipientsCount = if (bulkTargetCategory == "ALL") {
                                allCustomersList.size
                            } else {
                                allCustomersList.count { it.category == bulkTargetCategory }
                            }

                            Text(
                                text = "پیامک به صورت شخصی‌سازی شده برای ${Helpers.toPersianDigits(matchedRecipientsCount.toString())} مشتری ارسال می‌شود. مقدار {name} با نام مشتری جایگزین می‌گردد.",
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                                    .padding(8.dp)
                            )
                        }

                        // Text Templates
                        Text(
                            text = "قالب‌های آماده پیامک:",
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
                            // Temp 1: جشنواره تخفیف
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                                    .clickable {
                                        smsBodyText = if (smsTargetType == "BULK") {
                                            "سلام {name} عزیز، جشنواره بزرگ تخفیفی فروشگاه ما آغاز شد! خرید با کد تخفیف یلدا روی کل سایت."
                                        } else {
                                            "جشنواره تخفیف‌های طلایی با تا ۵۰٪ تخفیف شگفت‌انگیز شروع شد! همین حالا خرید کنید."
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("جشنواره تخفیفات", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                            }

                            // Temp 2: تبریک مناسبتی
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                                    .clickable {
                                        smsBodyText = if (smsTargetType == "BULK") {
                                            "همراه گرامی {name}، عید سعید فطر را به شما صمیمانه تبریک عرض می‌کنیم. کد تخفیف عیدانه ما: EID95"
                                        } else {
                                            "دوست گرامی، عید سعید و خجسته فطر بر شما مبارک باد. با آرزوی اوقاتی خوش."
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("تبریک و آفر مناسبتی", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                            }

                            // Temp 3: نظرسنجی
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                                    .clickable {
                                        smsBodyText = if (smsTargetType == "BULK") {
                                            "سلام {name} عزیز، نظر شما درباره کیفیت محصولات ما چیست؟ با پاسخ به این پیام ما را یاری دهید."
                                        } else {
                                            "مشتری گرامی، از تجربه خرید خود از ما راضی بوده‌اید؟ نظرات ارزشمند شما کیفیت کار ما را ارتقا می‌دهد."
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("نظرسنجی کیفیت", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedTextField(
                            value = smsBodyText,
                            onValueChange = { smsBodyText = it },
                            label = { Text("متن پیام ارسال شونده") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 6,
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isSendingQuickSms
                        )

                        // Char counter & estimate page count
                        val cLen = smsBodyText.length
                        val sPrts = if (cLen <= 70) 1 else (cLen + 66) / 67
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تعداد کاراکتر: ${Helpers.toPersianDigits(cLen.toString())}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "تعداد بخش پیامک: ${Helpers.toPersianDigits(sPrts.toString())} پیام",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Configuration warning banner
                        if (!isMeliConfigured) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "مشخصات وب‌سرویس ملی‌پیامک در تنظیمات تکمیل نیست. ارسال پیام به صورت شبیه‌ساز انجام خواهد شد. جهت اتصال واقعی، پنل خود را در تنظیمات وارد کنید.",
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (smsTargetType == "CUSTOM" && customPhoneInput.isBlank()) {
                                Toast.makeText(context, "لطفاً شماره تماس را وارد کنید.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (smsBodyText.isBlank()) {
                                Toast.makeText(context, "متن پیامک نباید خالی باشد.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSendingQuickSms = true
                            coroutineScope.launch {
                                if (smsTargetType == "CUSTOM") {
                                    // Single customization
                                    val result = viewModel.sendSmsToCustomer(customPhoneInput, smsBodyText)
                                    isSendingQuickSms = false
                                    when (result) {
                                        is com.example.core.network.SmsResult.Success -> {
                                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                            showQuickSmsDialog = false
                                        }
                                        is com.example.core.network.SmsResult.Error -> {
                                            Toast.makeText(context, result.errorMessage, Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    // Bulk customization loop
                                    val targets = if (bulkTargetCategory == "ALL") {
                                        allCustomersList
                                    } else {
                                        allCustomersList.filter { it.category == bulkTargetCategory }
                                    }

                                    if (targets.isEmpty()) {
                                        Toast.makeText(context, "هیچ مشتری فعالی در این دسته‌بندی یافت نشد.", Toast.LENGTH_LONG).show()
                                        isSendingQuickSms = false
                                        return@launch
                                    }

                                    var successCount = 0
                                    var errorMsg = ""

                                    targets.forEach { targetCustomer ->
                                        val personalizedMsg = smsBodyText.replace("{name}", "${targetCustomer.firstName} ${targetCustomer.lastName}".trim())
                                        val result = viewModel.sendSmsToCustomer(targetCustomer.phone, personalizedMsg)
                                        if (result is com.example.core.network.SmsResult.Success) {
                                            successCount++
                                        } else if (result is com.example.core.network.SmsResult.Error) {
                                            errorMsg = result.errorMessage
                                        }
                                    }

                                    isSendingQuickSms = false
                                    if (successCount > 0) {
                                        Toast.makeText(context, "تعداد ${Helpers.toPersianDigits(successCount.toString())} پیامک با موفقیت از طریق فرستنده ملی‌پیامک ارسال شد.", Toast.LENGTH_LONG).show()
                                        showQuickSmsDialog = false
                                    } else {
                                        Toast.makeText(context, "ارسال پیامک‌های گروهی ناموفق بود: $errorMsg", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("submit_quick_sms_button"),
                        enabled = !isSendingQuickSms,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSendingQuickSms) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("در حال عملیات...", fontSize = 11.sp)
                        } else {
                            Text(
                                text = if (smsTargetType == "CUSTOM") "ارسال پیامک آزاد" else "ارسال پیامک گروهی",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (smsTargetType == "CUSTOM" && customPhoneInput.isNotBlank()) {
                            // Mobile fallback options
                            OutlinedButton(
                                onClick = {
                                    val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$customPhoneInput")).apply {
                                        putExtra("sms_body", smsBodyText)
                                    }
                                    context.startActivity(smsIntent)
                                    showQuickSmsDialog = false
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenMoney),
                                border = BorderStroke(1.dp, GreenMoney.copy(alpha = 0.5f))
                            ) {
                                Text("باکارت گوشی", fontSize = 10.sp)
                            }
                        }

                        TextButton(
                            onClick = { showQuickSmsDialog = false },
                            enabled = !isSendingQuickSms
                        ) {
                            Text("انصراف", fontSize = 11.sp)
                        }
                    }
                }
            )
        }
    }
}


@Composable
fun CustomerItemCard(customer: WooCustomer, onClick: () -> Unit) {
    val catEnum = CustomerCategory.values().find { it.name == customer.category } ?: CustomerCategory.REGULAR
    val catColor = Color(android.graphics.Color.parseColor(catEnum.colorHex))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Name
                Text(
                    text = "${customer.firstName} ${customer.lastName}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Category badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(catColor.copy(alpha = 0.1f))
                        .border(1.dp, catColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = catEnum.persianLabel, color = catColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = "تلفن: ${customer.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Text(text = "ایمیل: ${customer.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))

            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("خریدهای موفق", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text("${Helpers.toPersianDigits(customer.ordersCount)} سفارش", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("مجموع پرداخت‌ها", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Text(Helpers.formatPrice(customer.totalSpent), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = GreenMoney)
                }
            }
        }
    }
}

// --- CUSTOMER DETAIL SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailsScreen(
    customerId: Long,
    viewModel: WooViewModel,
    onBack: () -> Unit
) {
    val customersList by viewModel.customers.collectAsState()
    val customer = customersList.find { it.id == customerId }
    val context = LocalContext.current

    var internalNotes by remember { mutableStateOf(customer?.internalNotes ?: "") }

    val coroutineScope = rememberCoroutineScope()
    var showSmsDialog by remember { mutableStateOf(false) }
    var smsText by remember { mutableStateOf("مشتری عزیز ${customer?.firstName ?: ""}، از همراهی شما با ما صمیمانه سپاسگزاریم.") }
    var isSendingSms by remember { mutableStateOf(false) }

    if (customer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("مشتری یافت نشد.")
        }
        return
    }

    val catEnum = CustomerCategory.values().find { it.name == customer.category } ?: CustomerCategory.REGULAR
    val catColor = Color(android.graphics.Color.parseColor(catEnum.colorHex))

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("پروفایل مشتری: ${customer.firstName}") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
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
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // Card info summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${customer.firstName} ${customer.lastName}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(catColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(catEnum.persianLabel, color = catColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("شماره موبایل: ${customer.phone}", fontSize = 13.sp)
                        Text("آدرس ایمیل: ${customer.email}", fontSize = 13.sp)
                        Text("تاریخ عضویت: ${customer.registeredDateJalali}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }

                // Call tools row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Call Phone
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${customer.phone}"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تماس تلفنی", fontSize = 11.sp)
                    }

                    // Send SMS
                    Button(
                        onClick = {
                            showSmsDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenMoney.copy(alpha = 0.1f), contentColor = GreenMoney),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ارسال پیامک", fontSize = 11.sp)
                    }

                    // WhatsApp Chat
                    Button(
                        onClick = {
                            try {
                                val url = "https://api.whatsapp.com/send?phone=${customer.phone.replace("۰", "0").replace("۰", "0")}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "واتساپ روی گوشی نصب نیست.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366).copy(alpha = 0.1f), contentColor = Color(0xFF25D366)),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("واتساپ", fontSize = 11.sp)
                    }
                }

                // Client purchase statistics
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("خلاصه خریدهای انجام‌شده", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                        StatisticRow("تعداد کل فاکتورها", "${Helpers.toPersianDigits(customer.ordersCount)} بار ثبت خرید")
                        StatisticRow("مجموع کل پرداخت به سایت", Helpers.formatPrice(customer.totalSpent))
                        StatisticRow("میانگین مبالغ فاکتورها", Helpers.formatPrice(customer.averageOrderValue))
                        StatisticRow("آخرین فعالیت خرید", customer.lastPurchaseDateJalali)
                    }
                }

                // Billing Address Detail
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("نشانی صورتحساب پستی دائم:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(customer.billingAddress, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        
                        Text("نشانی محل سکونت/تحویل:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(customer.shippingAddress, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }

                // Internal supports / Note editing
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("یادداشت‌های اختصاصی پشتیبانی مشتری:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = internalNotes,
                            onValueChange = {
                                internalNotes = it
                                viewModel.addCustomerNotes(customer.id, it)
                            },
                            placeholder = { Text("ثبت جزئیات پشتیبانی (خدمات، تلفن، پیگیری خاص و غیره)...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            maxLines = 5,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // --- Custom MeliPayamak SMS Sending Dialog Engine ---
        if (showSmsDialog) {
            val repo = viewModel.repository
            val isMeliConfigured = repo.melipayamakUsername.isNotBlank() && repo.melipayamakPassword.isNotBlank()

            AlertDialog(
                onDismissRequest = { if (!isSendingSms) showSmsDialog = false },
                title = {
                    Text(
                        text = "ارسال پیامک به ${customer.firstName} ${customer.lastName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "شماره گیرنده: ${customer.phone}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = "انتخاب سریع قالب پیام:",
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
                            // Temp 1: خوش آمدگویی
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .clickable {
                                        smsText = "مشتری عزیز ${customer.firstName} ${customer.lastName}، به جمع خانواده بزرگ ما خوش آمدید!"
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("خوش‌آمدگویی", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }

                            // Temp 2: تشکر از خرید
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .clickable {
                                        smsText = "مشتری عزیز ${customer.firstName}، سفارش جدید شما با موفقیت ثبت شد و به زودی ارسال می‌شود. از اعتماد شما سپاسگزاریم."
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("تشکر از خرید", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }

                            // Temp 3: پشتیبانی دائم
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .clickable {
                                        smsText = "سلام ${customer.firstName} عزیز، جهت هماهنگی فاکتور شما، لطفا با پشتیبانی فروشگاه ما تماس حاصل فرمایید."
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("تماس با پشتیبانی", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }

                            // Temp 4: کد تخفیف
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                    .clickable {
                                        smsText = "جناب ${customer.lastName} عزیز، به عنوان قدردانی، یک کد تخفیف ویژه اختصاصی برای خرید بعدی شما: OFF95"
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("کد تخفیف ویژه", fontSize = 10.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = smsText,
                            onValueChange = { smsText = it },
                            label = { Text("متن پیامک") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 6,
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isSendingSms
                        )

                        // Character count & estimated sms count
                        val charLength = smsText.length
                        val smsPages = if (charLength <= 70) 1 else (charLength + 66) / 67 // approximation for Farsi SMS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تعداد کاراکتر: ${Helpers.toPersianDigits(charLength.toString())}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "بخش‌های پیامک: ${Helpers.toPersianDigits(smsPages.toString())} پیام",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Configuration warning banner
                        if (!isMeliConfigured) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9800).copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, Color(0xFFFF9800).copy(alpha = 0.25f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFE65100),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "مشخصات پنل ملی‌پیامک در تنظیمات وارد نشده است. پیام شبیه‌سازی خواهد شد. برای فرستادن مستقیم، آنها را در تنظیمات وارد نمایید.",
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp,
                                        color = Color(0xFFE65100)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSendingSms = true
                            coroutineScope.launch {
                                val result = viewModel.sendSmsToCustomer(customer.phone, smsText)
                                isSendingSms = false
                                when (result) {
                                    is com.example.core.network.SmsResult.Success -> {
                                        Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                                        showSmsDialog = false
                                    }
                                    is com.example.core.network.SmsResult.Error -> {
                                        Toast.makeText(context, result.errorMessage, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("submit_sms_button"),
                        enabled = !isSendingSms && smsText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        if (isSendingSms) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("در حال ارسال...", fontSize = 11.sp)
                        } else {
                            Text("ارسال با ملی‌پیامک", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Fallback to local device sending
                        OutlinedButton(
                            onClick = {
                                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${customer.phone}")).apply {
                                    putExtra("sms_body", smsText)
                                }
                                context.startActivity(smsIntent)
                                showSmsDialog = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenMoney),
                            border = BorderStroke(1.dp, GreenMoney.copy(alpha = 0.5f))
                        ) {
                            Text("باکارت گوشی", fontSize = 10.sp)
                        }

                        TextButton(
                            onClick = { showSmsDialog = false },
                            enabled = !isSendingSms
                        ) {
                            Text("انصراف", fontSize = 11.sp)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun StatisticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}
