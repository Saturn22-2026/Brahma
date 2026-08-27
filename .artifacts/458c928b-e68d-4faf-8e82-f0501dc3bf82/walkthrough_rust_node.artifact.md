# Walkthrough: Phase 43.1 - Core L1 Node Logic (Rust)

I have successfully implemented the foundational **Layer 1 Node** logic for the `Bitcoin-Quantum` (BTQ) network using **Rust**. This establishes the high-performance execution layer required for a truly post-quantum decentralized economy.

## Changes Made

### 1. High-Performance Execution Layer
- **[main.rs](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/btq-node/src/main.rs)**: Implemented the core blockchain state machine.
    - **Post-Quantum Structures**: Defined `PQCAddress` and `Transaction` schemas designed for **ML-DSA (Dilithium)** signature integration.
    - **Cryptographic Hashing**: Integrated **SHA-256** via the `sha2` crate for secure, deterministic block identifiers.
    - **Proof-of-Work (PoW) Engine**: Implemented a native mining loop with configurable difficulty.

### 2. Native Monetary Policy
- **Asymptotic Emission Decay**: Hard-coded the continuous decay formula ($R = R_0 \cdot e^{-\lambda t}$) directly into the node's block reward calculation.
    - This ensures that the monetary policy is enforced by the very math of the execution layer, eliminating the need for abrupt halvings.
    - **Tail Emission**: Enforced a hard floor of **0.01 BTQ** per block to ensure permanent network security.

### 3. Serialization & State Management
- **Serde Integration**: Utilized `serde` and `serde_json` for efficient, reliable data serialization during hashing and network propagation.

## Technical & Economic Properties

> [!IMPORTANT]
> **Performance Reliability**: By choosing Rust for the L1 node, we ensure that the computationally intensive lattice-based cryptography and high-frequency mining loops operate with maximum hardware efficiency and memory safety.

> [!TIP]
> **Smooth Scarcity**: The asymptotic decay prevents the "miner shocks" associated with traditional Bitcoin halvings, creating a more stable and professional economic environment for institutional participants.

## Verification

### Local Blockchain Simulation
To run the BTQ node and simulate the mining of the first 5 blocks:
```powershell
cd btq-node
# Ensure Rust and Cargo are in your PATH (from Phase 43)
cargo run
```

Observe the output to verify:
- **Block Linking**: Each block correctly references the hash of the previous block.
- **Reward Decay**: The reward slightly decreases with every block (e.g., Block 1: 49.9950 BTQ, Block 5: 49.9750 BTQ).
- **PoW Validation**: Each block's hash meets the difficulty target (ending in '0000').
