package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AdminBluePrimary
import com.example.ui.theme.RedError
import com.example.ui.viewmodel.WooViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: WooViewModel,
    onLoginSuccess: () -> Unit
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = دمو, 1 = اتصال مستقیم ووکامرس

    // Quick sandbox fields
    var username by remember { mutableStateOf("admin") }
    var password by remember { mutableStateOf("admin123") }
    var showPassword by remember { mutableStateOf(false) }

    // API connection fields
    var storeName by remember { mutableStateOf("فروشگاه اصلی ووکامرس") }
    var storeUrl by remember { mutableStateOf("https://my-store.ir") }
    var consumerKey by remember { mutableStateOf("ck_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0") }
    var consumerSecret by remember { mutableStateOf("cs_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7r8s9t0") }
    var isHttpsOnly by remember { mutableStateOf(true) }

    // Connection process status
    var isConnecting by remember { mutableStateOf(false) }
    var connectionMessage by remember { mutableStateOf("") }
    var apiError by remember { mutableStateOf<String?>(null) }

    val loginError by viewModel.loginError.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Redirect when login succeeds
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onLoginSuccess()
        }
    }

    // Force RTL local layout direction for Persian user interfaces
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // Main Card container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand Accent logo
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (activeTab == 0) Icons.Default.Lock else Icons.Default.Lock,
                        contentDescription = "قفل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (activeTab == 0) "ورود به پنل مدیریت فروشگاه" else "اتصال امن به پنل ووکامرس",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (activeTab == 0) "سامانه یکپارچه کنترل وضعیت ووکامرس" else "تنظیم مشخصات و کلیدهای ارتباطی API",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Custom aesthetic segmented connection switcher tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable(enabled = !isConnecting) { activeTab = 0 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ورود با اکانت دمو",
                            color = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable(enabled = !isConnecting) { activeTab = 1 }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "اتصال به وب‌سایت (API)",
                            color = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (activeTab == 0) {
                    // --- TAB I: SANDBOX DEMO LOGIN ---
                    
                    // Error message
                    AnimatedVisibility(visible = loginError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = loginError ?: "",
                                color = RedError,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    // Username field
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("نام کاربری") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("username_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("رمز عبور") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "نمایش رمز"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login click button
                    Button(
                        onClick = { viewModel.performLogin(username, password) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("login_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = "ورود به حساب کاربری",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "حالت آزمایشی: admin / admin123",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // --- TAB II: REAL WOOCOMMERCE CONNECTION RECEIVER ---

                    // Connecting progress visual indicator
                    if (isConnecting) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = connectionMessage,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Input constraints or SSL exceptions
                    AnimatedVisibility(visible = apiError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = RedError.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = apiError ?: "",
                                color = RedError,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    // 1. Store Name Parameter
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("نام فروشگاه (عنوان اتصال)") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_store_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isConnecting,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Store URL
                    OutlinedTextField(
                        value = storeUrl,
                        onValueChange = { storeUrl = it },
                        label = { Text("آدرس سایت ووکامرس (URL)") },
                        placeholder = { Text("https://my-shop.com") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Home, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_store_url_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isConnecting,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Consumer Key (ck_...)
                    OutlinedTextField(
                        value = consumerKey,
                        onValueChange = { consumerKey = it },
                        label = { Text("کلید مصرف‌کننده (Consumer Key)") },
                        placeholder = { Text("ck_...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_consumer_key_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isConnecting,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Consumer Secret (cs_...)
                    OutlinedTextField(
                        value = consumerSecret,
                        onValueChange = { consumerSecret = it },
                        label = { Text("رمز مصرف‌کننده (Consumer Secret)") },
                        placeholder = { Text("cs_...") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("api_consumer_secret_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        enabled = !isConnecting,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. SSL / HTTPS Validation Option switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("الزام اتصال امن رمزگذاری‌شده (HTTPS)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("جهت محافظت رمزگذاری‌شده کلیدهای API و سفارشات با SSL", fontSize = 10.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isHttpsOnly,
                            onCheckedChange = { isHttpsOnly = it },
                            enabled = !isConnecting
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 6. Action Submission Button
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
                                onProgressUpdate = { msg ->
                                    connectionMessage = msg
                                },
                                onSuccess = {
                                    isConnecting = false
                                },
                                onError = { error ->
                                    isConnecting = false
                                    apiError = error
                                }
                            )
                        },
                        enabled = !isConnecting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("api_connect_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "اتصال امن به ووکامرس و ورود",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
