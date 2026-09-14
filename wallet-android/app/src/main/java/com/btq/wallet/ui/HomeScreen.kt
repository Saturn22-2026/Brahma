package com.btq.wallet.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.btq.wallet.viewmodel.WalletViewModel
import com.btq.wallet.viewmodel.TokenBalance

@Composable
fun ColumnScope.HomeScreen(viewModel: WalletViewModel) {
    val balance by viewModel.balance.collectAsState()
    val tokens by viewModel.tokens.collectAsState()
    val status by viewModel.status.collectAsState()
    val address by viewModel.address.collectAsState()
    val isHardwareCold by viewModel.isHardwareColdStorage.collectAsState()
    val totalUsers by viewModel.totalUsers.collectAsState()

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Bitcoin-Quantum Sovereign", style = MaterialTheme.typography.titleLarge, color = Color(0xFF00FF88), modifier = Modifier.weight(1f))
        if (isHardwareCold) {
            Surface(
                color = Color(0xFF00FF88).copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text("COLD", color = Color(0xFF00FF88), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Status: $status", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
    
    Spacer(Modifier.height(24.dp))
    
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Main Balance", color = Color.Gray)
            Text(
                text = "$balance BTQ", 
                style = MaterialTheme.typography.displayMedium, 
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }

    Spacer(Modifier.height(24.dp))

    if (totalUsers < 500) {
        Button(
            onClick = { viewModel.claimFaucet() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
        ) {
            Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Request Faucet Tokens")
        }
        Spacer(Modifier.height(24.dp))
    }

    // Exquisite Sharing Card (iPhone-inspired Growth Mechanic)
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF00FF88).copy(alpha = 0.1f)),
        onClick = {
            val msg = """
                🚀 Join the Bitcoin-Quantum Revolution! 
                
                Get free BTQ (Post-Quantum Secure) tokens from the Faucet.
                My Sovereign ID: $address
                
                Claim here: https://faucet.btq.li/claim?ref=$address
                Download Wallet: https://btq.li/download
                #PQC #BitcoinQuantum #SovereignIdentity
            """.trimIndent()

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, msg)
                type = "text/plain"
            }
            context.startActivity(Intent.createChooser(sendIntent, "Spread the Sovereign Word"))
        }
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Invite & Earn", fontWeight = FontWeight.Bold, color = Color(0xFF00FF88))
                Text("Share the faucet with your network", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(Icons.Default.Share, null, tint = Color(0xFF00FF88))
        }
    }

    Spacer(Modifier.height(24.dp))
    Text("L2 Assets", style = MaterialTheme.typography.titleMedium)
    
    if (tokens.isEmpty()) {
        Text("No L2 Assets discovered on your Sovereign ID.", 
            style = MaterialTheme.typography.bodySmall, color = Color.DarkGray, modifier = Modifier.padding(vertical = 12.dp))
    }

    tokens.forEach { token ->
        ListItem(
            headlineContent = { Text(token.symbol) },
            supportingContent = { Text(token.fiatValue) },
            trailingContent = { Text("${token.amount}", fontWeight = FontWeight.Bold) },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        HorizontalDivider(color = Color(0xFF222222))
    }
}
