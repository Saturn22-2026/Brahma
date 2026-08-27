# Walkthrough: Phase 36 - BTQ Core SDK (Node.js)

I have successfully implemented the **BTQ Core SDK**, a high-level JavaScript/Node.js library that allows developers to integrate the **Bitcoin-Quantum** economic layer into any application with just a few lines of code.

## Changes Made

### 1. Unified Interface
- **[btq-sdk.js](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin-quantum/sdk/btq-sdk.js)**: Created a professional SDK class that wraps the complex Solidity interactions into clean asynchronous methods.
    - **`getPrice()` / `getFloat()`**: Provides instant access to the on-chain bonding curve state.
    - **`buyTokens(ethAmount)`**: Streamlines the purchase flow for applications.
    - **`sellTokens(sqtAmount, slippageTolerance)`**: Implements the **Informed Consent** protocol at the SDK level. It automatically calculates the `minEthOut` based on a user-defined slippage tolerance (default 1%), ensuring developers cannot accidentally subject their users to high taxes without protection.
    - **`getAirdropBudget()`**: Allows apps to dynamically display the available rewards for community engagement.

### 2. Ready-to-Use Examples
- **[example.js](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin-quantum/sdk/example.js)**: Developed a comprehensive boilerplate script showing how to initialize the SDK and interact with the local testnet.

### 3. ABI Maintenance
- **[abi.json](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin-quantum/sdk/abi.json)**: Extracted the essential ABI fragment from the core contract to ensure the SDK is lightweight and focused on economic operations.

## Security & Developer Experience Properties

> [!IMPORTANT]
> **Safety Inheritance**: By using the SDK, external applications automatically inherit the **Whale Protection** and **Informed Consent** safeguards. The SDK won't allow a trade to execute unless the blockchain can guarantee the payout defined by the slippage tolerance.

> [!TIP]
> **Rapid Integration**: Institutional partners can now integrate BTQ as a settlement layer in their backend systems using standard Node.js patterns without needing to write custom Web3 plumbing.

## Verification

### Local SDK Test
To verify the SDK on your machine:
1. Ensure a local Anvil node is running with the BTQ contract deployed.
2. Navigate to the SDK directory:
```bash
cd bitcoin-quantum/sdk
npm install ethers
node example.js
```
The script will output the current price, tradeable float, and your wallet's BTQ balance directly from the smart contract.
