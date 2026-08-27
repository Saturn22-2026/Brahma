const hre = require("hardhat");

async function main() {
  console.log("=========================================");
  console.log("BTQ QUICK LAUNCH: PHASE 1 (L2 LAYER)");
  console.log("=========================================\n");

  const [deployer] = await hre.ethers.getSigners();
  console.log("Deploying with account:", deployer.address);

  // 1. Deploy the LaunchFactory
  const LaunchFactory = await hre.ethers.getContractFactory("LaunchFactory");
  const factory = await LaunchFactory.deploy();
  await factory.waitForDeployment();
  console.log("LaunchFactory deployed to:", await factory.getAddress());

  // 2. Deploy BTQToken via Factory
  console.log("\n[1/2] Deploying Sovereign BTQ Token...");
  const tx1 = await factory.deploySovereignToken();
  await tx1.wait();
  const tokenAddr = await factory.tokenAddress();
  console.log("BTQToken deployed to:", tokenAddr);

  // 3. Deploy L2 Factory via Factory
  console.log("\n[2/2] Configuring L2 Factory...");
  const tx2 = await factory.deployAndConfigureFactory();
  await tx2.wait();
  const l2FactoryAddr = await factory.factoryAddress();
  console.log("L2 Factory deployed to:", l2FactoryAddr);

  console.log("\n✅ PHASE 1 COMPLETE.");
  console.log("Update your btq-node and ai_agent configs with these addresses.");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
