package com.btq.wallet.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.btq.wallet.viewmodel.WalletViewModel

@Composable
fun OnboardingScreen(viewModel: WalletViewModel) {
    var recoveryPhrase by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showRecoverDialog by remember { mutableStateOf(false) }
    val status by viewModel.status.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFF00FF88), modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(24.dp))
        Text("Welcome to Bitcoin-Quantum", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text("The future of post-quantum sovereignty.", style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
        
        Spacer(Modifier.height(16.dp))
        Text("Node Status: $status", style = MaterialTheme.typography.labelSmall, color = if (status.contains("Error")) Color.Red else Color(0xFF00FF88))

        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = { showCreateDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
        ) {
            Text("Create New Sovereign Identity")
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("OR", color = Color.DarkGray, fontWeight = FontWeight.Bold)
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = recoveryPhrase,
            onValueChange = { recoveryPhrase = it },
            label = { Text("Enter Existing Seed Phrase", color = Color(0xFF00FF88)) },
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
        
        Spacer(Modifier.height(8.dp))
        
        TextButton(
            onClick = { if (recoveryPhrase.isNotEmpty()) showRecoverDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Recover Existing Identity", color = Color.White)
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Choose Storage Mode") },
            text = { Text("Standard mode (subject to 14-day sweep) or Hardware Cold Storage (Exempt).") },
            confirmButton = {
                TextButton(onClick = { viewModel.createWallet(true, true); showCreateDialog = false }) {
                    Text("Cold Storage")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.createWallet(true, false); showCreateDialog = false }) {
                    Text("Standard")
                }
            }
        )
    }

    if (showRecoverDialog) {
        AlertDialog(
            onDismissRequest = { showRecoverDialog = false },
            title = { Text("Recover Storage Mode") },
            text = { Text("Choose how you want to store this recovered identity. Cold Storage is recommended for long-term security.") },
            confirmButton = {
                TextButton(onClick = { viewModel.recoverWallet(recoveryPhrase, true); showRecoverDialog = false }) {
                    Text("Cold Storage")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.recoverWallet(recoveryPhrase, false); showRecoverDialog = false }) {
                    Text("Standard")
                }
            }
        )
    }
}
