package com.brahmnetwork.wallet

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.brahmnetwork.wallet.sentinel.SentinelRuntime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.brahmnetwork.wallet.ui.HomeScreen
import com.brahmnetwork.wallet.ui.MineScreen
import com.brahmnetwork.wallet.ui.OnboardingScreen
import com.brahmnetwork.wallet.ui.ReceiveScreen
import com.brahmnetwork.wallet.ui.SendScreen
import com.brahmnetwork.wallet.ui.SettingsScreen
import com.brahmnetwork.wallet.viewmodel.AuthRequest
import com.brahmnetwork.wallet.viewmodel.WalletViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: WalletViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { SentinelRuntime.start(this, meshNodeId()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestMeshPermissions()
        SentinelRuntime.start(this, meshNodeId())
        applyClaimIntent(intent)
        setContent {
            val scheme = darkColorScheme(
                primary = Color(0xFF00FF88),
                background = Color.Black,
                surface = Color(0xFF111111),
                onPrimary = Color.Black,
                onBackground = Color.White,
                onSurface = Color.White
            )
            MaterialTheme(colorScheme = scheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    WalletApp(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyClaimIntent(intent)
    }

    private fun applyClaimIntent(intent: Intent?) {
        val data = intent?.data?.toString() ?: return
        viewModel.applyClaimLink(data)
    }

    private fun meshNodeId(): String {
        val addr = getSharedPreferences("sovereign_prefs", MODE_PRIVATE)
            .getString("wallet_address", "")
        return if (addr.isNullOrBlank()) "brah-wallet" else addr
    }

    private fun requestMeshPermissions() {
        val needed = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed += Manifest.permission.NEARBY_WIFI_DEVICES
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            needed += Manifest.permission.BLUETOOTH_CONNECT
            needed += Manifest.permission.BLUETOOTH_SCAN
            needed += Manifest.permission.BLUETOOTH_ADVERTISE
        }
        permissionLauncher.launch(needed.toTypedArray())
    }
}

private enum class WalletTab { Home, Send, Receive, Mine, Settings }

@Composable
private fun WalletApp(viewModel: WalletViewModel) {
    val address by viewModel.address.collectAsState()
    var tab by remember { mutableStateOf(WalletTab.Home) }
    val accent = Color(0xFF00FF88)
    val activity = LocalContext.current as FragmentActivity
    val authGate by viewModel.authGate.collectAsState()
    val authFallback by viewModel.authFallback.collectAsState()

    LaunchedEffect(authGate) {
        val req = authGate ?: return@LaunchedEffect
        val title = when (req) {
            is AuthRequest.SendTokens -> "Confirm send"
            AuthRequest.ShowMnemonic -> "Reveal recovery phrase"
        }
        val subtitle = when (req) {
            is AuthRequest.SendTokens -> "Unlock to sign and broadcast"
            AuthRequest.ShowMnemonic -> "Unlock to show the 24-word phrase"
        }
        viewModel.clearAuthGate()
        if (SecurityUtils.isDeviceAuthAvailable(activity)) {
            SecurityUtils.showBiometricPrompt(
                activity,
                title,
                subtitle,
                onSuccess = { viewModel.executePendingAuth() },
                onError = { err -> viewModel.reportAuthError(err) }
            )
        } else {
            viewModel.showAuthFallback()
        }
    }

    if (authFallback != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelAuth() },
            title = { Text("Confirm") },
            text = {
                Text(
                    when (authFallback) {
                        is AuthRequest.SendTokens -> "No biometric or device lock available. Continue with this send?"
                        AuthRequest.ShowMnemonic -> "No biometric or device lock available. Reveal the recovery phrase?"
                        null -> ""
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.executePendingAuth() }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelAuth() }) { Text("Cancel") }
            }
        )
    }

    if (address.isEmpty()) {
        OnboardingScreen(viewModel)
        return
    }

    Scaffold(
        containerColor = Color.Black,
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF111111)) {
                WalletTab.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = tab == destination,
                        onClick = { tab = destination },
                        icon = {
                            Icon(
                                imageVector = when (destination) {
                                    WalletTab.Home -> Icons.Default.Home
                                    WalletTab.Send -> Icons.Default.Send
                                    WalletTab.Receive -> Icons.Default.QrCode
                                    WalletTab.Mine -> Icons.Default.Construction
                                    WalletTab.Settings -> Icons.Default.Settings
                                },
                                contentDescription = destination.name
                            )
                        },
                        label = { Text(destination.name) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accent,
                            selectedTextColor = accent,
                            indicatorColor = Color(0xFF1B3D2F),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (tab) {
                WalletTab.Home -> HomeScreen(viewModel)
                WalletTab.Send -> SendScreen(viewModel)
                WalletTab.Receive -> ReceiveScreen(viewModel)
                WalletTab.Mine -> MineScreen(viewModel)
                WalletTab.Settings -> SettingsScreen(viewModel)
            }
        }
    }
}
