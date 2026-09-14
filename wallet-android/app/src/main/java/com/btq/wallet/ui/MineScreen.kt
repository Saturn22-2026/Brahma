package com.btq.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.btq.wallet.viewmodel.WalletViewModel

@Composable
fun ColumnScope.MineScreen(viewModel: WalletViewModel) {
    Text("Sovereign Mining", style = MaterialTheme.typography.headlineSmall)
    Text("Participate in network security for rewards.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    
    Spacer(Modifier.height(32.dp))
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111))
    ) {
        Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Construction, null, tint = Color(0xFF00FF88), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("Proof of Work (Adaptive)", fontWeight = FontWeight.Bold, color = Color.White)
            Text("Reward: 0.1 BTQ per block", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
    
    Spacer(Modifier.height(24.dp))
    
    Button(
        onClick = { viewModel.mineBTQ() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
    ) {
        Text("Start Local Mine")
    }
    
    Spacer(Modifier.height(16.dp))
    Text("Note: After 500 global users, mobile mining will be disabled and moved to separate full nodes to maintain decentralization.", 
        style = MaterialTheme.typography.labelSmall, color = Color.Gray, textAlign = TextAlign.Center)
}
