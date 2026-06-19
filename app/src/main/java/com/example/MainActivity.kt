package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainAppContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WooViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: WooViewModel by viewModels()
    private var appWentToBackground = false

    override fun onPause() {
        super.onPause()
        appWentToBackground = true
    }

    override fun onResume() {
        super.onResume()
        if (appWentToBackground && viewModel.isLoggedIn.value) {
            appWentToBackground = false
            tryBiometricAuth()
        } else {
            appWentToBackground = false
        }
    }

    private fun tryBiometricAuth() {
        val bm = BiometricManager.from(this)
        val canAuth = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) return

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // App unlocked — no action needed
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled or too many attempts — do nothing; app stays visible
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("تأیید هویت")
            .setSubtitle("برای ورود مجدد به پنل هویت خود را تأیید کنید")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var isDarkTheme by remember { mutableStateOf(viewModel.repository.isDarkThemeEnabled) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (isLoggedIn) {
                            MainAppContainer(
                                viewModel = viewModel,
                                onLogout = { },
                                onToggleDarkTheme = {
                                    isDarkTheme = it
                                    viewModel.repository.isDarkThemeEnabled = it
                                },
                                isDarkTheme = isDarkTheme
                            )
                        } else {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = { }
                            )
                        }
                    }
                }
            }
        }
    }
}
