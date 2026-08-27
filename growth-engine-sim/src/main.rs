use std::collections::HashMap;
use chrono::Utc;
use rand::seq::SliceRandom;

// --- PQC Wallet Structure ---
#[derive(Clone, Debug)]
pub struct PQCWallet {
    pub address: String,
    pub btq_balance: f64,
    pub meme_coins: HashMap<String, f64>,
    pub referrals: u32,
    pub last_faucet_claim: i64,
}

// --- The Growth Engine ---
pub struct BTQGrowthEngine {
    pub wallets: HashMap<String, PQCWallet>,
    pub faucet_pool: f64,
    pub meme_coin_tickers: Vec<String>,
}

impl BTQGrowthEngine {
    pub fn new() -> Self {
        BTQGrowthEngine {
            wallets: HashMap::new(),
            faucet_pool: 1_000_000.0, // 1M BTQ allocated for faucet growth
            meme_coin_tickers: vec![
                "HOMIE".to_string(), "SLUMDOG".to_string(), "CGOD".to_string(),
                "BOUJIE".to_string(), "QMILE".to_string(), "SOF".to_string(),
                "5THAV".to_string(), "POOKIE".to_string()
            ],
        }
    }

    // Algorithm Step 1: Wallet Generation
    pub fn generate_wallet(&mut self, user_id: &str) -> String {
        // In production, this would generate real ML-DSA (Dilithium) keys.
        // For simulation, we hash the user_id to act as the PQC address.
        let address = format!("BTQ1{}", &user_id[hash(user_id)][..16]);

        let wallet = PQCWallet {
            address: address.clone(),
            btq_balance: 0.0,
            meme_coins: HashMap::new(),
            referrals: 0,
            last_faucet_claim: 0,
        };

        self.wallets.insert(address.clone(), wallet);
        println!("🔐 New PQC Wallet generated: {}", address);
        address
    }

    // Algorithm Step 2 & 3: Faucet Claim & Meme Roulette
    pub fn claim_faucet(&mut self, address: &str, referrer: Option<String>) -> bool {
        let wallet = match self.wallets.get_mut(address) {
            Some(w) => w,
            None => return false,
        };

        let now = Utc::now().timestamp();
        let cooldown = 86400; // 24 hours

        // Base claim
        if now - wallet.last_faucet_claim < cooldown && wallet.last_faucet_claim != 0 {
            println!("⏳ Faucet cooldown active for {}", address);
            return false;
        }

        // Algorithm Step 5: Referral Multiplier
        let mut claim_amount = 10.0; // Base 10 BTQ
        if let Some(ref_addr) = referrer.clone() {
            if let Some(ref_wallet) = self.wallets.get_mut(&ref_addr) {
                ref_wallet.referrals += 1;
                ref_wallet.btq_balance += 50.0; // Referrer gets 50 BTQ instantly
                self.faucet_pool -= 50.0;
                println!("🤝 Referral success! {} earned 50 BTQ for inviting {}", ref_addr, address);
                claim_amount = 25.0; // New user gets bonus for using referral
            }
        }

        wallet.btq_balance += claim_amount;
        wallet.last_faucet_claim = now;
        self.faucet_pool -= claim_amount;

        // Algorithm Step 3: Meme Coin Roulette Drop
        let meme = self.meme_coin_tickers.choose(&mut rand::thread_rng()).unwrap();
        let meme_amount = 1000.0;
        *wallet.meme_coins.entry(meme.clone()).or_insert(0.0) += meme_amount;

        println!("💧 Faucet Claimed: {} got {} BTQ and {} ${}", address, claim_amount, meme_amount, meme);
        true
    }

    // Algorithm Step 6: AI Airdrop to active network participants
    pub fn ai_distribute_meme_coins(&mut self) {
        println!("\n🤖 AI Agent scanning network for high-value wallets to reward...");
        let mut distributions = vec![];

        for (addr, wallet) in self.wallets.iter() {
            // AI Logic: If wallet has > 2 referrals, airdrop extra meme coins
            if wallet.referrals >= 2 {
                let reward_ticker = self.meme_coin_tickers.choose(&mut rand::thread_rng()).unwrap();
                let reward_amount = 5000.0;
                distributions.push((addr.clone(), reward_ticker.clone(), reward_amount));
            }
        }

        for (addr, ticker, amount) in distributions {
            if let Some(w) = self.wallets.get_mut(&addr) {
                *w.meme_coins.entry(ticker.clone()).or_insert(0.0) += amount;
                println!("🪂 AI Airdropped {} ${} to {} (High referral count)", amount, ticker, addr);
            }
        }
    }

    // Print the global state of the growth engine
    pub fn print_ecosystem_stats(&self) {
        println!("\n=== BTQ Ecosystem Stats ===");
        println!("Total Wallets: {}", self.wallets.len());
        println!("Faucet Pool Remaining: {} BTQ", self.faucet_pool);
        println!("---------------------------");
        for (addr, wallet) in &self.wallets {
            println!("Address: {} | BTQ: {:.2} | Refs: {} | Memes: {:?}",
                addr, wallet.btq_balance, wallet.referrals, wallet.meme_coins);
        }
        println!("===========================\n");
    }
}

// Simple hash function for address generation simulation
fn hash(s: &str) -> String {
    format!("{:x}", s.bytes().fold(0u64, |acc, b| acc.wrapping_mul(31).wrapping_add(b as u64)))
}

fn main() {
    let mut engine = BTQGrowthEngine::new();

    println!("🚀 Initializing BTQ Viral Growth Engine Algorithm...\n");

    // 1. Organic user discovers the project
    let alice = engine.generate_wallet("alice_email");
    engine.claim_faucet(&alice, None);

    // 2. Alice shares her referral link
    let bob = engine.generate_wallet("bob_email");
    engine.claim_faucet(&bob, Some(alice.clone()));

    // 3. Bob shares his link
    let charlie = engine.generate_wallet("charlie_email");
    engine.claim_faucet(&charlie, Some(bob.clone()));

    // 4. Charlie shares link back to Alice (completing a loop)
    // Alice already claimed, but her wallet is marked as highly active
    let dave = engine.generate_wallet("dave_email");
    engine.claim_faucet(&dave, Some(alice.clone()));

    // 5. The AI Agent wakes up and rewards the most active users (Alice & Bob)
    engine.ai_distribute_meme_coins();

    // Final Stats
    engine.print_ecosystem_stats();
}
