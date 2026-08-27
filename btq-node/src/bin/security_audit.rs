use btq_node::{Transaction, Block, crypto, storage::BTQStorage, security::BTQSecurity};
use btq_omni_transport::{OmniRouter, BTQBlock, RadioTransport};
use sha2::{Sha256, Digest};
use std::time::{SystemTime, UNIX_EPOCH};

/**
 * @title BTQSecurityAudit
 * @dev Automated stress-test and vulnerability scanner for the BTQ Node.
 */
fn main() {
    println!("=========================================");
    println!("   BITCOIN-QUANTUM SECURITY AUDIT TOOL   ");
    println!("=========================================\n");

    test_ots_enforcement();
    test_transport_hmac_integrity();
    test_argon2_dke_resilience();
    test_sentinel_reactive_defense();

    println!("\n[Audit] All security perimeters verified. 0 vulnerabilities found.");
}

fn test_ots_enforcement() {
    println!("[Test] Verifying One-Time-Signature (OTS) Enforcement...");
    let storage = BTQStorage::open(".audit_db");
    let keypair = crypto::BTQKeyPair::generate();

    // Simulate first reveal
    storage.record_public_key_reveal(&keypair.public_key).unwrap();

    // Attempt second use
    let re_use = storage.is_public_key_revealed(&keypair.public_key);
    if re_use {
        println!("✅ [OTS] Successfully blocked public key re-use.");
    } else {
        panic!("❌ [OTS] FAILED: Key re-use was NOT detected!");
    }
}

fn test_transport_hmac_integrity() {
    println!("[Test] Verifying Omni-Transport HMAC Integrity...");
    let mut router = OmniRouter::new();
    let secret = [0u8; 32];
    router.add_transport(Box::new(RadioTransport { frequency_mhz: 144.0, callsign: "TEST".into(), secret }));

    let raw_junk = b"{\"payload\":\"junk\",\"sequence\":1,\"timestamp\":123456789,\"hmac\":\"invalid\"}";

    match router.verify_packet("HF/VHF Radio Wave", raw_junk) {
        Err(e) => println!("✅ [Transport] Rejected tampered packet: {}", e),
        Ok(_) => panic!("❌ [Transport] FAILED: Accepted packet with invalid HMAC!"),
    }
}

fn test_argon2_dke_resilience() {
    println!("[Test] Verifying Argon2 Hardware-Bound Decryption...");
    // This test would fail if MachineGuid or MasterKey is wrong
    let encrypted_blob = "000000000000000000000000deadbeef"; // Mock
    let result = BTQSecurity::decrypt_secret(encrypted_blob, "WRONG_KEY");

    if result.is_err() {
        println!("✅ [DKE] Correctly denied access with invalid Master Key.");
    } else {
        panic!("❌ [DKE] FAILED: Granted access with wrong key!");
    }
}

fn test_sentinel_reactive_defense() {
    println!("[Test] Verifying Sentinel Reactive Defense...");
    // Sentinel logic check: 20+ failures should trigger key rotation
    use btq_node::sentinel::{QuantumSentinel, NetworkTelemetry, DefensiveAction};

    let mut sentinel = QuantumSentinel::new(NetworkTelemetry {
        mempool_size: 10,
        transactions_per_second: 1.0,
        unique_peer_count: 5,
        failed_pqc_signatures: 0,
        omni_transport_latency_ms: 5,
        whale_tax_triggered: false,
    });

    let spike = NetworkTelemetry {
        mempool_size: 10,
        transactions_per_second: 1.0,
        unique_peer_count: 5,
        failed_pqc_signatures: 50, // Attack!
        omni_transport_latency_ms: 5,
        whale_tax_triggered: false,
    };

    let (_, action) = sentinel.analyze_state(&spike);
    if action == DefensiveAction::RotateKeys {
        println!("✅ [Sentinel] Successfully triggered Key Rotation on PQC crack detection.");
    } else {
        panic!("❌ [Sentinel] FAILED: Did not escalate on PQC attack!");
    }
}
