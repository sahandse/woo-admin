package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainAppContainer
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.WooViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: WooViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dynamic system bars configuration
        enableEdgeToEdge()

        setContent {
            // Persistent dark mode toggle loaded from preferences
            var isDarkTheme by remember { mutableStateOf(viewModel.repository.isDarkThemeEnabled) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val isLoggedIn by viewModel.isLoggedIn.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        if (isLoggedIn) {
                            MainAppContainer(
                                viewModel = viewModel,
                                onLogout = { /* Handled in VM */ },
                                onToggleDarkTheme = { 
                                    isDarkTheme = it
                                    viewModel.repository.isDarkThemeEnabled = it
                                },
                                isDarkTheme = isDarkTheme
                            )
                        } else {
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = { /* Redirect flows */ }
                            )
                        }
                    }
                }
            }
        }
    }
}
