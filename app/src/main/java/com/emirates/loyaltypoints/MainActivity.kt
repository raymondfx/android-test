package com.emirates.loyaltypoints

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emirates.loyaltypoints.data.network.NetworkMonitorImpl
import com.emirates.loyaltypoints.data.repository.AuthRepositoryImpl
import com.emirates.loyaltypoints.presentation.home.HomeScreen
import com.emirates.loyaltypoints.presentation.login.LoginScreen
import com.emirates.loyaltypoints.presentation.login.LoginViewModel
import com.emirates.loyaltypoints.ui.theme.LoyaltyPointsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LoyaltyPointsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    // State to track if user is logged in
                    var isLoggedIn by remember { mutableStateOf(false) }

                    if (isLoggedIn) {
                        // Show home screen after successful login
                        HomeScreen(
                            onLogout = {
                                isLoggedIn = false
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        // Show login screen
                        val authRepository = AuthRepositoryImpl(this@MainActivity)
                        val networkMonitor = NetworkMonitorImpl(this@MainActivity)
                        val viewModel = LoginViewModel(authRepository, networkMonitor)

                        LoginScreen(
                            viewModel = viewModel,
                            onNavigateToHome = {
                                isLoggedIn = true
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LoyaltyPointsTheme {
        Greeting("Android")
    }
}