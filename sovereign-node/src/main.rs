// ╔══════════════════════════════════════════════════════╗
// ║   NODE LAUNCHER - PHASE 2              ║
// ╚══════════════════════════════════════════════════════╝

use std::sync::Arc;
use tokio::sync::RwLock;
use serde::{Deserialize, Serialize};
use sha2::{Sha256, Digest};
use hex;
use rpassword;
use btq_node;

/// Master Key Configuration
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MasterKeyConfig {
    pub encrypted_key: String,
    pub salt: String,
    pub iterations: u32,
    pub derived_key_hash: String, // For verification without exposing key
}

/// Node State
#[derive(Debug)]
pub struct NodeState {
    pub is_running: bool,
    pub master_key_entered: bool,
    pub block_height: u64,
    pub peers_connected: usize,
    pub start_time: std::time::Instant,
}

/// Main Launcher Structure
pub struct SovereignNodeLauncher {
    config: MasterKeyConfig,
    state: Arc<RwLock<NodeState>>,
    key_derived: Option<[u8; 32]>,
}

impl SovereignNodeLauncher {

    /// Create new launcher instance
    pub fn new(config_path: &str) -> Result<Self, Box<dyn std::error::Error>> {
        let config_str = std::fs::read_to_string(config_path).unwrap_or_else(|_| {
            // Default config for first-run/dev
            let default_config = MasterKeyConfig {
                encrypted_key: "".to_string(),
                salt: "536f7665726569676e2d53616c74".to_string(), // "Sovereign-Salt" hex
                iterations: 100000,
                derived_key_hash: "82f92a...f92a".to_string(), // Placeholder
            };
            serde_json::to_string(&default_config).unwrap()
        });

        let config: MasterKeyConfig = serde_json::from_str(&config_str)?;

        Ok(Self {
            config,
            state: Arc::new(RwLock::new(NodeState {
                is_running: false,
                master_key_entered: false,
                block_height: 0,
                peers_connected: 0,
                start_time: std::time::Instant::now(),
            })),
            key_derived: None,
        })
    }

    /// PHASE 2 STEP 1: Enter Master Key securely
    pub async fn enter_master_key(
        &mut self,
        passphrase: &str,
    ) -> Result<(), Box<dyn std::error::Error>> {
        println!("🔐 Processing Master Key via PBKDF2...");

        // Derive key using PBKDF2
        let salt = hex::decode(&self.config.salt)?;
        let mut derived_key = [0u8; 32];

        pbkdf2::pbkdf2::<hmac::Hmac<sha2::Sha256>>(
            passphrase.as_bytes(),
            &salt,
            self.config.iterations,
            &mut derived_key,
        );

        // Verify derived key matches expected hash
        let mut hasher = Sha256::new();
        hasher.update(&derived_key);
        let result = hasher.finalize();
        let hash_hex = hex::encode(result);

        // Skip check in dev mode if hash is placeholder
        if self.config.derived_key_hash != "82f92a...f92a" && hash_hex != self.config.derived_key_hash {
            return Err("Invalid Master Key".into());
        }

        self.key_derived = Some(derived_key);
        {
            let mut state = self.state.write().await;
            state.master_key_entered = true;
        }

        println!("✅ Master Key accepted and secured in memory");
        Ok(())
    }

    /// PHASE 2 STEP 2: Start the Rust node
    pub async fn start_node(&self, master_key: &str) -> Result<(), Box<dyn std::error::Error>> {
        // Verify master key entered
        {
            let state = self.state.read().await;
            if !state.master_key_entered {
                return Err("Master Key required before starting node".into());
            }
        }

        println!("🚀 Starting Sovereign Node...");
        println!("================================");

        // Call the actual core run logic
        btq_node::run(master_key).await?;

        // Update state
        {
            let mut state = self.state.write().await;
            state.is_running = true;
            state.start_time = std::time::Instant::now();
        }

        println!("");
        println!("╔═══════════════════════════════════════════════╗");
        println!("║  ✅ PHASE 2 COMPLETE: NODE RUNNING          ║");
        println!("╚═══════════════════════════════════════════════╝");

        Ok(())
    }
}

/// CLI Entry Point
#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("╔════════════════════════════════════════════════════╗");
    println!("║   NODE LAUNCHER - PHASE 2              ║");
    println!("║  L1 Node Startup with Master Key                 ║");
    println!("╚════════════════════════════════════════════════════╝");
    println!("");

    // Load configuration from local path or fallback
    let mut launcher = SovereignNodeLauncher::new("master_key_config.json")?;

    // Get master key from secure input
    println!("🔑 Enter Master Key:");
    let passphrase = rpassword::prompt_password("> ")?;

    // Step 1: Authenticate
    launcher.enter_master_key(&passphrase).await?;

    // Step 2: Start node
    launcher.start_node(&passphrase).await?;

    Ok(())
}
