const { buildModule } = require("@nomicfoundation/hardhat-ignition/modules");

/**
 * @title BTQMasterModule
 * @dev The official Genesis deployment module for Bitcoin-Quantum (BTQ).
 * Automates the deployment, linking, and initial funding of the modular ecosystem.
 */
module.exports = buildModule("BTQMasterModule", (m) => {
  const deployer = m.getAccount(0);

  // 1. Deploy Token with all 6 wallet splits (using deployer for testnet)
  // Distribution: 10M/10M/5M/10M/10M/40M + 15M Mining
  const btqToken = m.contract("BTQToken", [
    deployer, // Wealth Wallet
    deployer, // Empowerment Wallet
    deployer, // Stability Wallet (becomes the reserveWallet)
    deployer, // AI Donation Wallet (Locked for 730 days)
    deployer, // Airdrop Pool Source
    deployer  // Initial AMM Float Source
  ]);

  // 2. Deploy Airdrop Contract (10-Year Linear Drip Engine)
  const btqAirdrop = m.contract("BTQAirdrop", [btqToken, deployer]);

  // 3. Deploy Mining Contract (Continuous Asymptotic Decay)
  const btqMining = m.contract("BTQMining", [btqToken]);

  // 4. Deploy Faucet (Community Recurring Bootstrap)
  const btqFaucet = m.contract("BTQFaucet", [btqToken]);

  // --- POST-DEPLOYMENT SETUP ---

  // 5. Link Mining Contract to Token
  // This allows the mining contract to trigger 'mintMiningReward' on the token.
  m.call(btqToken, "setMiningContract", [btqMining]);

  // 6. Transfer 10M BTQ to Airdrop Contract
  // The 'airdropWallet' (deployer) seeds the specialized drip engine.
  const airdropAmount = 10000000000000000000000000n; // 10M BTQ (18 decimals)
  m.call(btqToken, "transfer", [btqAirdrop, airdropAmount]);

  // 7. Transfer 1M BTQ to Faucet for testnet users
  // Seed the recurring community rewards pool.
  const faucetAmount = 1000000000000000000000000n; // 1M BTQ (18 decimals)
  m.call(btqToken, "transfer", [btqFaucet, faucetAmount]);

  return { btqToken, btqAirdrop, btqMining, btqFaucet };
});
