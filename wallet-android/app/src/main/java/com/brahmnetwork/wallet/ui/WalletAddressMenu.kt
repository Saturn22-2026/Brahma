package com.brahmnetwork.wallet.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.brahmnetwork.wallet.CryptoManager
import com.brahmnetwork.wallet.viewmodel.WalletViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletAddressMenu(viewModel: WalletViewModel, modifier: Modifier = Modifier) {
    val slots by viewModel.walletSlots.collectAsState()
    val active by viewModel.address.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    val shown = active.ifBlank { "Choose a wallet" }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = shown,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Wallet") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF00FF88),
                unfocusedBorderColor = Color(0xFF222222),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color(0xFF00FF88),
                unfocusedLabelColor = Color.Gray
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            slots.forEach { slot ->
                val current = CryptoManager.sameWalletAddress(slot, active)
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (current) "Active  $slot" else slot,
                            color = if (current) Color(0xFF00FF88) else Color.White
                        )
                    },
                    onClick = {
                        expanded = false
                        if (!current) viewModel.switchDeviceWallet(slot)
                    }
                )
            }
        }
    }
}
