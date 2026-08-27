# Walkthrough: Phase 43.5 & 43.6 - PQ-Security & JSON-RPC API

I have successfully upgraded the **BTQ Mainnet Node** with full **Post-Quantum (ML-DSA)** signature validation and a professional **JSON-RPC 2.0 Interface**. This moves the node from a standalone experimental project to a production-grade infrastructure component capable of supporting external wallets.

## Changes Made

### 1. Post-Quantum Security Layer
- **[crypto.rs](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/btq-node/src/crypto.rs)**: Implemented the native **ML-DSA (Dilithium2)** cryptographic module.
    - **Native Keypair Generation**: Support for generating 1312-byte public and 2528-byte secret keys.
    - **Signature Verification**: Native wrapper for `pqc_dilithium` to verify 2420-byte signatures against transaction hashes.
    - **Address Derivation**: Implemented `derive_address()` which creates a human-readable `0x` address from the SHA-256 hash of the Dilithium public key.
- **On-Chain Validation**: Updated **[main.rs](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/btq-node/src/main.rs)** to strictly enforce signature checks. The `BlockchainCore` now rejects any block containing a transaction with an invalid ML-DSA signature.

### 2. professional JSON-RPC 2.0 API
- **[rpc.rs](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/btq-node/src/rpc.rs)**: Developed a high-performance API server using `jsonrpsee`.
    - **Standardized Endpoints**:
        - `btq_getNetworkStats`: Returns real-time chain height, total mined tokens, and network difficulty.
        - `btq_getLatestBlock`: Returns the full JSON-serialized data of the most recent block.
- **Asynchronous Orchestration**: The RPC server runs in a dedicated background task, ensuring that API queries do not block the P2P networking or mining loops.

### 3. State-Aware Synchronization
- Implemented thread-safe access to the `BlockchainCore` using `Arc<RwLock>`, allowing the RPC server and P2P handlers to share the same persistent state safely across multiple threads.

## Security & Connectivity Properties

> [!IMPORTANT]
> **Quantum-Immune Identity**: By integrating ML-DSA directly into the transaction validation hook, every wallet on the BTQ network is natively protected against attacks from future quantum computers.

> [!TIP]
> **Web3 Tooling Compatibility**: The JSON-RPC 2.0 interface on port `8545` allows standard Web3 libraries (like `ethers.js` or `web3.py`) to connect to your custom Rust node with minimal modification.

## Verification

### 1. Cryptographic Forgery Protection
The node now rejects any transaction where the data has been modified after signing. If you change even 1 bit of the `amount`, the `Transaction::verify()` call will fail, and the block will be dropped.

### 2. Querying the Node via RPC
You can now query your running node using standard tools like `curl`:
```bash
curl -X POST -H "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"btq_getNetworkStats","params":[],"id":1}' http://localhost:8545
```
**Expected Response**:
```json
{"jsonrpc":"2.0","result":{"chain_height":5,"total_mined":249.95,"difficulty":4,"p2p_status":"CONNECTED"},"id":1}
```
