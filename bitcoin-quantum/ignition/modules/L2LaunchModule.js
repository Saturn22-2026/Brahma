const { buildModule } = require("@nomicfoundation/hardhat-ignition/modules");
const BTQModule = require("./BTQModule");

/**
 * @title L2 Launch Module
 * @dev Orchestrates the deployment of the L2 Factory and bootstrapping of the 8 Sovereign Memecoins.
 */
module.exports = buildModule("L2LaunchModule", (m) => {
  const { btqToken } = m.useModule(BTQModule);
  const deployer = m.getAccount(0);

  // 1. Deploy BTQL2Factory
  const l2Factory = m.contract("BTQL2Factory", [btqToken, deployer]);

  // 2. Bootstrap the 8 Sovereign Memecoins
  m.call(l2Factory, "bootstrapInitial8", []);

  return { l2Factory };
});
