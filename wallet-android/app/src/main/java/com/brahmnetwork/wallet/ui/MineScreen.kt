package com.brahmnetwork.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brahmnetwork.wallet.viewmodel.WalletViewModel
import java.util.Locale

@Composable
fun ColumnScope.MineScreen(viewModel: WalletViewModel) {
    val status by viewModel.status.collectAsState()
    val mineStatus by viewModel.mineStatus.collectAsState()
    val mining by viewModel.mining.collectAsState()
    val balance by viewModel.balance.collectAsState()
    val chainHeight by viewModel.chainHeight.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val blockReward by viewModel.blockReward.collectAsState()
    val totalMined by viewModel.totalMined.collectAsState()
    val firstMinePending by viewModel.firstMinePending.collectAsState()
    val joinedCount by viewModel.joinedCount.collectAsState()
    val hideL1Balances by viewModel.hideL1Balances.collectAsState()

    Text("Sovereign Mining", style = MaterialTheme.typography.headlineSmall)
    Text("Phone PoW seals the next block. No internet: the signed block rides Bluetooth, Wi-Fi Aware, and LAN mesh until a node is in range.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Text("Status: $status", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00FF88))
    if (mineStatus.isNotBlank()) {
        Text(mineStatus, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
    }
    Text(
        text = if (hideL1Balances) "Wallet: ••••" else "Wallet: $balance Brahma Coin",
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White
    )
    Text("Height $chainHeight  ·  PoW $difficulty bits  ·  $joinedCount joined", style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
    Text(
        text = String.format(Locale.US, "Next mine %.4f Brahma Coin  ·  mined %.4f Brahma Coin", blockReward, totalMined),
        style = MaterialTheme.typography.bodySmall,
        color = Color.LightGray
    )

    Spacer(Modifier.height(32.dp))

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Construction, null, tint = Color(0xFF00FF88), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Proof of Work (~8s blocks)", fontWeight = FontWeight.Bold, color = Color.White)
            Text(
                text = String.format(Locale.US, "This block: %.4f Brahma Coin", blockReward),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            if (firstMinePending) {
                Text("Includes one-time +1 Brahma Coin first-mine bonus", style = MaterialTheme.typography.labelSmall, color = Color(0xFF00FF88))
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = { viewModel.mineBrahma() },
        enabled = !mining,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
    ) {
        Text(if (mining) "Mining…" else "Start Local Mine")
    }

    Spacer(Modifier.height(16.dp))
    Text(
        "0.00014 Brahma Coin/block for the first 10,000 users, then half every 100,000 users. Yearly mine cap stays 5M Brahma Coin; unused year stays in the mining pool.",
        style = MaterialTheme.typography.labelSmall,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
}
