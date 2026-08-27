# Task List: BTQ Ecosystem Growth

## Component 1: BTQ Native Android Wallet
- `[ ]` Initialize Android Wallet Project (`wallet-android`)
    - `[ ]` Setup Gradle, Jetpack Compose, and Material 3
    - `[ ]` Configure Rust JNI toolchain (Mozilla `rust-android-gradle` or similar)
- `[ ]` Integrate ML-DSA (Dilithium) Rust Bindings
    - `[ ]` Create JNI bridge for `mldsa-native`
    - `[ ]` Implement Key Generation and Signing in Kotlin/Rust
- `[ ]` Core Wallet Features
    - `[ ]` PQC Address Derivation
    - `[ ]` Balance Fetching (RPC)
    - `[ ]` Transaction Construction and Signing
- `[ ]` Wallet UI/UX
    - `[ ]` Dashboard (BTQ + 8 Meme Coins)
    - `[ ]` Whale Tax Estimator Screen
    - `[ ]` Receive (QR Code) and Send Flows

## Component 2: BTQ Block Explorer
- `[ ]` Initialize React Explorer Project
- `[ ]` Connect to Rust L1 Indexing Service
- `[ ]` Build Block and Transaction Visualizers

## Component 3: BTQ Cross-Chain Bridge
- `[ ]` Deploy Bridge Contracts (Sepolia/Testnet)
- `[ ]` Implement Bridge Relayer Service in Rust

## Component 4: BTQ DEX (AMM)
- `[ ]` Implement Whale-Resistant Swap Logic
- `[ ]` Deploy AMM on BTQ Testnet
