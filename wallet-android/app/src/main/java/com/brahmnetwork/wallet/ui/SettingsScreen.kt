package com.brahmnetwork.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brahmnetwork.wallet.viewmodel.WalletViewModel

@Composable
fun ColumnScope.SettingsScreen(viewModel: WalletViewModel) {
    val context = LocalContext.current
    val mnemonic by viewModel.mnemonic.collectAsState()
    val isHardwareCold by viewModel.isHardwareColdStorage.collectAsState()
    val hideL1Balances by viewModel.hideL1Balances.collectAsState()
    var recoveryPhrase by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRecoverDialog by remember { mutableStateOf(false) }
    val isVisible by viewModel.isMnemonicVisible.collectAsState()

    Text("Security & Recovery", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))

    val rpcUrl by viewModel.rpcUrl.collectAsState()
    var rpcDraft by remember(rpcUrl) { mutableStateOf(rpcUrl) }
    Text("Node RPC", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00FF88))
    Text("Leave empty to auto-find the node on Wi‑Fi (Sentinel HELLO) or last working URL. Mine tries LAN (1s) then the public HTTPS host. This is not an Ethereum RPC.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = rpcDraft,
        onValueChange = { rpcDraft = it },
        label = { Text("RPC URL") },
        placeholder = { Text(com.brahmnetwork.wallet.Constants.publicHttps()) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF00FF88),
            unfocusedBorderColor = Color(0xFF222222),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { viewModel.setRpcUrl(rpcDraft) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
    ) {
        Text("Save RPC URL")
    }

    Spacer(Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isHardwareCold) Color(0xFF00FF88).copy(alpha = 0.1f) else Color(0xFF111111))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isHardwareCold) Icons.Default.AcUnit else Icons.Default.AccountBalanceWallet, null, tint = if (isHardwareCold) Color(0xFF00FF88) else Color.Gray)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("14-day inactivity sweep", fontWeight = FontWeight.Bold)
                Text(
                    if (isHardwareCold) "Off. This phone vault will not auto-sweep after 14 days idle." else "On. Unused funds on this phone vault can be swept after 14 days.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Switch(
                checked = !isHardwareCold,
                onCheckedChange = { viewModel.setInactivitySweep(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF88))
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VisibilityOff, null, tint = if (hideL1Balances) Color(0xFF00FF88) else Color.Gray)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Hide L1 Brahma Coin", fontWeight = FontWeight.Bold)
                Text(
                    "Hides Brahma Coin amounts on Home and Mine. Next-mine and network totals stay visible.",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Switch(
                checked = hideL1Balances,
                onCheckedChange = { viewModel.setHideL1Balances(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF00FF88))
            )
        }
    }

    Spacer(Modifier.height(16.dp))
    Text("Secret Recovery Phrase", color = Color(0xFFFFCC00), fontWeight = FontWeight.Bold)
    Text("This phrase provides total access to your Brahma Coin.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

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

    Text("Referral Sovereignty", style = MaterialTheme.typography.titleMedium, color = Color(0xFF00FF88))
    Text("Expand the network and earn Genesis rewards.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(Modifier.height(16.dp))

    val userAddress by viewModel.address.collectAsState()
    val referralCount by viewModel.referralTreeCount.collectAsState()
    val referralLink = com.brahmnetwork.wallet.Constants.shareClaimUrl(
        userAddress,
        viewModel.shareNodeBase()
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Referral Tree Size", fontWeight = FontWeight.Bold)
                Text("$referralCount Peers", color = Color(0xFF00FF88), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = referralLink,
                onValueChange = {},
                readOnly = true,
                label = { Text("Your Unique Referral Link") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Referral Link", referralLink)
                        clipboard.setPrimaryClip(clip)
                    }) {
                        Icon(Icons.Default.ContentCopy, null, tint = Color.Gray)
                    }
                },
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00FF88),
                    unfocusedBorderColor = Color(0xFF222222),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        }
    }

    Spacer(Modifier.height(24.dp))
    HorizontalDivider(color = Color(0xFF222222))
    Spacer(Modifier.height(24.dp))

    Text("Crypto Governance", style = MaterialTheme.typography.titleMedium, color = Color.White)
    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = "ML-DSA (NIST-65) + SPHINCS+",
        onValueChange = {},
        readOnly = true,
        label = { Text("Active Cryptographic Primitive") },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Gray,
            unfocusedBorderColor = Color(0xFF222222),
            focusedTextColor = Color.Gray,
            unfocusedTextColor = Color.Gray
        )
    )

    Spacer(Modifier.height(24.dp))
    HorizontalDivider(color = Color(0xFF222222))
    Spacer(Modifier.height(24.dp))

    Text("Sentinel mesh (MY NETWORK)", style = MaterialTheme.typography.titleMedium, color = Color.White)
    Text(
        "Store-and-forward gossip. A mined block is only paid after the node height increases.",
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray
    )

    Spacer(Modifier.height(24.dp))
    HorizontalDivider(color = Color(0xFF222222))
    Spacer(Modifier.height(24.dp))

    Text("Import Existing Wallet", style = MaterialTheme.typography.titleMedium, color = Color.White)
    OutlinedTextField(
        value = recoveryPhrase,
        onValueChange = { recoveryPhrase = it },
        label = { Text("Enter 24-word phrase (legacy 12-word still derives)", color = Color(0xFF00FF88)) },
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
            text = { Text("Choose how you want to store this recovered identity. Sweep-exempt skips the 14-day inactivity timer. Both are software vaults.") },
            confirmButton = {
                TextButton(onClick = { viewModel.recoverWallet(recoveryPhrase, true); showRecoverDialog = false }) {
                    Text("Sweep-exempt vault")
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

    OutlinedButton(
        onClick = {
            viewModel.shareInvite(context)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Share, null, modifier = Modifier.padding(end = 8.dp))
        Text("Share invite link")
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
            text = { Text("Standard mode sweeps inactive funds after 14 days. Sweep-exempt vault skips that timer. Both are software vaults on this phone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.createWallet(true, true); showCreateDialog = false }) {
                    Text("Sweep-exempt vault")
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
        text = "Brahma Coin · BRAHMNETWORK v1.0.34",
        modifier = Modifier.fillMaxWidth(),
        style = MaterialTheme.typography.labelSmall,
        color = Color.DarkGray,
        textAlign = TextAlign.Center
    )
}
