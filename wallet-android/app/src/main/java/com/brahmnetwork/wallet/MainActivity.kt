package com.brahmnetwork.wallet

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.brahmnetwork.wallet.sentinel.SentinelRuntime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.brahmnetwork.wallet.viewmodel.VaultPasswordPrompt
import androidx.fragment.app.FragmentActivity
import com.brahmnetwork.wallet.ui.AppLockScreen
import com.brahmnetwork.wallet.ui.HomeScreen
import com.brahmnetwork.wallet.ui.MineScreen
import com.brahmnetwork.wallet.ui.OnboardingScreen
import com.brahmnetwork.wallet.ui.ReceiveScreen
import com.brahmnetwork.wallet.ui.SendScreen
import com.brahmnetwork.wallet.ui.SettingsScreen
import com.brahmnetwork.wallet.viewmodel.PasswordPurpose
import com.brahmnetwork.wallet.viewmodel.WalletViewModel

class MainActivity : FragmentActivity() {
    private val viewModel: WalletViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { SentinelRuntime.start(this, meshNodeId()) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
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

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            viewModel.lockSession()
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
    val passwordPrompt by viewModel.passwordPrompt.collectAsState()
    val lockOnOpen by viewModel.lockOnOpen.collectAsState()
    val sessionUnlocked by viewModel.sessionUnlocked.collectAsState()
    val hasWalletPassword by viewModel.hasWalletPassword.collectAsState()
    var vaultPassword by remember { mutableStateOf("") }
    var vaultConfirm by remember { mutableStateOf("") }

    val locked = address.isNotBlank() && lockOnOpen && hasWalletPassword && !sessionUnlocked

    LaunchedEffect(passwordPrompt) {
        vaultPassword = ""
        vaultConfirm = ""
    }

    if (!locked) passwordPrompt?.let { prompt ->
        val creating = prompt is VaultPasswordPrompt.Create
        val purpose = when (prompt) {
            is VaultPasswordPrompt.Create -> prompt.purpose
            is VaultPasswordPrompt.Enter -> prompt.purpose
        }
        val copy = passwordDialogCopy(creating, purpose)
        AlertDialog(
            onDismissRequest = { viewModel.dismissPasswordPrompt() },
            title = { Text(copy.first) },
            text = {
                Column {
                    Text(copy.second)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = vaultPassword,
                        onValueChange = { vaultPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (creating) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = vaultConfirm,
                            onValueChange = { vaultConfirm = it },
                            label = { Text("Confirm password") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.submitVaultPassword(vaultPassword, vaultConfirm)
                    vaultPassword = ""
                    vaultConfirm = ""
                }) {
                    Text(copy.third)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    vaultPassword = ""
                    vaultConfirm = ""
                    viewModel.dismissPasswordPrompt()
                }) { Text("Cancel") }
            }
        )
    }

    if (address.isEmpty()) {
        OnboardingScreen(viewModel)
        return
    }

    if (lockOnOpen && hasWalletPassword && !sessionUnlocked) {
        AppLockScreen(viewModel)
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

private fun passwordDialogCopy(
    creating: Boolean,
    purpose: PasswordPurpose
): Triple<String, String, String> {
    if (creating) {
        val body = "At least 8 characters. This password is never stored. It also locks extra wallets on this phone."
        return Triple("Create wallet password", body, "Save password")
    }
    return when (purpose) {
        PasswordPurpose.Send -> Triple(
            "Confirm send",
            "Enter your wallet password to sign this send.",
            "Send"
        )
        PasswordPurpose.Burn -> Triple(
            "Confirm burn",
            "Enter your wallet password to permanently destroy this amount.",
            "Burn"
        )
        PasswordPurpose.LaunchL2 -> Triple(
            "L2 launches closed",
            "New L2 launches are closed. There is no burn-to-launch.",
            "OK"
        )
        PasswordPurpose.ShowMnemonic -> Triple(
            "Reveal recovery phrase",
            "Enter your wallet password to show the 24-word phrase.",
            "Show"
        )
        PasswordPurpose.AppUnlock -> Triple(
            "Unlock wallet",
            "Enter your wallet password.",
            "Unlock"
        )
        PasswordPurpose.DisableAppLock -> Triple(
            "Turn off lock on open",
            "Enter your wallet password to stop asking at open.",
            "Turn off"
        )
        PasswordPurpose.DisableSendLock -> Triple(
            "Turn off send password",
            "Enter your wallet password to send without asking.",
            "Turn off"
        )
        else -> Triple(
            "Wallet password",
            "Enter your wallet password.",
            "Unlock"
        )
    }
}
