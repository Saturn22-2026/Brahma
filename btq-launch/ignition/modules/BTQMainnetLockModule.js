const { buildModule } = require("@nomicfoundation/hardhat-ignition/modules");

module.exports = buildModule("BTQMainnetLockModule", (m) => {
  const deployer = m.getAccount(0);

  // 1. Deploy all contracts
  const btqToken = m.contract("BTQToken", []);

  const btqAirdrop = m.contract("BTQAirdrop", [btqToken, deployer]);
  const btqMining = m.contract("BTQMining", [btqToken]);
  const btqFaucet = m.contract("BTQFaucet", [btqToken]);

  // 2. Activate Systems (Fund the contracts)
  m.call(btqToken, "setMiningContract", [btqMining]);
  m.call(btqToken, "setAirdropContract", [btqAirdrop]);

  // Note: The contracts use SovereignConstants for their funding,
  // but we ensure the links are established.

  // 3. THE IRREVOCABLE LOCK
  // Renounce ownership on all Ownable contracts.
  // Once these execute, the deployer cannot change any settings, mint more tokens, or upgrade.
  m.call(btqAirdrop, "renounceOwnership", []);
  m.call(btqMining, "renounceOwnership", []);
  m.call(btqFaucet, "renounceOwnership", []);

  return { btqToken, btqAirdrop, btqMining, btqFaucet };
});
