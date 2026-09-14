import json
import time
from http.server import BaseHTTPRequestHandler, HTTPServer

DATA_FILE = "btq_mock_data.json"

def load_data():
    try:
        with open(DATA_FILE, "r") as f:
            return json.load(f)
    except:
        return {"balances": {}, "total_users": 0, "chain_height": 3600, "total_mined": 150000.0}

def save_data(data):
    with open(DATA_FILE, "w") as f:
        json.dump(data, f)

class BTQHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        request = json.loads(post_data)
        method = request.get("method")
        params = request.get("params", [])

        response_result = None
        error = None

        data = load_data()

        if method == "btq_getNetworkStats":
            response_result = {
                "chain_height": data["chain_height"],
                "total_mined": data["total_mined"],
                "total_users": len(data["balances"]),
                "difficulty": 4,
                "p2p_status": "CONNECTED"
            }
        elif method == "btq_getBalance":
            addr = params[0]
            # Return balances for asset 0 (BTQ) and assets 1-8 (L2)
            response_result = data["balances"].get(addr, {"0": 0.0})
        elif method == "btq_requestFaucet":
            addr = params[0]
            if addr not in data["balances"]:
                data["balances"][addr] = {"0": 100.0}
                # Boost with L2 coins
                for i in range(1, 9):
                    data["balances"][addr][str(i)] = 1000.0
                save_data(data)
                response_result = "Received 100 BTQ and L2 Sovereignty Pack"
            else:
                error = {"code": -32602, "message": "24h cooldown active"}
        elif method == "btq_mine":
            addr = params[0]
            if addr not in data["balances"]:
                data["balances"][addr] = {"0": 0.0}

            reward = 0.1
            data["balances"][addr]["0"] += reward
            data["total_mined"] += reward
            data["chain_height"] += 1
            save_data(data)
            response_result = f"Block Mined. New Balance: {data['balances'][addr]['0']}"
        elif method == "btq_sendTransaction":
            tx = params[0]
            sender = tx["sender"]
            receiver = tx["receiver"]
            amount = tx["amount"]
            asset_id = str(tx["asset_id"])

            if data["balances"].get(sender, {}).get(asset_id, 0.0) >= amount:
                data["balances"][sender][asset_id] -= amount
                if receiver not in data["balances"]:
                    data["balances"][receiver] = {"0": 0.0}
                data["balances"][receiver][asset_id] = data["balances"][receiver].get(asset_id, 0.0) + amount
                save_data(data)
                response_result = "0x" + "a"*64 # Mock tx hash
            else:
                error = {"code": -32603, "message": "Insufficient balance"}
        elif method == "btq_requestSystemDrip":
            addr = params[0]
            amount = params[1]
            if addr not in data["balances"]:
                data["balances"][addr] = {"0": 0.0}
            data["balances"][addr]["0"] += amount
            save_data(data)
            response_result = f"System drip of {amount} BTQ initialized"

        response = {
            "jsonrpc": "2.0",
            "id": request.get("id", 1),
            "result": response_result,
            "error": error
        }

        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()
        self.wfile.write(json.dumps(response).encode())

    def do_OPTIONS(self):
        self.send_response(200)
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'POST, GET, OPTIONS')
        self.send_header('Access-Control-Allow-Headers', 'Content-Type')
        self.end_headers()

def run(server_class=HTTPServer, handler_class=BTQHandler, port=8545):
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    print(f'Starting BTQ Mock Node on port {port}...')
    httpd.serve_forever()

if __name__ == "__main__":
    run()
