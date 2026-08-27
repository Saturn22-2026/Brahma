# Wallet Deployment and AI Release Plan

This plan outlines the steps to resolve synchronization issues, deploy the Bitcoin-Quantum Android wallet, and activate the autonomous AI Council for airdrop and faucet releases.

## User Review Required

> [!IMPORTANT]
> **Environment Variables**: The AI Council requires `AI_AGENT_PRIVATE_KEY` and `X_API_KEY` to be set. I will attempt to run a dry-run first. Please ensure these are configured in your environment if you want live on-chain execution.
> **Target Device**: Deployment requires an Android device or emulator to be connected via ADB.

## Proposed Changes

### Build System & Sync Fixes

I will consolidate the nested Gradle structure to ensure all modules are visible to the root project.

#### [MODIFY] [settings.gradle.kts](file:///C:/GitHub/Bitcoin-Quantum/settings.gradle.kts)
- Explicitly include `:wallet-android:app`, `:wallet-android:shared`, and `:wallet-android:composeApp`.
- This fixes the "project not found" errors during build.

---

### Android Wallet Deployment

I will verify the build and deploy the wallet to the connected device.

#### [TASK] Build Application
- Run `./gradlew :wallet-android:app:assembleDebug` to verify the native Android app.
- Run `./gradlew :wallet-android:composeApp:assembleDebug` to verify the KMP Compose app.

#### [TASK] Deploy to Device
- Use `deploy` tool to push the chosen wallet (`com.btq.wallet`) to the device.

---

### AI Council & Release Activation

I will trigger the autonomous agents to release airdrops and expand the faucet.

#### [MODIFY] [main.py](file:///C:/GitHub/Bitcoin-Quantum/ai_agent/main.py) (Optional)
- Add a `--dry-run` flag or simulation mode if not already present to verify quorum before live execution.

#### [TASK] Execute Council Cycle
- Run `python -m ai_agent.main` to convening the 8 specialized agents.
- This will:
    1. Verify technical content vs hype via the `MarketingAIScorer`.
    2. Collect 5/8 signatures for the `OnChainAirdropAgent`.
    3. Trigger the `TreasuryAgent` to evaluate faucet expansion tiers.

## Verification Plan

### Automated Verification
- Run `python ai_agent/tests/test_ai_intelligence.py` to ensure heuristics are still passing.
- Run `gradle_sync` to verify the new project structure.

### Manual Verification
- Check logcat for `[Quorum] ✅ 5/8 Agents Synchronized` and `[Agent] Triggering daily drip`.
- Verify the app UI loads on the device.
