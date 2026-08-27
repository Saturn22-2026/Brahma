use serde::{Serialize, Deserialize};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct FaucetVoucher {
    pub issuer: String,
    pub amount: f64,
    pub timestamp: u64,
    pub signature: String,
}

pub struct FaucetBeacon {
    pub node_id: String,
}

impl FaucetBeacon {
    pub fn new(node_id: String) -> Self {
        Self { node_id }
    }

    /// Generates a signed voucher for local sharing.
    pub fn create_voucher(&self, amount: f64) -> FaucetVoucher {
        let now = SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .expect("Time went backwards")
            .as_secs();

        FaucetVoucher {
            issuer: self.node_id.clone(),
            amount,
            timestamp: now,
            signature: format!("BEACON_SIG_{}", uuid::Uuid::new_v4()),
        }
    }

    /// Simulates Bluetooth LE advertising of the faucet.
    pub async fn start_bluetooth_ad(&self) {
        println!("[Omni-Transport] Starting Bluetooth LE Advertisement for BTQ Faucet...");
        // Logic to use btleplug to advertise a specific Service UUID
        // containing the voucher data or a pointer to it.
    }

    /// Simulates WiFi P2P (mDNS) discovery.
    pub async fn start_wifi_discovery(&self) {
        println!("[Omni-Transport] Starting WiFi P2P (mDNS) Beacon...");
        // Logic to use libp2p mdns to announce presence to local peers.
    }
}
