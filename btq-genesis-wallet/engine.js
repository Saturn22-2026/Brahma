const readline = require('readline');
const crypto = require('crypto');

// --- Simulated On-Chain Database (State) ---
const BlockchainState = {
    tokenName: "Mainnet Project Token",
    symbol: "MPT",
    totalSupply: 1000000,
    balances: {
        '0xOwnerAddress': 1000000
    },
    userNonces: {},
    referrers: {},
    tierPercentages: [0.10, 0.05, 0.02], // 10%, 5%, 2%
    baseMiningReward: 50,
    difficultyZeros: 3, // Simulating mining difficulty (number of leading hex zeros)

    getNonce(address) {
        return this.userNonces[address] || 0;
    },

    getBalance(address) {
        return this.balances[address] || 0;
    }
};

// --- Core Logic Engines ---
function localMineFaucet(minerAddress, nonce, referrerAddress) {
    const currentNonce = BlockchainState.getNonce(minerAddress);

    // Cryptographic validation mirroring the Solidity Keccak256 proof-of-work
    const hash = crypto.createHash('sha256')
        .update(`${minerAddress}${currentNonce}${nonce}`)
        .digest('hex');

    const targetPrefix = '0'.repeat(BlockchainState.difficultyZeros);
    if (!hash.startsWith(targetPrefix)) {
        return { success: false, reason: `Invalid Proof. Hash (${hash.substring(0, 8)}...) did not meet difficulty target.` };
    }

    // Set up local referral mapping if valid
    if (referrerAddress && !BlockchainState.referrers[minerAddress] && referrerAddress !== minerAddress) {
        BlockchainState.referrers[minerAddress] = referrerAddress;
    }

    // Process State Changes
    BlockchainState.userNonces[minerAddress] = currentNonce + 1;
    BlockchainState.balances[minerAddress] = BlockchainState.getBalance(minerAddress) + BlockchainState.baseMiningReward;
    BlockchainState.totalSupply += BlockchainState.baseMiningReward;

    let logLines = [`[+] Block successfully mined by ${minerAddress}! Reward: +${BlockchainState.baseMiningReward} ${BlockchainState.symbol}`];

    // Process Multi-Tier Referral Payouts
    let currentReferrer = BlockchainState.referrers[minerAddress];
    for (let i = 0; i < BlockchainState.tierPercentages.length; i++) {
        if (!currentReferrer) break;

        const referralReward = BlockchainState.baseMiningReward * BlockchainState.tierPercentages[i];
        BlockchainState.balances[currentReferrer] = BlockchainState.getBalance(currentReferrer) + referralReward;
        BlockchainState.totalSupply += referralReward;

        logLines.push(`    [->] Tier ${i + 1} Referral Paid to ${currentReferrer}: +${referralReward} ${BlockchainState.symbol}`);
        currentReferrer = BlockchainState.referrers[currentReferrer];
    }

    return { success: true, logs: logLines };
}

function processLocalAirdrop(recipientsList, amountPerWallet) {
    let totalAirddropped = 0;
    recipientsList.forEach(wallet => {
        BlockchainState.balances[wallet] = BlockchainState.getBalance(wallet) + amountPerWallet;
        totalAirddropped += amountPerWallet;
    });
    BlockchainState.totalSupply += totalAirddropped;
    return totalAirddropped;
}

// --- Console Terminal System ---
const rl = readline.createInterface({ input: process.stdin, output: process.stdout });

function runCliMenu() {
    console.log(`
===================================================
   OFFLINE BLOCKCHAIN ENGINE & FAUCET CONTROLLER
===================================================
Total Supply: ${BlockchainState.totalSupply} ${BlockchainState.symbol}
---------------------------------------------------
1. View Wallet Balances & Nonces
2. Run Mining Faucet Engine
3. Trigger Batch Airdrop Distribution
4. Exit System
===================================================`);
    rl.question('Action > ', (choice) => {
        switch (choice.trim()) {
            case '1':
                console.log("\n--- CURRENT STATE STORAGE ---");
                console.log(JSON.stringify({
                    Balances: BlockchainState.balances,
                    NextRequiredNonces: BlockchainState.userNonces,
                    ReferralLinks: BlockchainState.referrers
                }, null, 2));
                runCliMenu();
                break;

            case '2':
                rl.question('Miner Wallet Address (e.g., 0xAlice): ', (miner) => {
                    const currentNonce = BlockchainState.getNonce(miner);
                    console.log(`[*] System expects active Nonce constraint: ${currentNonce}`);
                    rl.question('Enter Computed Nonce String: ', (nonce) => {
                        rl.question('Referrer Wallet Address (Optional, press enter to skip): ', (ref) => {
                            const result = localMineFaucet(miner.trim(), nonce.trim(), ref.trim());
                            if (result.success) {
                                console.log(`\n\x1b[32m${result.logs.join('\n')}\x1b[0m`);
                            } else {
                                console.log(`\n\x1b[31m[-] Mining Rejected: ${result.reason}\x1b[0m`);
                            }
                            runCliMenu();
                        });
                    });
                });
                break;

            case '3':
                rl.question('Enter Comma-Separated Wallets (e.g. 0xBob,0xCharlie): ', (walletsInput) => {
                    rl.question('Token Amount per Wallet: ', (amt) => {
                        const targets = walletsInput.split(',').map(w => w.trim());
                        const tokens = parseFloat(amt) || 0;
                        const total = processLocalAirdrop(targets, tokens);
                        console.log(`\n\x1b[32m[+] Batch Airdrop Dispatched! Distributed ${total} ${BlockchainState.symbol} across ${targets.length} wallets.\x1b[0m`);
                        runCliMenu();
                    });
                });
                break;

            case '4':
                console.log('Shutting down local node.');
                rl.close();
                break;

            default:
                console.log('Unknown action.');
                runCliMenu();
        }
    });
}

runCliMenu();
