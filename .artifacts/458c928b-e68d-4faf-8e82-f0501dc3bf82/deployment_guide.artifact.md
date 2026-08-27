# Deployment & Verification Guide: Sovereign Smart Contract

This guide provides the necessary steps to deploy the **SovereignEconomy** smart contract to a local testnet and verify its core economic features.

## Prerequisites
- Node.js installed.
- Hardhat or Foundry installed.
- OpenZeppelin Contracts (optional, the provided contract is self-contained for the core logic).

## 1. Local Deployment (Hardhat)

Save the following as `scripts/deploy.js`:

```javascript
async function main() {
  const [deployer, wealth, empower, reserve] = await ethers.getSigners();

  console.log("Deploying SovereignEconomy with account:", deployer.address);

  const SovereignEconomy = await ethers.getContractFactory("SovereignEconomy");
  const initialSupply = 1000000; // 1M tokens

  const contract = await SovereignEconomy.deploy(
    wealth.address,
    empower.address,
    reserve.address,
    initialSupply
  );

  await contract.deployed();

  console.log("Contract deployed to:", contract.address);
  console.log("Wealth Wallet:", wealth.address);
  console.log("Empowerment Wallet:", empower.address);
  console.log("Reserve Wallet:", reserve.address);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
```

## 2. Economic Verification Script

Save this as `scripts/verify_economy.js` to test the Whale Tax and Bonding Curve:

```javascript
async function main() {
  const [deployer, wealth, empower, reserve, buyer] = await ethers.getSigners();
  const contractAddress = "YOUR_DEPLOYED_CONTRACT_ADDRESS";
  const contract = await ethers.getContractAt("SovereignEconomy", contractAddress);

  // 1. Check Initial Price
  let price = await contract.currentPrice();
  console.log("Initial Price:", ethers.utils.formatEther(price), "ETH");

  // 2. Execute a Buy (Price should increase)
  await contract.connect(buyer).buyTokens({ value: ethers.utils.parseEther("1.0") });
  price = await contract.currentPrice();
  console.log("Price after 1 ETH buy:", ethers.utils.formatEther(price), "ETH");

  // 3. Execute a Whale Sell (Price should decrease + trigger Tax)
  const balance = await contract.balanceOf(buyer.address);
  // Sell a huge chunk to trigger the 2.5% dump ratio
  const whaleAmount = balance.div(2); // Example large amount

  const tx = await contract.connect(buyer).sellTokens(whaleAmount);
  const receipt = await tx.wait();

  const taxEvent = receipt.events.find(e => e.event === 'WhaleTaxApplied');
  if (taxEvent) {
    console.log("Whale Tax Detected! Taxed Amount:", ethers.utils.formatUnits(taxEvent.args.taxedAmount, 18), "SQT");
  }

  const newPrice = await contract.currentPrice();
  console.log("Final Price:", ethers.utils.formatEther(newPrice), "ETH");
}
```

## 3. Running the Verification
```bash
npx hardhat node
npx hardhat run scripts/deploy.js --network localhost
# Update the address in verify_economy.js
npx hardhat run scripts/verify_economy.js --network localhost
```
