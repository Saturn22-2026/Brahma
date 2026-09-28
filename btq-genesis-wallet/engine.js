const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const readline = require('readline');

// --- Configuration Management ---
const CONFIG_PATH = path.join(__dirname, 'config.json');
const SEED_HEX_LENGTH = 48;

function loadConfig() {
    try {
        if (fs.existsSync(CONFIG_PATH)) {
            return JSON.parse(fs.readFileSync(CONFIG_PATH, 'utf8'));
        }
    } catch (e) {
        console.error("Error loading config:", e.message);
    }
    return { referrer: null, is_first_boot: true };
}

function saveConfig(next) {
    try {
        fs.writeFileSync(CONFIG_PATH, JSON.stringify(next, null, 2));
        config = next;
    } catch (e) {
        console.error("Error saving config:", e.message);
    }
}

function persistWallet() {
    saveConfig({
        ...config,
        referrer: config.referrer ?? null,
        is_first_boot: config.is_first_boot === true,
        seed: BlockchainState.keys.seed,
        balances: BlockchainState.balances,
        nonces: BlockchainState.nonces,
        genesisStatus: BlockchainState.genesisStatus
    });
}

let config = loadConfig();

// --- Deterministic key derivation (simulated lattice labels; not BIP-39) ---
function seedToPhrase(seedHex) {
    return seedHex.match(/.{4}/g).join(' ');
}

function phraseToSeed(phrase) {
    const compact = String(phrase || '').trim().toLowerCase().replace(/\s+/g, '');
    if (!/^[0-9a-f]+$/.test(compact) || compact.length !== SEED_HEX_LENGTH) {
        throw new Error('Invalid seed phrase');
    }
    return compact;
}

function keysFromSeed(seedHex) {
    const seed = String(seedHex || '').trim().toLowerCase();
    if (!/^[0-9a-f]+$/.test(seed) || seed.length !== SEED_HEX_LENGTH) {
        throw new Error('Invalid seed');
    }
    const privateKey = crypto.createHash('sha512').update(seed + 'priv').digest('hex');
    const publicKey = crypto.createHash('sha512').update(seed + 'pub').digest('hex');
    const address = 'BTQ1G' + crypto.createHash('sha256').update(publicKey).digest('hex').substring(0, 20).toUpperCase();
    return { seed, privateKey, publicKey, address, seedPhrase: seedToPhrase(seed) };
}

function generateQuantumKeys() {
    return keysFromSeed(crypto.randomBytes(SEED_HEX_LENGTH / 2).toString('hex'));
}

function loadOrCreateKeys() {
    try {
        if (config.seed) {
            return keysFromSeed(config.seed);
        }
        if (config.seedPhrase) {
            return keysFromSeed(phraseToSeed(config.seedPhrase));
        }
    } catch (e) {
        console.error("Error restoring wallet keys:", e.message);
    }
    return generateQuantumKeys();
}

function defaultBalances() {
    return { BTQ: 0, HOMIE: 0, POOKIE: 0, CGOD: 0 };
}

function defaultNonces() {
    return { transactions: 0, mining: 0 };
}

// --- Blockchain State ---
const BlockchainState = {
    tokenName: "Bitcoin Quantum",
    symbol: "BTQ",
    balances: { ...defaultBalances(), ...(config.balances || {}) },
    nonces: { ...defaultNonces(), ...(config.nonces || {}) },
    keys: loadOrCreateKeys(),
    genesisStatus: config.genesisStatus || "None",

    addBalance(token, amount) {
        this.balances[token] = (this.balances[token] || 0) + amount;
        persistWallet();
    }
};

persistWallet();

// --- Sentinel Network Logic ---
function runSentinelChecks() {
    console.log(`[Sentinel] Checking network status for address: ${BlockchainState.keys.address}...`);

    if (config.is_first_boot) {
        console.log(`\x1b[32m[Sentinel] SUCCESS: Genesis 500 Status Confirmed!\x1b[0m`);
        BlockchainState.genesisStatus = "Genesis 500 Member";

        // Welcome Reward
        console.log(`[Sentinel] Granting Welcome Reward: 100 BTQ`);
        BlockchainState.addBalance("BTQ", 100);

        // Referral Bounty
        if (config.referrer) {
            console.log(`[Sentinel] Referral found (${config.referrer}). Granting Referral Bounty: 50 BTQ`);
            BlockchainState.addBalance("BTQ", 50);
        }

        config.is_first_boot = false;
        persistWallet();
    } else {
        console.log(`[Sentinel] Existing wallet session detected.`);
        BlockchainState.genesisStatus = BlockchainState.genesisStatus || "Genesis 500 Member";
    }
}

// --- AI Background Daemons ---
function startBackgroundDaemons() {
    // AI Miner: Mines 50 BTQ every 10 seconds
    setInterval(() => {
        BlockchainState.addBalance("BTQ", 50);
        BlockchainState.nonces.mining++;
        persistWallet();
        process.stdout.write(`\r\x1b[36m[AI Miner] Mined 50 BTQ (Block #${BlockchainState.nonces.mining})\x1b[0m\n > `);
    }, 10000);

    // AI Airdrop: Captures social airdrops every 15 seconds
    const airdropTokens = ["HOMIE", "POOKIE", "CGOD"];
    setInterval(() => {
        const token = airdropTokens[Math.floor(Math.random() * airdropTokens.length)];
        const amount = Math.floor(Math.random() * 50) + 10;
        BlockchainState.addBalance(token, amount);
        process.stdout.write(`\r\x1b[35m[AI Airdrop] Captured ${amount} ${token} social drop!\x1b[0m\n > `);
    }, 15000);

    // Omni-Faucet: Auto-claims 100 BTQ every 20 seconds
    setInterval(() => {
        BlockchainState.addBalance("BTQ", 100);
        process.stdout.write(`\r\x1b[33m[Omni-Faucet] Auto-claimed 100 BTQ from network\x1b[0m\n > `);
    }, 20000);
}

// --- Interactive CLI ---
const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    terminal: true
});

function showMenu() {
    console.log(`
===================================================
      BTQ GENESIS WALLET - QUANTUM INTERFACE
===================================================
Address: ${BlockchainState.keys.address}
Status:  ${BlockchainState.genesisStatus}
---------------------------------------------------
1. View Wallet Balances & Nonces
2. View Secret Seed Phrase
3. Generate Referral Link
4. Exit
===================================================`);
    askChoice();
}

function askChoice() {
    rl.question('Action > ', (choice) => {
        switch (choice.trim()) {
            case '1':
                console.log("\n--- WALLET ASSETS ---");
                for (const [token, balance] of Object.entries(BlockchainState.balances)) {
                    console.log(`${token}: ${balance.toLocaleString()} ${token}`);
                }
                console.log("--- SYSTEM NONCES ---");
                console.log(`Mining Cycles: ${BlockchainState.nonces.mining}`);
                console.log(`Transactions:  ${BlockchainState.nonces.transactions}`);
                showMenu();
                break;

            case '2':
                console.log("\n\x1b[31m!!! WARNING: DO NOT SHARE THIS WITH ANYONE !!!\x1b[0m");
                console.log(`Seed Phrase: ${BlockchainState.keys.seedPhrase}`);
                console.log("-----------------------------------------------");
                showMenu();
                break;

            case '3':
                const refLink = `https://btq.genesis/referral?id=${BlockchainState.keys.address.substring(5, 13)}`;
                console.log(`\n[Referral] Your unique link: ${refLink}`);
                console.log(`[Referral] Share this to earn 50 BTQ per referral!`);
                showMenu();
                break;

            case '4':
                persistWallet();
                console.log('Exiting BTQ Genesis Wallet...');
                process.exit(0);
                break;

            default:
                console.log('Invalid option.');
                askChoice();
        }
    });
}

// --- Initialization ---
console.clear();
runSentinelChecks();
startBackgroundDaemons();
showMenu();
