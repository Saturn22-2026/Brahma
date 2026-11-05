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
    val tokens by viewModel.tokens.collectAsState()
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var assetId by remember { mutableIntStateOf(0) }
    var assetMenu by remember { mutableStateOf(false) }
    val choices = listOf(0 to "Brahma Coin") + tokens.map { it.id to it.name }
    val assetName = choices.firstOrNull { it.first == assetId }?.second ?: "Brahma Coin"
    val assetBalance = if (assetId == 0) {
        balance
    } else {
        tokens.firstOrNull { it.id == assetId }?.amount ?: 0.0
    }

    Text("Send", style = MaterialTheme.typography.headlineSmall)
    Text("Status: $status", style = MaterialTheme.typography.bodySmall, color = Color(0xFF00FF88))
    Spacer(Modifier.height(16.dp))
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { assetMenu = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00FF88))
        ) {
            Text(assetName)
        }
        DropdownMenu(expanded = assetMenu, onDismissRequest = { assetMenu = false }) {
            choices.forEach { (id, name) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        assetId = id
                        amount = ""
                        assetMenu = false
                    }
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
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
                onClick = { amount = shareOf(assetBalance, pct, whole = assetId != 0) },
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
        onClick = { viewModel.requestSendTokens(recipient, amount.toDoubleOrNull() ?: 0.0, assetId) },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
    ) {
        Text("Sign & Broadcast (Dilithium3)")
    }
}

private fun shareOf(balance: Double, percent: Int, whole: Boolean): String {
    if (balance <= 0.0) return ""
    val scale = if (whole) 0 else 8
    return BigDecimal.valueOf(balance)
        .multiply(BigDecimal(percent))
        .divide(BigDecimal(100))
        .setScale(scale, RoundingMode.DOWN)
        .stripTrailingZeros()
        .toPlainString()
}
