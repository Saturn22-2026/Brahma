# Implementation Plan - Emergency Distress Protocol (EDP)

This plan implements a "Panic Sweep" feature that allows a user under duress to quickly transfer all assets (BTQ and L2 tokens) to a pre-configured successor address.

## User Review Required

> [!IMPORTANT]
> The "Distress Trigger" is disguised as a long-press on the "Version" text in the Settings screen to avoid detection by an attacker.
> This feature will immediately attempt to broadcast transactions for ALL assets.

## Proposed Changes

### Core Logic & Storage

#### [MODIFY] [WalletViewModel.kt](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/app/src/main/java/com/btq/wallet/viewmodel/WalletViewModel.kt)
- Add `successorAddress` state (StateFlow).
- Implement `setSuccessorAddress(address: String)` to persist the destination.
- Implement `executeEmergencyDistressSweep()`:
    - Sets status to an innocuous message (e.g., "Network Optimization...").
    - Loops through primary balance and all L2 tokens.
    - Signs and broadcasts "Send All" transactions to the successor address.
    - Wipes local state (clears `address` and `mnemonic`) upon completion to prevent further tampering.

### User Interface

#### [MODIFY] [MainActivity.kt](file:///C:/GitHub/Bitcoin-Quantum/wallet-android/app/src/main/java/com/btq/wallet/MainActivity.kt)
- **Settings Screen:**
    - Add a "Successor Configuration" section where the user can input a trusted address.
    - Add a version info text at the bottom.
    - Add a `Modifier.pointerInput` or `Modifier.combinedClickable` to the version text to trigger the sweep on a 5-second long-press.
- **Home Screen:**
    - Ensure the "Distress Status" is innocuous if triggered.

## Verification Plan

### Automated Tests
- Unit test for `executeEmergencyDistressSweep` to verify it generates transactions for all non-zero balances.

### Manual Verification
1. Open Settings.
2. Enter a valid BTQ address as the "Successor".
3. Navigate to the bottom and long-press the version string for 5 seconds.
4. Verify that the UI clears the wallet and the "Successor" receives the (mocked) transaction broadcast.
