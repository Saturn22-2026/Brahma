// src/keys.rs
pub fn generate_deterministic_key(seed: &str, nonce: u64) -> String {
    use sha2::{Sha256, Digest};
    let mut hasher = Sha256::new();
    hasher.update(format!("{}-{}", seed, nonce).as_bytes());
    format!("{:x}", hasher.finalize())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_key_length_and_hex() {
        let key = generate_deterministic_key("test_seed", 42);
        assert_eq!(key.len(), 64); // SHA-256 hex string is 64 chars
    }

    #[test]
    fn test_determinism() {
        let key1 = generate_deterministic_key("secure_seed", 1);
        let key2 = generate_deterministic_key("secure_seed", 1);
        assert_eq!(key1, key2);
    }
}
