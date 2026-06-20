package com.sahand.wooadmin.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sahand.wooadmin.ui.theme.RedError
import com.sahand.wooadmin.ui.viewmodel.WooViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: WooViewModel,
    onLoginSuccess: () -> Unit
) {
    var storeName   by remember { mutableStateOf("") }
    var storeUrl    by remember { mutableStateOf("") }
    var consumerKey by remember { mutableStateOf("") }
    var consumerSecret by remember { mutableStateOf("") }
    var isHttpsOnly by remember { mutableStateOf(true) }

    var isConnecting     by remember { mutableStateOf(false) }
    var connectionMessage by remember { mutableStateOf("") }
    var apiError         by remember { mutableStateOf<String?>(null) }

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    LaunchedEffect(isLoggedIn) { if (isLoggedIn) onLoginSuccess() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))

                // Logo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "اتصال به فروشگاه",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "اطلاعات ووکامرس خود را وارد کنید",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
                )

                // Card form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Progress
                        AnimatedVisibility(visible = isConnecting) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    connectionMessage,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Error
                        AnimatedVisibility(visible = apiError != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(RedError.copy(alpha = 0.1f))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.ErrorOutline, null, tint = RedError, modifier = Modifier.size(18.dp))
                                Text(apiError ?: "", color = RedError, fontSize = 12.sp)
                            }
                        }

                        // نام فروشگاه
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("نام فروشگاه") },
                            placeholder = { Text("مثال: فروشگاه من") },
                            leadingIcon = { Icon(Icons.Default.Store, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isConnecting
                        )

                        // آدرس سایت
                        OutlinedTextField(
                            value = storeUrl,
                            onValueChange = { storeUrl = it },
                            label = { Text("آدرس سایت") },
                            placeholder = { Text("https://my-shop.com") },
                            leadingIcon = { Icon(Icons.Default.Language, null) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isConnecting
                        )

                        // Consumer Key
                        OutlinedTextField(
                            value = consumerKey,
                            onValueChange = { consumerKey = it },
                            label = { Text("Consumer Key") },
                            placeholder = { Text("ck_...") },
                            leadingIcon = { Icon(Icons.Default.VpnKey, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isConnecting
                        )

                        // Consumer Secret
                        OutlinedTextField(
                            value = consumerSecret,
                            onValueChange = { consumerSecret = it },
                            label = { Text("Consumer Secret") },
                            placeholder = { Text("cs_...") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isConnecting
                        )

                        // HTTPS toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("اتصال امن HTTPS", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Text("برای سایت‌های بدون SSL خاموش کنید", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = isHttpsOnly,
                                onCheckedChange = { isHttpsOnly = it },
                                enabled = !isConnecting
                            )
                        }

                        // دکمه اتصال
                        Button(
                            onClick = {
                                isConnecting = true
                                apiError = null
                                viewModel.connectWooCommerceSecurely(
                                    storeName = storeName,
                                    storeUrl = storeUrl,
                                    consumerKey = consumerKey,
                                    consumerSecret = consumerSecret,
                                    isHttpsOnly = isHttpsOnly,
                                    onProgressUpdate = { msg -> connectionMessage = msg },
                                    onSuccess = { isConnecting = false },
                                    onError = { err -> isConnecting = false; apiError = err }
                                )
                            },
                            enabled = !isConnecting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            if (isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.Link, null, modifier = Modifier.size(20.dp), tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("اتصال و ورود", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // راهنما
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("چطور کلیدها را بگیرم؟", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(
                            "در پنل ووردپرس: ووکامرس ← تنظیمات ← پیشرفته ← REST API ← افزودن کلید",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
