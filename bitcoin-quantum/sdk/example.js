const BTQ_SDK = require('./btq-sdk');

async function runExample() {
    // Configuration for local Anvil node
    const RPC_URL = "http://127.0.0.1:8545";
    const PRIVATE_KEY = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80";
    const CONTRACT_ADDR = "0x5FbDB2315678afecb367f032d93F642f64180aa3";

    const sdk = new BTQ_SDK(RPC_URL, PRIVATE_KEY, CONTRACT_ADDR);

    console.log("=== Bitcoin-Quantum SDK Example ===");

    try {
        // 1. Fetch Stats
        const price = await sdk.getPrice();
        const float = await sdk.getFloat();
        const budget = await sdk.getAirdropBudget();
        console.log(`- Current Price: ${price} ETH`);
        console.log(`- Tradeable Float: ${float} BTQ`);
        console.log(`- Airdrop Budget: ${budget} BTQ`);

        // 2. Buy BTQ
        // await sdk.buyTokens("1.0");
        // console.log("✅ Purchase successful.");

        // 3. Check Balance
        const balance = await sdk.getBalance();
        console.log(`- Your Balance: ${balance} BTQ`);

        // 4. Protected Sell
        // This will automatically calculate minEthOut to protect against high slippage/tax
        // await sdk.sellTokens("100", 0.01); // 1% tolerance
        // console.log("✅ Protected sell successful.");

    } catch (error) {
        console.error("❌ SDK Error:", error.message);
    }
}

runExample();
