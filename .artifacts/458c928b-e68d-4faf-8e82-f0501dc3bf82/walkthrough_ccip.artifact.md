# Walkthrough: Phase 34 - Cross-Chain Interoperability (Chainlink CCIP)

I have successfully implemented the **Cross-Chain Bridge** infrastructure for the `Bitcoin-Quantum` network. This enables the movement of BTQ tokens between the Sovereign L1 and any EVM-compatible chain (like Ethereum, Base, or Arbitrum) using **Chainlink CCIP**.

## Changes Made

### 1. Token Bridge Readiness
- **[BitcoinQuantum.sol](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin-quantum/src/BitcoinQuantum.sol)**:
    - Added the `bridgeTransfer` function and `authorizedBridges` mapping.
    - This allows authorized bridge contracts to release tokens from the contract's float to users, supporting a **Lock & Mint** (or burn/mint) bridging model.

### 2. CCIP Sender & Receiver
- **[CCIPBridgeSender.sol](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin-quantum/src/CCIPBridgeSender.sol)**:
    - Implemented the source-chain logic for locking BTQ and initiating a CCIP cross-chain message.
    - Uses the `IRouterClient` to calculate fees (payable in LINK) and securely broadcast the message.
- **[CCIPBridgeReceiver.sol](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin-quantum/src/CCIPBridgeReceiver.sol)**:
    - Implemented the destination-chain logic to handle incoming CCIP messages.
    - Automatically decodes the message and calls `bridgeTransfer` to release the equivalent tokens on the new chain.

### 3. Integrated Tooling & UI
- **[remappings.txt](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin-quantum/remappings.txt)**: Configured the project to resolve Chainlink CCIP contract imports.
- **[useSovereignToken.ts](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/portal/src/hooks/useSovereignToken.ts)**: Added the `bridge()` function to the portal's custom hook, allowing users to initiate cross-chain transfers directly from the web dashboard.
- **[node.go](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/sequencer/node.go)**: Integrated a mock **Cross-Chain Monitor** to ensure the L2 sequencer can stay synchronized with incoming bridge events on L1.

## Security & Interoperability Properties

> [!IMPORTANT]
> **Universal Liquidity**: BTQ is no longer confined to its native chain. It can now act as a globally liquid asset while retaining its "Sovereign" economic properties (Whale Tax, bonding curve) on the primary network.

> [!TIP]
> **Don-Validated Security**: By utilizing Chainlink CCIP, the bridge security is backed by a Decentralized Oracle Network, protecting users from the central-point-of-failure risks associated with traditional multi-sig bridges.

## Verification

### Local Simulation
1.  Navigate to `bitcoin-quantum/` and ensure the CCIP libraries are cloned in `lib/`.
2.  Deploy `CCIPBridgeSender` and `CCIPBridgeReceiver` to local test chains.
3.  Execute a `bridgeBTQ` call and verify that the `bridgeTransfer` event is emitted on the destination contract.

### Dashboard Integration
Check the **User Portal** code to see the new `bridge` method available for integration into a cross-chain transfer UI.
