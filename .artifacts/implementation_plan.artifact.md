# Bitcoin-Quantum (BTQ) Growth Strategy & Implementation Plan

This plan outlines the execution of the "Immediate Must-Build Checklist" to transition BTQ from a codebase to a globally adopted network. The strategy focuses on four pillars: Meme Coins (Retail), Post-Quantum Security (Institutional), Omni-Transport (Apocalypse Network), and AI Agents (Autonomous Economy).

## User Review Required

> [!IMPORTANT]
> **Post-Quantum Mobile Security**: We are implementing ML-DSA (Dilithium) signatures directly in the mobile wallet. This requires native Rust bindings for Android to ensure performance and security.
> **Whale Extinguisher Integration**: The mobile wallet will need to display real-time tax estimates for large swaps to inform users of the Whale Extinguisher impact.

## Open Questions
1. **RPC Gateway**: For the mobile wallet, should we use a public RPC endpoint or implement a lightweight SPV-style client?
2. **Bridge Mechanism**: Do we want a centralized custodian-based bridge for V1, or a decentralized multi-sig bridge using ML-DSA?

## Proposed Changes

### [Component 1] BTQ Native Android Wallet
We will build a high-performance, quantum-secure Android wallet.

#### [NEW] [BTQWalletApp](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/wallet-android/)
- **Tech Stack**: Kotlin, Jetpack Compose, Rust (via JNI for ML-DSA).
- **Key Features**:
    - ML-DSA (Dilithium) Key Management.
    - Support for $BTQ and the 8 Meme Coins ($HOMIE, $POOKIE, etc.).
    - Transaction signing and RPC interaction.
    - Whale Extinguisher tax calculator.

### [Component 2] BTQ Block Explorer
A web interface to visualize the chain's health.

#### [NEW] [BTQExplorer](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/portal/explorer/)
- **Tech Stack**: React, TypeScript, Tailwind CSS.
- **Features**:
    - Real-time block and transaction feed.
    - Address lookup and balance tracking.
    - AI Agent activity logs (e.g., $HOMIE treasury management).

### [Component 3] BTQ Cross-Chain Bridge
A bridge between Ethereum and BTQ.

#### [NEW] [BTQBridge](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/contracts/Bridge.sol)
- **Solidity Contract**: Handles locking ETH/USDC on Ethereum.
#### [NEW] [BridgeRelayer](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/scripts/bridge_relayer.rs)
- **Rust Service**: Monitors Ethereum events and mints wrapped-BTQ on the L1.

### [Component 4] BTQ DEX (AMM)
A native Automated Market Maker.

#### [NEW] [BTQDex](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/contracts/BTQDex.sol)
- **Whale-Resistant AMM**: Integrates the `SovereignEconomy` logic into a Uniswap-style pool.

## Verification Plan

### Automated Tests
- **PQC Verification**: Unit tests for ML-DSA signature generation and verification on Android.
- **RPC Integration**: Integration tests between the Wallet and the `btq-node`.
- **Economic Stress Tests**: Simulations of the Whale Extinguisher tax under high volatility.

### Manual Verification
- **End-to-End Swap**: Bridge ETH from Sepolia to BTQ Testnet, swap for $POOKIE, and verify the transaction in the Explorer.
- **Node Sync**: Verify that the Android Wallet correctly reads balances from the `btq-node`.
