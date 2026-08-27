const { buildModule } = require("@nomicfoundation/hardhat-ignition/modules");

module.exports = buildModule("BTQModule", (m) => {
  const deployer = m.getAccount(0);

  // 1. Deploy Token with updated 6-wallet split
  const btqToken = m.contract("BTQToken", [
    deployer, // Wealth Wallet (10M)
    deployer, // Empowerment Wallet (10M)
    deployer, // Stability Wallet (5M)
    deployer, // AI Donation Wallet (10M - Locked 2 yrs)
    deployer, // Airdrop Wallet (10M - Will be transferred to Airdrop Contract)
    deployer  // Float Wallet (40M - AMM)
  ]);

  // 2. Deploy Airdrop Contract
  const btqAirdrop = m.contract("BTQAirdrop", [
    btqToken,
    deployer // AI Agent Wallet
  ]);

  // 3. Deploy Mining Contract
  const btqMining = m.contract("BTQMining", [
    btqToken
  ]);

  // 4. Link Mining Contract to Token
  m.call(btqToken, "setMiningContract", [btqMining]);

  // 5. Transfer 10M BTQ from deployer (who received Airdrop Wallet share) to the Airdrop Contract
  const airdropAmount = 10000000000000000000000000n; // 10M BTQ with 18 decimals
  m.call(btqToken, "transfer", [btqAirdrop, airdropAmount], { from: deployer });

  return { btqToken, btqAirdrop, btqMining };
});
