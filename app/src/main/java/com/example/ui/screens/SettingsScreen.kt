package com.sahand.wooadmin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahand.wooadmin.data.WooStore
import com.sahand.wooadmin.ui.viewmodel.WooViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: WooViewModel,
    onNavigateToActivities: () -> Unit,
    onNavigateToCoupons: () -> Unit,
    onNavigateToReports: () -> Unit = {},
    onNavigateToAdminUsers: () -> Unit = {},
    onToggleDarkTheme: (Boolean) -> Unit,
    isDarkTheme: Boolean
) {
    val loggedInUser by viewModel.loggedInUser.collectAsState()
    val storesList by viewModel.stores.collectAsState(initial = emptyList())
    val activeStore = storesList.find { it.isActive }
    
    var backendUrlInput by remember { mutableStateOf(viewModel.repository.backendUrl) }
    
    var showAddStoreDialog by remember { mutableStateOf(false) }
    var newStoreName by remember { mutableStateOf("") }
    var newStoreUrl by remember { mutableStateOf("") }
    var newStoreKey by remember { mutableStateOf("") }
    var newStoreSecret by remember { mutableStateOf("") }

    val repo = viewModel.repository
    var meliUsername by remember { mutableStateOf(repo.melipayamakUsername) }
    var meliPassword by remember { mutableStateOf(repo.melipayamakPassword) }
    var meliSender by remember { mutableStateOf(repo.melipayamakSender) }
    var smsStatusEnabled by remember { mutableStateOf(repo.smsStatusChangeEnabled) }
    var templateProcessing by remember { mutableStateOf(repo.smsTemplateStatusProcessing) }
    var templateCompleted by remember { mutableStateOf(repo.smsTemplateStatusCompleted) }

    val context = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile header panel
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
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
                            text = loggedInUser?.fullName ?: "مدیر دمو سیستم",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val roleText = when (loggedInUser?.role) {
                            "SUPER_ADMIN" -> "مدیر اصلی فروشگاه"
                            "STORE_ADMIN" -> "مدیر عمومی"
                            "STOCK_CLERK" -> "انباردار بخش مرسولات"
                            else -> "تیم پشتیبانی"
                        }
                        Text(
                            text = roleText,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Connection Profile Settings
            SettingCategory("تنظیمات اتصال وبسایت") {
                val isSyncing by viewModel.isSyncing.collectAsState()
                val lastSyncTime by viewModel.lastSyncTime.collectAsState()
                val syncMessage by viewModel.syncMessage.collectAsState()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("همگام‌سازی با ووکامرس", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (isSyncing) syncMessage ?: "در حال دریافت..."
                                   else if (lastSyncTime != null) "آخرین بروزرسانی: $lastSyncTime"
                                   else "برای دریافت اطلاعات جدید از سایت، دکمه را بزنید",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.syncAllData() },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            } else {
                                Icon(imageVector = Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSyncing) "در حال بروزرسانی" else "همگام‌سازی", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { viewModel.smartClean() },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(imageVector = Icons.Default.AutoDelete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("هوشمند پاک کن", fontSize = 12.sp)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Text("نشانی سرور بکاند اختصاصی", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = backendUrlInput,
                    onValueChange = {
                        backendUrlInput = it
                        viewModel.repository.backendUrl = it
                    },
                    placeholder = { Text("https://api.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Branches / Multiplying stores lists
            SettingCategory("مدیریت چند فروشگاهی (WooCommerce)") {
                storesList.forEach { store ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (store.isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent)
                            .border(1.dp, if (store.isActive) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.changeActiveStore(store.id)
                                Toast.makeText(context, "فروشگاه فعال به «${store.name}» تغییر یافت", Toast.LENGTH_SHORT).show()
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = store.isActive, onClick = { viewModel.changeActiveStore(store.id) })

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(store.name, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(store.url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }

                        if (storesList.size > 1) {
                            IconButton(onClick = { viewModel.removeStore(store.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Button(
                    onClick = { showAddStoreDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("افزودن فروشگاه جدید", fontSize = 12.sp)
                }
            }

            // Shortcuts lists
            SettingCategory("ابزارهای کمکی فروشگاه") {
                SettingShortcutRow(Icons.Default.ConfirmationNumber, "کدهای تخفیف و کوپن آنلاین") {
                    onNavigateToCoupons()
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                SettingShortcutRow(Icons.Default.History, "سیاهه کامل فعالیت مدیران سیستم") {
                    onNavigateToActivities()
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                SettingShortcutRow(Icons.Default.BarChart, "گزارشات مالی و تحلیل فروش") {
                    onNavigateToReports()
                }

                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                SettingShortcutRow(Icons.Default.ManageAccounts, "مدیریت کاربران ادمین") {
                    onNavigateToAdminUsers()
                }
            }

            // --- MELIPAYAMAK SETTINGS SECTION ---
            SettingCategory("تنظیمات درگاه ملی‌پیامک (MeliPayamak)") {
                OutlinedTextField(
                    value = meliUsername,
                    onValueChange = {
                        meliUsername = it
                        repo.melipayamakUsername = it
                    },
                    label = { Text("نام کاربری ملی‌پیامک") },
                    placeholder = { Text("مثال: 09123456789") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = meliPassword,
                    onValueChange = {
                        meliPassword = it
                        repo.melipayamakPassword = it
                    },
                    label = { Text("کلمه عبور وب‌سرویس یا API Key") },
                    placeholder = { Text("رمز عبور پنل پیامک شما") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = meliSender,
                    onValueChange = {
                        meliSender = it
                        repo.melipayamakSender = it
                    },
                    label = { Text("شماره فرستنده (خط اختصاصی/اشتراکی)") },
                    placeholder = { Text("مثال: 500040001015") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ارسال خودکار زمان تغییر وضعیت سفارش", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("ارسال مستقیم پیامکِ به‌روزرسانی زمان آماده‌سازی و قطعی شدن فاکتور", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = smsStatusEnabled,
                        onCheckedChange = {
                            smsStatusEnabled = it
                            repo.smsStatusChangeEnabled = it
                        }
                    )
                }

                if (smsStatusEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = templateProcessing,
                        onValueChange = {
                            templateProcessing = it
                            repo.smsTemplateStatusProcessing = it
                        },
                        label = { Text("قالب وضعیت: در حال آماده‌سازی (PROCESSING)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text("متغیرها: {name} برای نام مشتری، {order_id} برای شماره فاکتور", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = templateCompleted,
                        onValueChange = {
                            templateCompleted = it
                            repo.smsTemplateStatusCompleted = it
                        },
                        label = { Text("قالب وضعیت: تکمیل شده (COMPLETED)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Text("متغیرها: {name} برای نام، {order_id} برای شماره فاکتور، {amount} برای مبلغ نهایی فاکتور", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                }
            }

            // Visual details
            SettingCategory("تنظیمات ظاهری برنامه") {
                // Dark Theme toggle
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("حالت تاریک سیستم (شب)", fontSize = 14.sp)
                        Text("تم رنگی تیره و کاهش نور مضر مانیتور", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { onToggleDarkTheme(it) }
                    )
                }
            }

            // Logouts
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Button(
                    onClick = { viewModel.logout() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("logout_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("خروج ایمن از حساب سیستم", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Add Store Dialog overlay
        if (showAddStoreDialog) {
            AlertDialog(
                onDismissRequest = { showAddStoreDialog = false },
                title = { Text("افزودن فروشگاه جدید") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(value = newStoreName, onValueChange = { newStoreName = it }, label = { Text("نام فروشگاه (شعبه)") })
                        OutlinedTextField(value = newStoreUrl, onValueChange = { newStoreUrl = it }, label = { Text("آدرس الکترونیکی (URL)") })
                        OutlinedTextField(value = newStoreKey, onValueChange = { newStoreKey = it }, label = { Text("کلید مصرف کننده (Consumer Key)") })
                        OutlinedTextField(value = newStoreSecret, onValueChange = { newStoreSecret = it }, label = { Text("رمز مصرف کننده (Consumer Secret)") })
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newStoreName.isNotBlank() && newStoreUrl.isNotBlank()) {
                                viewModel.createStore(
                                    WooStore(
                                        name = newStoreName,
                                        url = newStoreUrl,
                                        consumerKey = newStoreKey,
                                        consumerSecret = newStoreSecret,
                                        isActive = false
                                    )
                                )
                                showAddStoreDialog = false
                                Toast.makeText(context, "فروشگاه جدید به پنل اضافه شد", Toast.LENGTH_SHORT).show()
                                
                                // Reset fields
                                newStoreName = ""
                                newStoreUrl = ""
                                newStoreKey = ""
                                newStoreSecret = ""
                            }
                        }
                    ) {
                        Text("افزودن")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddStoreDialog = false }) { Text("انصراف") }
                }
            )
        }
    }
}

@Composable
fun SettingCategory(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        content()
    }
}

@Composable
fun SettingShortcutRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Text(text = label, fontSize = 14.sp)
        }
        Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null, tint = Color.Gray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCleanScreen(viewModel: WooViewModel) {
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncMessage by viewModel.syncMessage.collectAsState()
    var lastResult by remember { mutableStateOf<String?>(null) }
    var hasRun by remember { mutableStateOf(false) }

    LaunchedEffect(syncMessage) {
        if (syncMessage != null && !isSyncing) {
            lastResult = syncMessage
            hasRun = true
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("هوشمند پاک کن", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "هوشمند پاک کن چیست؟",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                "این ابزار، محصولات و سفارش‌هایی را که از ووکامرس حذف شده‌اند اما هنوز در پایگاه داده محلی باقی مانده‌اند پاکسازی می‌کند.",
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Default.CleaningServices,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "پاکسازی هوشمند",
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        if (isSyncing && syncMessage != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(12.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(syncMessage ?: "", fontSize = 13.sp)
                            }
                        }

                        if (hasRun && !isSyncing && lastResult != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(lastResult ?: "", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }

                        Button(
                            onClick = {
                                hasRun = false
                                lastResult = null
                                viewModel.smartClean()
                            },
                            enabled = !isSyncing,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onError
                                )
                            } else {
                                Icon(Icons.Default.AutoDelete, null, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (isSyncing) "در حال پاکسازی..." else "شروع پاکسازی", fontSize = 15.sp)
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "چه چیزی پاک می‌شود؟",
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        listOf(
                            "محصولاتی که از پنل ووکامرس حذف شده‌اند",
                            "سفارش‌هایی که از پنل ووکامرس پاک شده‌اند",
                            "رکوردهای قدیمی که دیگر معتبر نیستند"
                        ).forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                                Text(item, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
