package com.brahmnetwork.wallet.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.brahmnetwork.wallet.Constants
import com.brahmnetwork.wallet.CryptoManager
import com.brahmnetwork.wallet.ShareActions
import com.brahmnetwork.wallet.sentinel.SentinelRuntime
import com.brahmnetwork.wallet.viewmodel.WalletViewModel
import java.util.Locale

@Composable
fun ColumnScope.HomeScreen(viewModel: WalletViewModel) {
    val balance by viewModel.balance.collectAsState()
    val status by viewModel.status.collectAsState()
    val address by viewModel.address.collectAsState()
    val isHardwareCold by viewModel.isHardwareColdStorage.collectAsState()
    val genesisRank by viewModel.genesisRank.collectAsState()
    val chainHeight by viewModel.chainHeight.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val totalMined by viewModel.totalMined.collectAsState()
    val blockReward by viewModel.blockReward.collectAsState()
    val firstMinePending by viewModel.firstMinePending.collectAsState()
    val joinedCount by viewModel.joinedCount.collectAsState()
    val meshPeers by viewModel.meshPeers.collectAsState()
    val rpcUrl by viewModel.rpcUrl.collectAsState()
    val referralTreeCount by viewModel.referralTreeCount.collectAsState()
    val queuedTx by SentinelRuntime.queuedTx.collectAsState()
    val hideL1Balances by viewModel.hideL1Balances.collectAsState()

    var showBurnDialog by remember { mutableStateOf(false) }
    var burnAmount by remember { mutableStateOf("") }

    val sovereignGreen = Color(0xFF00FF88)

    val mineBanner = buildString {
        append(String.format(Locale.US, "Next mine %.4f Brahma Coin", blockReward))
        if (firstMinePending) append(" · first-mine +1 included")
        append(String.format(Locale.US, " · %d joined", joinedCount))
        if (genesisRank > 0L) append(" · rank #$genesisRank")
    }
    Surface(
        color = sovereignGreen,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = mineBanner,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = Color.Black,
            fontWeight = FontWeight.ExtraBold
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Brahma Coin", style = MaterialTheme.typography.headlineMedium, color = sovereignGreen, modifier = Modifier.weight(1f), fontWeight = FontWeight.Black)
        if (isHardwareCold) {
            Surface(
                color = Color(0xFF00FF88).copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("VAULT", color = Color(0xFF00FF88), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }
    }
    Text(
        text = if (address.isBlank()) "No address" else address,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Status: $status", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Mesh $meshPeers",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00FF88)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Manual Re-sync",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00FF88),
            modifier = Modifier
                .clickable { viewModel.refreshBalances() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }

    Spacer(Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Brahma Coin", color = Color.DarkGray, style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (hideL1Balances) "••••" else String.format(Locale.US, "%.4f", balance),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { viewModel.setHideL1Balances(!hideL1Balances) }) {
                    Icon(
                        imageVector = if (hideL1Balances) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (hideL1Balances) "Show L1" else "Hide L1",
                        tint = Color.DarkGray
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
        border = BorderStroke(1.dp, sovereignGreen)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ElectricBolt, null, tint = Color(0xFFFFCC00), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Live Mining", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                MiningStat("Height", chainHeight.toString())
                MiningStat("Reward", String.format(Locale.US, "%.4f", blockReward))
                MiningStat("PoW", "$difficulty bits")
                MiningStat("Diff", difficulty.toString())
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = String.format(Locale.US, "Network total mined: %.1f Brahma Coin", totalMined),
                style = MaterialTheme.typography.labelSmall,
                color = Color.LightGray
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { showBurnDialog = true },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5544), contentColor = Color.White)
    ) {
        Text("Burn Brahma Coin", fontWeight = FontWeight.Bold)
    }
    Text(
        "Burns leave circulation permanently. A send larger than 10% of circulating supply also burns 25% of that send.",
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray
    )

    if (showBurnDialog) {
        AlertDialog(
            onDismissRequest = { showBurnDialog = false },
            title = { Text("Burn Brahma Coin") },
            text = {
                Column {
                    Text(
                        "This permanently destroys the amount you enter. It cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = burnAmount,
                        onValueChange = { burnAmount = it },
                        label = { Text("Amount") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = burnAmount.toDoubleOrNull()
                        if (amount != null && amount > 0.0) {
                            viewModel.burnBrahma(amount)
                        }
                        showBurnDialog = false
                        burnAmount = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF5544),
                        contentColor = Color.White
                    )
                ) {
                    Text("Burn")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBurnDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Spacer(Modifier.height(16.dp))

    Button(
        onClick = { viewModel.claimFaucet() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
    ) {
        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        Text("Claim adoption reward")
    }
    Text(
        text = "First 10k joins: 100 Brahma Coin + 50 to referrer. Next 25k: 50 + 25. Then half every 25k. Your tree: $referralTreeCount. Mine is how holdings grow after that.",
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray
    )
    if (queuedTx > 0) {
        Text(
            text = "$queuedTx signed tx(s) waiting on mesh (Bluetooth / Wi-Fi / LAN)",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF00FF88)
        )
    }
    Spacer(Modifier.height(24.dp))

    val context = LocalContext.current
    val inviteLink = remember(address, rpcUrl) { ShareActions.inviteMessage(address, rpcUrl) }
    val meshInvite by SentinelRuntime.lastInvite.collectAsState()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00FF88).copy(alpha = 0.1f)),
        onClick = { viewModel.shareInvite(context) }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Share invite link", fontWeight = FontWeight.Bold, color = Color(0xFF00FF88))
                Text(
                    text = inviteLink ?: "Create a checksummed wallet to get a link",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Icon(Icons.Default.Share, null, tint = Color(0xFF00FF88))
        }
    }
    if (meshInvite != null && meshInvite!!.ref != address) {
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val claim = meshInvite!!.claim
                if (CryptoManager.isSafeClaimUrl(claim) &&
                    CryptoManager.claimHostMatchesRpc(claim, viewModel.shareNodeBase())
                ) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(claim)))
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Open mesh invite · ${meshInvite!!.ref.take(16)}…")
        }
    }
}

@Composable
private fun MiningStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
