package com.btq.wallet.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.btq.wallet.viewmodel.WalletViewModel

@Composable
fun ColumnScope.ReceiveScreen(viewModel: WalletViewModel) {
    val address by viewModel.address.collectAsState()
    val context = LocalContext.current
    val shareIdentity = {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "My Bitcoin-Quantum PQC Address: $address")
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    Text("Receive Assets", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(32.dp))
    
    Box(
        modifier = Modifier
            .size(200.dp)
            .background(Color.White)
            .align(Alignment.CenterHorizontally)
    ) {
        // QR Code Placeholder
        Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.fillMaxSize().padding(16.dp), tint = Color.Black)
    }
    
    Spacer(Modifier.height(32.dp))
    Text("Your Sovereign Address:", color = Color.Gray)
    Card(onClick = shareIdentity, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer {
                Text(address, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            }
            Icon(Icons.Default.Share, null, tint = Color(0xFF00FF88))
        }
    }
    Text("Tap card to share address", style = MaterialTheme.typography.labelSmall, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
}
