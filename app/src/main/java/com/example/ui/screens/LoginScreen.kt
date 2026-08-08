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
import com.example.ui.theme.RedError
import com.example.ui.viewmodel.WooViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: WooViewModel,
    onLoginSuccess: () -> Unit
) {
    // API connection fields
    var storeName by remember { mutableStateOf("") }
    var storeUrl by remember { mutableStateOf("") }
    var consumerKey by remember { mutableStateOf("") }
    var consumerSecret by remember { mutableStateOf("") }

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
                        imageVector = Icons.Default.Lock,
                        contentDescription = "قفل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "اتصال به ووکامرس",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "برای مدیریت فروشگاه، اطلاعات اتصال به سایت ووکامرس را وارد کنید",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                    label = { Text("نام فروشگاه") },
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

                Spacer(modifier = Modifier.height(24.dp))

                // Action Submission Button
                Button(
                    onClick = {
                        isConnecting = true
                        apiError = null
                        viewModel.connectWooCommerceSecurely(
                            storeName = storeName,
                            storeUrl = storeUrl,
                            consumerKey = consumerKey,
                            consumerSecret = consumerSecret,
                            isHttpsOnly = true,
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
                            "اتصال به ووکامرس",
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
