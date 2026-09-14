import json
import time
from http.server import BaseHTTPRequestHandler, HTTPServer
import threading

# --- MAINNET GENESIS STATE ---
BALANCES = {}

class MainnetHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        try:
            content_length = int(self.headers.get('Content-Length', 0))
            post_data = self.rfile.read(content_length)
            request = json.loads(post_data)
            method = request.get("method")
            params = request.get("params", [])

            print(f"RPC Request: {method}")

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
                    BALANCES[addr] = {
                        "0": 1000.0,
                        "1": 10000.0, "2": 10000.0, "3": 1000.0, "4": 1000.0,
                        "5": 1000.0, "6": 1000.0, "7": 1000.0, "8": 1000.0
                    }
                result = BALANCES.get(addr)
            elif method == "btq_requestFaucet":
                addr = params[0]
                if addr in BALANCES:
                    BALANCES[addr]["0"] += 100.0
                result = "Sovereign Faucet: 100 BTQ Dispatched"
            elif method == "btq_mine":
                addr = params[0]
                if addr in BALANCES:
                    BALANCES[addr]["0"] += 0.1
                result = "Mainnet Block Discovery: 0.1 BTQ Reward"
            elif method == "btq_sendTransaction":
                result = "0x" + "f"*64
            elif method == "btq_requestSystemDrip":
                addr = params[0]
                amount = params[1]
                if addr in BALANCES:
                    BALANCES[addr]["0"] += amount
                result = "AI Sentinel: Airdrop Dispatched"

            response = {"jsonrpc": "2.0", "id": request.get("id", 1), "result": result, "error": error}
            body = json.dumps(response).encode('utf-8')

            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Content-Length', len(body))
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()
            self.wfile.write(body)
        except Exception as e:
            print(f"Error: {e}")
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

def run_node():
    server = HTTPServer(('0.0.0.0', 8545), MainnetHandler)
    print("🚀 BITCOIN-QUANTUM MAINNET NODE IS LIVE ON PORT 8545")
    server.serve_forever()

if __name__ == "__main__":
    run_node()
