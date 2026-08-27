# Bitcoin-Quantum: Multi-Layer Security Dissection

This document provides a technical breakdown of the defensive layers protecting the **Bitcoin-Quantum (BTQ)** ecosystem from modern and future threats, including Quantum Computing and Advanced Synthetic AI.

## Layer 1: Cryptographic Sovereignty (ML-DSA)
- **The Threat**: Shor's Algorithm (Quantum factorization of ECDSA keys).
- **The Defense**: Native implementation of **ML-DSA (Dilithium2)**.
    - Unlike ECC, Dilithium relies on the hardness of **Learning With Errors (LWE)** over modules.
    - **Entropy Injection**: Every signature includes a unique polynomial shift, preventing "deterministic leakage" even if the same transaction hash is signed twice.
- **Verification**: Enforced by the Rust execution client in `crypto.rs` and the Solidity bridge.

## Layer 2: Network Resilience (libp2p & Resource Management)
- **The Threat**: Peer-to-Peer DDoS and CPU exhaustion.
- **The Defense**:
    - **Resource Manager**: Sets hard limits on open streams and memory usage per peer.
    - **Gossipsub v1.1 Peer Scoring**: A reputation-based filter. Peers that send invalid blocks or excessive junk are penalized.
    - **Async Validation**: Computational validation (signatures) is offloaded from the main event loop, preventing "freezing" attacks.
- **Verification**: Stress-tested via the `attacker.rs` chaos binary.

## Layer 3: Economic Armor (Whale Extinguisher v4)
- **The Threat**: VC/Institutional "Exit Dumps" and price manipulation.
- **The Defense**:
    - **DEX-Hooked Taxation**: Applied at the base token level for any address marked as an AMM pair.
    - **Progressive Friction**: 5% to 35% tax based on the ratio of the sell to the *currently released* tradeable float.
    - **Stability Rerouting**: Taxed tokens are not burned; they fund the **Sovereign Stability Wallet** to create a permanent price support floor.
- **Verification**: Mathematically proven in `WhaleEconomicStress.t.sol`.

## Layer 4: Infrastructure Autonomy (Omni Transport)
- **The Threat**: Global internet censorship, DNS hijacking, or grid outages.
- **The Defense**: **Omni-Channel Physical Routing**.
    - The node can gossip blocks via **HF/VHF Radio Waves** and **LEO Satellites**.
    - This creates a "Shadow Network" that operates independently of the public TCP/IP stack.
- **Verification**: Implemented in the `btq-omni-transport` Rust crate.

## Layer 5: Adversarial AI Resilience (Proof-of-Engagement)
- **The Threat**: Advanced synthetic agents (LLMs) used to farm community funds via "fake" technical analysis.
- **The Defense**:
    - **Heuristic Depth Check**: The scorer doesn't just look for keywords; it uses **NLP logical-linking** to reward deep structural analysis while penalizing generic AI-generated hype.
    - **On-Chain Identity Proof**: Airdrop eligibility is weighted by **On-chain Loyalty Days** and **Discord Interaction Density**. An AI-generated account with zero "Behavioral History" is automatically filtered out.
- **Verification**: Tested against the "LLM-Mimicry" dataset in `test_ai_intelligence.py`.

---

### Conclusion
The BTQ security model is not a single wall, but a series of interlocking biological and mathematical systems. An attacker must simultaneously break **Lattice Cryptography**, bypass **Reputation Filters**, absorb a **35% Economic Loss**, and operate across **Physical Omni-Channels**.
