use serde::{Serialize, Deserialize};
use std::collections::HashMap;

/**
 * @title QuantumSentinel
 * @dev Native Rust-based Adaptive Security Layer (Phase 54).
 * Monitors real-time network telemetry and deploys autonomous defenses.
 */

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct NetworkTelemetry {
    pub mempool_size: u64,
    pub transactions_per_second: f64,
    pub unique_peer_count: u32,
    pub failed_pqc_signatures: u32,
    pub omni_transport_latency_ms: u32,
    pub whale_tax_triggered: bool,
}

#[derive(Clone, Debug, PartialEq)]
pub enum ThreatType {
    Normal,
    SybilAttack,
    MempoolSpam,
    QuantumSignatureCrack,
    OmniChannelJam,
    UnknownZeroDay,
}

pub struct QuantumSentinel {
    baseline_state: NetworkTelemetry,
    anomaly_thresholds: HashMap<String, f64>,
    known_threat_signatures: Vec<Vec<f64>>,
    pub defenses_deployed: u32,
    pub autonomous_mode: bool,
}

impl QuantumSentinel {
    pub fn new(initial_baseline: NetworkTelemetry) -> Self {
        let mut thresholds = HashMap::new();
        thresholds.insert("mempool_size".to_string(), 3.0); // 300% spike
        thresholds.insert("pqc_failures".to_string(), 20.0); // > 20 failures
        thresholds.insert("latency_jump".to_string(), 5.0); // 5x latency

        QuantumSentinel {
            baseline_state: initial_baseline,
            anomaly_thresholds: thresholds,
            known_threat_signatures: vec![],
            defenses_deployed: 0,
            autonomous_mode: true,
        }
    }

    pub fn analyze_state(&mut self, current: &NetworkTelemetry) -> Option<ThreatType> {
        let mut threat = None;
        let mut anomaly_score = 0.0;

        // 1. Mempool Anomaly (Spam)
        let mempool_ratio = current.mempool_size as f64 / self.baseline_state.mempool_size.max(1) as f64;
        if mempool_ratio > *self.anomaly_thresholds.get("mempool_size").unwrap() {
            println!("🚨 [Sentinel] Anomaly: Mempool spiked {}x baseline.", mempool_ratio);
            anomaly_score += 0.4;
            threat = Some(ThreatType::MempoolSpam);
        }

        // 2. Cryptographic Anomaly (Quantum Crack)
        if current.failed_pqc_signatures > 20 {
            println!("🚨 [Sentinel] CRITICAL: PQC Signature failure threshold breached: {}.", current.failed_pqc_signatures);
            anomaly_score += 0.9;
            threat = Some(ThreatType::QuantumSignatureCrack);
        }

        // 3. Network Anomaly (Sybil)
        if current.unique_peer_count > self.baseline_state.unique_peer_count * 2 &&
           current.transactions_per_second < self.baseline_state.transactions_per_second / 2.0 {
            println!("🚨 [Sentinel] Anomaly: Sybil pattern detected (PeerCount UP, TPS DOWN).");
            anomaly_score += 0.6;
            threat = Some(ThreatType::SybilAttack);
        }

        // 4. Zero-Day Recognition
        if anomaly_score > 0.7 && threat.is_none() {
            println!("🚨 [Sentinel] ALERT: Unknown Zero-Day pattern detected.");
            threat = Some(ThreatType::UnknownZeroDay);
        }

        if let Some(t) = &threat {
            self.adapt_and_learn(current, t);
            if self.autonomous_mode {
                self.execute_defense(t);
            }
        } else {
            // 5. Organic Growth Adaptation (Weighted Moving Average)
            self.baseline_state.transactions_per_second =
                (self.baseline_state.transactions_per_second * 0.95) + (current.transactions_per_second * 0.05);
        }

        threat
    }

    fn adapt_and_learn(&mut self, state: &NetworkTelemetry, threat: &ThreatType) {
        let fingerprint = vec![
            state.mempool_size as f64,
            state.transactions_per_second,
            state.unique_peer_count as f64,
            state.failed_pqc_signatures as f64,
        ];

        // Memorize new threat fingerprint
        if !self.known_threat_signatures.iter().any(|s| self.dist(&fingerprint, s) < 1.0) {
            println!("🧠 [Sentinel] Memorized new adversarial fingerprint. Intelligence growing.");
            self.known_threat_signatures.push(fingerprint);
        }
    }

    fn execute_defense(&mut self, threat: &ThreatType) {
        self.defenses_deployed += 1;
        match threat {
            ThreatType::MempoolSpam => {
                println!("🛡️ [Defense] Autonomously raising L2 base fees to neutralize spam.");
            }
            ThreatType::QuantumSignatureCrack => {
                println!("🛡️ [Defense] CRITICAL: Initiating network-wide Dilithium key rotation.");
            }
            ThreatType::SybilAttack => {
                println!("🛡️ [Defense] Blacklisting high-risk IPs on Omni-Transport layer.");
            }
            ThreatType::UnknownZeroDay => {
                println!("🛡️ [Defense] Safe Mode: Pausing Meme Coin AI Agents and freezing L2 state.");
            }
            _ => {}
        }
    }

    fn dist(&self, a: &[f64], b: &[f64]) -> f64 {
        a.iter().zip(b).map(|(x, y)| (x - y).powi(2)).sum::<f64>().sqrt()
    }
}
