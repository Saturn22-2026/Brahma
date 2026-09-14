const http = require('http');

const BALANCES = {};

const server = http.createServer((req, res) => {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        res.writeHead(200);
        res.end();
        return;
    }

    if (req.method === 'POST') {
        let body = '';
        req.on('data', chunk => {
            body += chunk.toString();
        });
        req.on('end', () => {
            try {
                const request = JSON.parse(body);
                const method = request.method;
                const params = request.params || [];
                let result = null;
                let error = null;

                console.log(`[RPC] ${method}`);

                if (method === "btq_getNetworkStats") {
                    result = {
                        chain_height: 42069,
                        total_mined: 25000000.0,
                        total_users: 1337,
                        difficulty: 4,
                        p2p_status: "SYNCED"
                    };
                } else if (method === "btq_getBalance") {
                    const addr = params[0];
                    if (!BALANCES[addr]) {
                        BALANCES[addr] = {
                            "0": 1000.0,
                            "1": 10000.0, "2": 10000.0, "3": 1000.0, "4": 1000.0,
                            "5": 1000.0, "6": 1000.0, "7": 1000.0, "8": 1000.0
                        };
                    }
                    result = BALANCES[addr];
                } else if (method === "btq_requestFaucet") {
                    const addr = params[0];
                    if (BALANCES[addr]) BALANCES[addr]["0"] += 100.0;
                    result = "Sovereign Faucet: 100 BTQ Dispatched";
                } else if (method === "btq_mine") {
                    const addr = params[0];
                    if (BALANCES[addr]) BALANCES[addr]["0"] += 0.1;
                    result = "Mainnet Block Found: 0.1 BTQ Reward";
                } else if (method === "btq_sendTransaction") {
                    result = "0x" + "f".repeat(64);
                } else if (method === "btq_requestSystemDrip") {
                    const addr = params[0];
                    const amount = params[1];
                    if (BALANCES[addr]) BALANCES[addr]["0"] += amount;
                    result = "AI Sentinel: Airdrop Dispatched";
                }

                const response = JSON.stringify({
                    jsonrpc: "2.0",
                    id: request.id || 1,
                    result: result,
                    error: error
                });

                res.writeHead(200, { 'Content-Type': 'application/json', 'Content-Length': Buffer.byteLength(response) });
                res.end(response);
            } catch (e) {
                console.error(e);
                res.writeHead(500);
                res.end("Internal Server Error");
            }
        });
    } else {
        res.writeHead(404);
        res.end();
    }
});

const PORT = 8545;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`🚀 BTQ MAINNET NODE LIVE ON PORT ${PORT}`);
});
