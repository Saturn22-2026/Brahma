use pqc_dilithium::*;
use sha2::{Sha256, Digest};

/**
 * @title BTQCrypto
 * @dev Native Post-Quantum Cryptography (ML-DSA) implementation for BTQ.
 */
pub struct BTQKeyPair {
    pub public_key: [u8; 1312], // Dilithium2 public key size
    pub secret_key: [u8; 2528], // Dilithium2 secret key size
}

impl BTQKeyPair {
    pub fn generate() -> Self {
        let keys = Keypair::generate();
        BTQKeyPair {
            public_key: keys.public,
            secret_key: keys.secret,
        }
    }

    pub fn sign(&self, message: &[u8]) -> Vec<u8> {
        // Sign the message using Dilithium2
        let sig = sign(&message, &self.secret_key).expect("Signing failed");
        sig.to_vec()
    }
}

pub fn verify_pqc_signature(public_key: &[u8], message: &[u8], signature: &[u8]) -> bool {
    // Verify the ML-DSA signature
    // Dilithium2 signature size is 2420 bytes
    if signature.len() != 2420 || public_key.len() != 1312 {
        return false;
    }

    let mut sig_arr = [0u8; 2420];
    sig_arr.copy_from_slice(signature);

    let mut pub_arr = [0u8; 1312];
    pub_arr.copy_from_slice(public_key);

    verify(message, &sig_arr, &pub_arr).is_ok()
}

pub fn derive_address(public_key: &[u8]) -> String {
    let mut hasher = Sha256::new();
    hasher.update(public_key);
    let result = hasher.finalize();
    format!("0x{}", hex::encode(&result[0..20])) // Take first 20 bytes like Ethereum
}
