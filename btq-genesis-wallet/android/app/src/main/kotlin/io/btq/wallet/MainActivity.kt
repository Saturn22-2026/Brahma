package io.btq.wallet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope

class MainActivity : ComponentActivity() {
    private lateinit var engine: WalletEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = WalletEngine(lifecycleScope)
        engine.initialize()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    WalletDashboard(engine)
                }
            }
        }
    }
}

@Composable
fun WalletDashboard(engine: WalletEngine) {
    val state by engine.state.collectAsState()

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            "BTQ GENESIS WALLET",
            color = Color.Cyan,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Node: ${state.address}",
            color = Color.LightGray,
            fontSize = 12.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("BALANCE", color = Color.Gray, fontSize = 12.sp)
                Text("${state.balance} BTQ", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                
                if (state.airdrops.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Assets:", color = Color.Gray, fontSize = 12.sp)
                    state.airdrops.forEach { (coin, amt) ->
                        Text("$amt \$$coin", color = Color(0xFFBB86FC), fontSize = 14.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Text("SENTINEL AI LOGS", color = Color.Gray, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(8.dp)
        ) {
            items(state.logs) { log ->
                Text(
                    log,
                    color = when {
                        log.contains("⛏️") -> Color.Yellow
                        log.contains("🪂") -> Color.Magenta
                        log.contains("💧") -> Color.Cyan
                        else -> Color.Green
                    },
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
