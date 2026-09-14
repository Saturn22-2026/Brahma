// src/mining.rs
pub struct BlockSimulation {
    pub data: String,
    pub difficulty: usize, // number of leading zeros required
}

impl BlockSimulation {
    pub fn verify_nonce(&self, nonce: u64) -> bool {
        use sha2::{Sha256, Digest};
        let mut hasher = Sha256::new();
        hasher.update(format!("{}{}", self.data, nonce).as_bytes());
        let result = format!("{:x}", hasher.finalize());
        result.starts_with(&"0".repeat(self.difficulty))
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_mining_verification_success() {
        let sim = BlockSimulation {
            data: "genesis_block".to_string(),
            difficulty: 2,
        };

        // Brute-force a valid nonce purely in test context to verify logic
        let mut valid_nonce = 0;
        for nonce in 0..100000 {
            if sim.verify_nonce(nonce) {
                valid_nonce = nonce;
                break;
            }
        }
        assert!(sim.verify_nonce(valid_nonce));
    }
}
