package com.brahmnetwork.wallet.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.brahmnetwork.wallet.ShareActions
import com.brahmnetwork.wallet.viewmodel.WalletViewModel

@Composable
fun ColumnScope.ReceiveScreen(viewModel: WalletViewModel) {
    val address by viewModel.address.collectAsState()
    val status by viewModel.status.collectAsState()
    val rpcUrl by viewModel.rpcUrl.collectAsState()
    val context = LocalContext.current
    val live = status == "Live"
    val qr = remember(address) {
        if (address.isBlank()) null else ShareActions.qrBitmap(address)
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Share Wallet", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        Surface(
            color = if (live) Color(0xFF00FF88) else Color(0xFFFF5252),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (live) "LIVE" else status.ifBlank { "OFFLINE" },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("Scan or share your live Brahma Coin address.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    Spacer(Modifier.height(24.dp))

    Box(
        modifier = Modifier
            .size(220.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .align(Alignment.CenterHorizontally),
        contentAlignment = Alignment.Center
    ) {
        if (qr != null) {
            Image(bitmap = qr.asImageBitmap(), contentDescription = "Wallet QR", modifier = Modifier.fillMaxSize().padding(12.dp))
        } else {
            Text("No wallet", color = Color.Black)
        }
    }

    Spacer(Modifier.height(24.dp))
    Text("Your Sovereign Address:", color = Color.Gray)
    Card(
        onClick = { viewModel.shareInvite(context) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            SelectionContainer {
                Text(address, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), color = Color.White)
            }
            Icon(Icons.Default.Share, null, tint = Color(0xFF00FF88))
        }
    }
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Brahma Coin address", address))
            },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333))
        ) {
            Text("Copy")
        }
        Button(
            onClick = { viewModel.shareInvite(context) },
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
        ) {
            Text("Share link")
        }
    }
}
