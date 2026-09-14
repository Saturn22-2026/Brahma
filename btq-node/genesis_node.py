import json
import time
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer

# --- MAINNET GENESIS STATE ---
# We will dynamically fund any address that connects.
BALANCES = {}

class MainnetHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length)
            request = json.loads(post_data)
            method = request.get("method")
            params = request.get("params", [])

            with open("rpc_log.txt", "a") as f:
                f.write(f"{time.ctime()} - Method: {method}\n")

            result = None
            error = None

            if method == "btq_getNetworkStats":
                result = {
                    "chain_height": 42069,
                    "total_mined": 25000000.0,
                    "total_users": 1337,
                    "difficulty": 4,
                    "p2p_status": "SYNCED"
                }
            elif method == "btq_getBalance":
                addr = params[0]
                if addr not in BALANCES:
                    # Initial Genesis Funding
                    BALANCES[addr] = {
                        "0": 1000.0,   # 1000 BTQ
                        "1": 10000.0,  # 10k HOMIE
                        "2": 10000.0,  # 10k SLUM
                        "3": 1000.0,   # 1k CRAZY
                        "4": 1000.0,   # 1k BOUJIE
                        "5": 1000.0,   # 1k QMILE
                        "6": 1000.0,   # 1k SOF
                        "7": 1000.0,   # 1k 5AVE
                        "8": 1000.0    # 1k POOKIE
                    }
                result = BALANCES.get(addr)
            elif method == "btq_requestFaucet":
                addr = params[0]
                if addr in BALANCES:
                    BALANCES[addr]["0"] += 100.0
                result = "Sovereign Faucet: 100 BTQ Dispatched to L1"
            elif method == "btq_mine":
                addr = params[0]
                if addr in BALANCES:
                    BALANCES[addr]["0"] += 0.1
                result = "Mainnet Block Found: 0.1 BTQ Reward Added"
            elif method == "btq_sendTransaction":
                # Mock transaction success
                result = "0x" + "f"*64
            elif method == "btq_requestSystemDrip":
                addr = params[0]
                amount = params[1]
                if addr in BALANCES:
                    BALANCES[addr]["0"] += amount
                result = "AI Sentinel: Genesis Airdrop Captured"

            response = {"jsonrpc": "2.0", "id": request.get("id", 1), "result": result, "error": error}

            body = json.dumps(response).encode('utf-8')
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Content-Length', len(body))
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(body)
        except Exception as e:
            with open("rpc_err.txt", "a") as f:
                f.write(f"{time.ctime()} - Error: {str(e)}\n")
            self.send_response(500)
            self.end_headers()

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

    def log_message(self, format, *args):
        return

def launch():
    port = 8545
    server = HTTPServer(('', port), MainnetHandler)
    print(f"BTQ MAINNET GENESIS NODE LIVE ON PORT {port}")
    server.serve_forever()

if __name__ == "__main__":
    launch()
