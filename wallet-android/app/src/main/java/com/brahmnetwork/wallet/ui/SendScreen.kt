package com.brahmnetwork.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.brahmnetwork.wallet.viewmodel.WalletViewModel
import java.math.BigDecimal
import java.math.RoundingMode

@Composable
fun ColumnScope.SendScreen(viewModel: WalletViewModel) {
    val status by viewModel.status.collectAsState()
    val balance by viewModel.balance.collectAsState()
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }

    Text("Send Brahma Coin", style = MaterialTheme.typography.headlineSmall)
    Text("Status: $status", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00FF88))
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
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(25 to "25%", 50 to "50%", 75 to "75%", 100 to "Max").forEach { (pct, label) ->
            OutlinedButton(
                onClick = { amount = shareOf(balance, pct) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 4.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF88))
            ) {
                Text(label)
            }
        }
    }
    
    Spacer(Modifier.height(24.dp))
    Button(
        onClick = { viewModel.requestSendTokens(recipient, amount.toDoubleOrNull() ?: 0.0, 0) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
    ) {
        Text("Sign & Broadcast (Dilithium3)")
    }
}

private fun shareOf(balance: Double, percent: Int): String {
    if (balance <= 0.0) return ""
    return BigDecimal.valueOf(balance)
        .multiply(BigDecimal(percent))
        .divide(BigDecimal(100))
        .setScale(8, RoundingMode.DOWN)
        .stripTrailingZeros()
        .toPlainString()
}
