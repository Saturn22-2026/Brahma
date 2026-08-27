// SPDX-License-Identifier: GPL-3.0-only
use async_trait::async_trait;
use serde::{Serialize, Deserialize};
use std::error::Error;
use std::sync::Arc;

// --- BTQ Block Structure (Simplified for transport) ---
#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct BTQBlock {
    pub index: u64,
    pub previous_hash: String,
    pub miner: String,
    pub transactions: Vec<String>,
}

// --- The Transport Abstraction Layer ---
#[async_trait]
pub trait PhysicalTransport: Send + Sync {
    fn name(&self) -> &str;

    // Initializes the hardware/interface
    async fn connect(&mut self) -> Result<(), Box<dyn Error + Send + Sync>>;

    // Transmits raw bytes over the physical medium
    async fn transmit(&self, data: &[u8]) -> Result<(), Box<dyn Error + Send + Sync>>;

    // Listens for incoming raw bytes from the physical medium
    async fn receive(&self) -> Result<Vec<u8>, Box<dyn Error + Send + Sync>>;
}

// --- 1. Radio Wave Transport (Packet Radio / AX.25) ---
pub struct RadioTransport {
    pub frequency_mhz: f64,
    pub callsign: String,
}

#[async_trait]
impl PhysicalTransport for RadioTransport {
    fn name(&self) -> &str { "HF/VHF Radio Wave" }

    async fn connect(&mut self) -> Result<(), Box<dyn Error + Send + Sync>> {
        println!("[Radio] Tuning to {} MHz | Callsign: {}", self.frequency_mhz, self.callsign);
        Ok(())
    }

    async fn transmit(&self, data: &[u8]) -> Result<(), Box<dyn Error + Send + Sync>> {
        println!("[Radio] Transmitting {} bytes via 1200 baud AFSK on {}", data.len(), self.frequency_mhz);
        Ok(())
    }

    async fn receive(&self) -> Result<Vec<u8>, Box<dyn Error + Send + Sync>> {
        Ok(vec![])
    }
}

// --- 2. Satellite Transport (e.g., Swarm/Iridium) ---
pub struct SatelliteTransport {
    pub imei: String,
    pub api_endpoint: String,
}

#[async_trait]
impl PhysicalTransport for SatelliteTransport {
    fn name(&self) -> &str { "LEO Satellite" }

    async fn connect(&mut self) -> Result<(), Box<dyn Error + Send + Sync>> {
        println!("[Satellite] Modem {} registered to network.", self.imei);
        Ok(())
    }

    async fn transmit(&self, data: &[u8]) -> Result<(), Box<dyn Error + Send + Sync>> {
        println!("[Satellite] Uploading {} bytes to LEO constellation.", data.len());
        Ok(())
    }

    async fn receive(&self) -> Result<Vec<u8>, Box<dyn Error + Send + Sync>> {
        Ok(vec![])
    }
}

// --- The Omni-Channel Router ---
pub struct OmniRouter {
    pub transports: Vec<Arc<tokio::sync::Mutex<dyn PhysicalTransport>>>,
}

impl OmniRouter {
    pub fn new() -> Self {
        Self { transports: vec![] }
    }

    pub fn add_transport(&mut self, transport: Box<dyn PhysicalTransport>) {
        self.transports.push(Arc::new(tokio::sync::Mutex::new(transport)));
    }

    pub async fn broadcast_block(&self, block: &BTQBlock) {
        let serialized = serde_json::to_vec(block).expect("Failed to serialize block");

        let mut tasks = vec![];
        for transport in &self.transports {
            let data = serialized.clone();
            let t = Arc::clone(transport);

            tasks.push(tokio::spawn(async move {
                let lock = t.lock().await;
                println!("Dispatching to {}...", lock.name());
                let _ = lock.transmit(&data).await;
            }));
        }

        for task in tasks {
            let _ = task.await;
        }
    }
}

// Implement Clone manually for OmniRouter by cloning the Arcs
impl Clone for OmniRouter {
    fn clone(&self) -> Self {
        OmniRouter {
            transports: self.transports.iter().map(|t| Arc::clone(t)).collect(),
        }
    }
}
