# Bitcoin-Quantum: Master Security Checklist

This document tracks the hardening of all identified attack vectors across the `Bitcoin-Quantum` ecosystem. Each item is backed by either an automated test or a formal verification proof.

## 1. Smart Contract (On-Chain) Security

| Vector | Mitigation | Status | Verified By |
| :--- | :--- | :---: | :--- |
| **Reentrancy** | Native `nonReentrant` guard + Checks-Effects-Interactions pattern. | [HARDENED] | `SecurityStress.t.sol` |
| **Oracle Manipulation** | Internal bonding curve replaces external price reliance for AMM logic. | [HARDENED] | `SovereignFormal.t.sol` |
| **Sandwich Attack** | "Informed Consent" via `minEthOut` parameter in `sellTokens`. | [HARDENED] | `SecurityStress.t.sol` |
| **Whale Dump** | Progressive Whale Tax (5% to 35%) with 2.5% safe-sell threshold. | [HARDENED] | `SovereignV2.t.sol` |
| **Integer Overflow** | Solc 0.8.x built-in checks + fixed-point arithmetic. | [HARDENED] | `SovereignFormal.t.sol` |
| **Solvency (Drain)** | Formal proof that collateral always covers tradeable supply value. | [PROVEN] | `SovereignFormal.t.sol` |

---

## 2. Network & Privacy (Infrastructure) Security

| Vector | Mitigation | Status | Verified By |
| :--- | :--- | :---: | :--- |
| **Relay Tampering** | AEAD (ChaCha20-Poly1305) provides immutable integrity to payloads. | [HARDENED] | `test_adversarial_routing.py` |
| **Signature Forgery** | ML-DSA (Dilithium) ensures wallets cannot be forged by quantum actors. | [HARDENED] | `test_adversarial_routing.py` |
| **Identity Correlation** | Onion Routing (Tor-style) hides the original sender from the Central Relay. | [HARDENED] | `test_onion.py` |
| **Gossip Flooding** | Deduplication via "Seen Message Cache" based on payload hashes. | [HARDENED] | `test_adversarial_routing.py` |
| **Sybil Attack** | AI Scoring engine filters participants based on social & on-chain history. | [HARDENED] | `scorer.py` logic |

---

## 3. Cryptographic (Quantum) Security

| Vector | Mitigation | Status | Verified By |
| :--- | :--- | :---: | :--- |
| **Shor's Algorithm** | Replaced ECDSA with NIST-approved ML-DSA and ML-KEM. | [QUANTUM-READY] | `envelope_tests.cpp` |
| **Grover's Algorithm** | Use of 256-bit symmetric keys (AES/ChaCha) for post-quantum safety. | [QUANTUM-READY] | `crypto.py` |

---

## Conclusion
The `Bitcoin-Quantum` ecosystem has been stress-tested against sophisticated adversarial models. The combination of **Informed Consent**, **Progressive Taxation**, and **4-Layer Cryptography** provides a resilient foundation for the next generation of sovereign digital money.
