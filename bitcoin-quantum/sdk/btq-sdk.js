const { ethers } = require('ethers');
const BTQ_ABI = require('./abi.json');

class BTQ_SDK {
    /**
     * @param {string} rpcUrl - RPC URL for the blockchain node.
     * @param {string} privateKey - Private key for the hot wallet.
     * @param {string} contractAddress - Address of the deployed BitcoinQuantum contract.
     */
    constructor(rpcUrl, privateKey, contractAddress) {
        this.provider = new ethers.JsonRpcProvider(rpcUrl);
        this.wallet = new ethers.Wallet(privateKey, this.provider);
        this.contract = new ethers.Contract(contractAddress, BTQ_ABI, this.wallet);
    }

    // 1. Get current bonding curve price
    async getPrice() {
        const price = await this.contract.currentPrice();
        return ethers.formatEther(price);
    }

    // 2. Get current tradeable float
    async getFloat() {
        const float = await this.contract.tradeableFloat();
        return ethers.formatEther(float);
    }

    // 3. Get available airdrop budget
    async getAirdropBudget() {
        const budget = await this.contract.getCurrentAirdropBudget();
        return ethers.formatEther(budget);
    }

    // 4. Buy BTQ with native currency (e.g. ETH)
    async buyTokens(ethAmount) {
        console.log(`[SDK] Purchasing BTQ with ${ethAmount} ETH...`);
        const tx = await this.contract.buyTokens({ value: ethers.parseEther(ethAmount) });
        const receipt = await tx.wait();
        return receipt;
    }

    /**
     * 5. Sell BTQ with Informed Consent (minEthOut)
     * @param {string} sqtAmount - Amount of BTQ to sell.
     * @param {number} slippageTolerance - Max allowed slippage (e.g. 0.01 for 1%).
     */
    async sellTokens(sqtAmount, slippageTolerance = 0.01) {
        const currentPrice = await this.getPrice();
        const tokensToSell = ethers.parseEther(sqtAmount);

        // Simplified estimate (in real app, use a callStatic or a specialized view function to get exact tax)
        // For this SDK, we calculate the floor based on the current price and slippage.
        const expectedEth = parseFloat(sqtAmount) * parseFloat(currentPrice);
        const minEthOut = ethers.parseUnits((expectedEth * (1 - slippageTolerance)).toFixed(18), 18);

        console.log(`[SDK] Selling ${sqtAmount} BTQ. Expecting >= ${ethers.formatEther(minEthOut)} ETH...`);

        const tx = await this.contract.sellTokens(tokensToSell, minEthOut);
        const receipt = await tx.wait();
        return receipt;
    }

    // 6. Standard BTQ transfer
    async transferTokens(toAddress, amount) {
        console.log(`[SDK] Transferring ${amount} BTQ to ${toAddress}...`);
        const tx = await this.contract.transfer(toAddress, ethers.parseEther(amount));
        const receipt = await tx.wait();
        return receipt;
    }

    // 7. Get user balance
    async getBalance(address) {
        const balance = await this.contract.balanceOf(address || this.wallet.address);
        return ethers.formatEther(balance);
    }
}

module.exports = BTQ_SDK;
