package com.btq.wallet.ui

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.btq.wallet.viewmodel.WalletViewModel
import com.btq.wallet.viewmodel.SuccessorConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnScope.SettingsScreen(viewModel: WalletViewModel) {
    val mnemonic by viewModel.mnemonic.collectAsState()
    val isHardwareCold by viewModel.isHardwareColdStorage.collectAsState()
    var recoveryPhrase by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRecoverDialog by remember { mutableStateOf(false) }
    val isVisible by viewModel.isMnemonicVisible.collectAsState()
    val successors by viewModel.successorConfigs.collectAsState()
    val isDmsEnabled by viewModel.isDmsEnabled.collectAsState()
    val scope = rememberCoroutineScope()

    Text("Security & Recovery", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isHardwareCold) Color(0xFF00FF88).copy(alpha = 0.1f) else Color(0xFF111111))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isHardwareCold) Icons.Default.AcUnit else Icons.Default.AccountBalanceWallet, null, tint = if (isHardwareCold) Color(0xFF00FF88) else Color.Gray)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(if (isHardwareCold) "Hardware Cold Storage" else "Standard Storage", fontWeight = FontWeight.Bold)
                Text(if (isHardwareCold) "Exempt from all inactivity sweeps" else "Subject to 14-day inactivity sweep", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("Secret Recovery Phrase", color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold)
    Text("This phrase provides total access to your BTQ and all 8 L2 memecoins.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), 
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        onClick = { if (isVisible) viewModel.hideMnemonic() else viewModel.requestShowMnemonic() }
    ) {
        if (isVisible) {
            androidx.compose.foundation.text.selection.SelectionContainer {
                Text(
                    text = mnemonic,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = Color.White
                )
            }
        } else {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VisibilityOff, null, tint = Color.Gray, modifier = Modifier.padding(end = 8.dp))
                Text("Tap to Reveal Recovery Phrase", color = Color.Gray)
            }
        }
    }
    
    Spacer(Modifier.height(24.dp))
    HorizontalDivider(color = Color(0xFF222222))
    Spacer(Modifier.height(24.dp))

    // Successor Configuration
    Text("Emergency Distress Protocol (EDP)", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00FF88))
    Text("Program pre-designated recipients for emergency distribution.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(Modifier.height(16.dp))

    // Dead Man's Switch Toggle
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text("Dead Man's Switch", fontWeight = FontWeight.Bold)
            Text("Auto-sweep after 14 days of inactivity", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
        Switch(
            checked = isDmsEnabled,
            onCheckedChange = { viewModel.toggleDms(it) },
            colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF88))
        )
    }

    val activeSuccessors = successors.count { it.address.isNotEmpty() && it.percentage > 0 }
    if (isDmsEnabled && activeSuccessors == 0) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "DMS is ENABLED but no successors are configured. Automated sweep will not trigger.",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    successors.forEachIndexed { index, config ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = config.address,
                onValueChange = { viewModel.updateSuccessorConfig(index, it, config.percentage) },
                label = { Text("Successor ${index + 1} Address") },
                modifier = Modifier.weight(0.7f),
                textStyle = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = config.percentage.toString(),
                onValueChange = { val p = it.toIntOrNull() ?: 0; viewModel.updateSuccessorConfig(index, config.address, p) },
                label = { Text("%") },
                modifier = Modifier.weight(0.3f),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
    }

    val totalPercentage = successors.sumOf { it.percentage }
    if (totalPercentage > 100) {
        Text("Warning: Total distribution exceeds 100% ($totalPercentage%)", color = Color.Red, style = MaterialTheme.typography.labelSmall)
    } else {
        Text("Total distribution: $totalPercentage%", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }

    Spacer(Modifier.height(24.dp))
    HorizontalDivider(color = Color(0xFF222222))
    Spacer(Modifier.height(24.dp))
    
    Text("Import Existing Wallet", style = MaterialTheme.typography.titleMedium, color = Color.White)
    OutlinedTextField(
        value = recoveryPhrase, 
        onValueChange = { recoveryPhrase = it }, 
        label = { Text("Enter 12/24 Word Phrase", color = Color(0xFF00FF88)) }, 
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("word1 word2 ...", color = Color.DarkGray) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00FF88),
            unfocusedBorderColor = Color.Gray,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            cursorColor = Color(0xFF00FF88)
        )
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = { if (recoveryPhrase.isNotEmpty()) showRecoverDialog = true }, 
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)
    ) {
        Text("Recover Identity")
    }

    if (showRecoverDialog) {
        AlertDialog(
            onDismissRequest = { showRecoverDialog = false },
            title = { Text("Recover Storage Mode") },
            text = { Text("Choose how you want to store this recovered identity. Cold Storage is recommended for long-term security.") },
            confirmButton = {
                TextButton(onClick = { viewModel.recoverWallet(recoveryPhrase, true); showRecoverDialog = false }) {
                    Text("Cold Storage")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.recoverWallet(recoveryPhrase, false); showRecoverDialog = false }) {
                    Text("Standard")
                }
            }
        )
    }

    Spacer(Modifier.height(40.dp))
    
    // Growth Mechanic: Share the Sovereign App
    val context = LocalContext.current
    OutlinedButton(
        onClick = {
            val shareMsg = """
                🔐 Step into the Future of Sovereignty.
                
                I'm inviting you to join Bitcoin-Quantum, the world's first post-quantum secure economy.
                
                Download the Sovereign Wallet: https://btq.li/download
                Join the revolution. #Sovereign #PQC #BTQ
            """.trimIndent()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareMsg)
            }
            context.startActivity(Intent.createChooser(intent, "Share Sovereign App"))
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Share, null, modifier = Modifier.padding(end = 8.dp))
        Text("Invite Peers to Sovereign Network")
    }

    Spacer(Modifier.height(12.dp))
    TextButton(
        onClick = { showCreateDialog = true },
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Text("Generate Brand New Identity", color = Color.Red)
    }

    TextButton(
        onClick = { viewModel.wipeWallet() },
        modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
        Text("Wipe & Reset Local Node", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Choose Storage Mode") },
            text = { Text("Standard mode sweeps inactive funds after 14 days. Hardware Cold Storage is exempt but requires manually managing security.") },
            confirmButton = {
                TextButton(onClick = { viewModel.createWallet(true, true); showCreateDialog = false }) {
                    Text("Cold Storage")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.createWallet(true, false); showCreateDialog = false }) {
                    Text("Standard")
                }
            }
        )
    }

    Spacer(Modifier.height(48.dp))
    Text(
        text = "BTQ Sovereign v1.2.4-stable\nNode ID: 0x82...f92a",
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val sweepJob = scope.launch {
                            delay(5000)
                            viewModel.executeEmergencyDistressSweep()
                        }
                        try {
                            awaitRelease()
                        } finally {
                            sweepJob.cancel()
                        }
                    }
                )
            },
        style = MaterialTheme.typography.labelSmall,
        color = Color.DarkGray,
        textAlign = TextAlign.Center,
        lineHeight = MaterialTheme.typography.labelSmall.lineHeight * 1.5
    )
}
