use pqc_dilithium::*;
use sha2::{Sha256, Digest};
use hex;

fn main() {
    println!("=========================================");
    println!("BTQ NATIVE PQC WALLET GENERATOR");
    println!("Algorithm: CRYSTALS-Dilithium2 (NIST L2)");
    println!("=========================================\n");

    // 1. Generate Keypair
    let keys = Keypair::generate();

    // 2. Derive Address (SHA-256 of Public Key)
    let mut hasher = Sha256::new();
    hasher.update(keys.public);
    let result = hasher.finalize();
    let address = format!("0x{}", hex::encode(&result[0..20]));

    println!("Public Key (Hex):");
    println!("  {}\n", hex::encode(&keys.public[..64]));
    println!("Secret Key (Hex):");
    println!("  {}...\n", hex::encode(&keys.secret[..64]));
    println!("Sovereign Address:");
    println!("  {}\n", address);

    println!("-----------------------------------------");
    println!("✅ Wallet generated natively via pqc_dilithium.");
    println!("⚠️  Backup your Secret Key securely!");
}
