package com.btq.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.btq.wallet.viewmodel.WalletViewModel
import com.btq.wallet.viewmodel.TokenBalance

@Composable
fun ColumnScope.SendScreen(viewModel: WalletViewModel) {
    val tokens by viewModel.tokens.collectAsState()
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedAssetId by remember { mutableStateOf(0) }

    Text("Send Assets", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(16.dp))
    
    Text("Select Asset", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
    // Simplified selector
    ScrollableTabRow(selectedTabIndex = selectedAssetId, edgePadding = 0.dp, containerColor = Color.Transparent) {
        Tab(selected = selectedAssetId == 0, onClick = { selectedAssetId = 0 }, text = { Text("BTQ") })
        tokens.forEach { token ->
            Tab(selected = selectedAssetId == token.id, onClick = { selectedAssetId = token.id }, text = { Text(token.symbol) })
        }
    }

    Spacer(Modifier.height(16.dp))
    OutlinedTextField(
        value = recipient, 
        onValueChange = { recipient = it }, 
        label = { Text("Recipient Address") }, 
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF00FF88),
            unfocusedBorderColor = Color.Gray
        )
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = amount, 
        onValueChange = { amount = it }, 
        label = { Text("Amount") }, 
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = Color(0xFF00FF88),
            unfocusedBorderColor = Color.Gray
        )
    )
    
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = { viewModel.requestSendTokens(selectedAssetId, recipient, amount.toDoubleOrNull() ?: 0.0) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
    ) {
        Text("Sign & Broadcast (Dilithium3)")
    }
}
