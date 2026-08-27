import time
import uuid

class MockBlockchainCore:
    def __init__(self):
        self.faucet_pool = 1000000.0
        self.faucet_users_count = 0
        self.faucet_tier2_active = False
        self.faucet_tier3_active = False
        self.faucet_pool_t2 = 1000000.0
        self.faucet_pool_t3 = 1000000.0
        self.faucet_claims = {}

    def process_faucet_request(self, address):
        now = int(time.time())

        # Cooldown check
        if address in self.faucet_claims:
            if now < self.faucet_claims[address] + 86400:
                return "ERROR: 24h cooldown active"

        # Tiered logic
        if self.faucet_users_count < 10000:
            # Tier 1
            if self.faucet_pool < 100.0:
                return "ERROR: Tier 1 Pool Depleted"
            amount = 100.0
            self.faucet_pool -= amount
        elif self.faucet_tier2_active:
            # Tier 2
            if self.faucet_pool_t2 < 50.0:
                return "ERROR: Tier 2 Pool Depleted"
            amount = 50.0
            self.faucet_pool_t2 -= amount
        elif self.faucet_tier3_active:
            # Tier 3
            if self.faucet_pool_t3 < 25.0:
                return "ERROR: Tier 3 Pool Depleted"
            amount = 25.0
            self.faucet_pool_t3 -= amount
        else:
            return "ERROR: Bootstrap Period Complete. No further drips available without AI Consensus."

        self.faucet_users_count += 1
        self.faucet_claims[address] = now
        return f"SUCCESS: Received {amount} BTQ"

def run_diagnostic():
    print("=== BTQ FAUCET TIER DIAGNOSTIC ===")
    core = MockBlockchainCore()

    # 1. Test Tier 1 normal operation
    print("\n[Test 1] Testing Tier 1 (100 BTQ)...")
    res = core.process_faucet_request("0xUser1")
    print(f"  Request 1: {res}")

    # 2. Test cooldown
    print("\n[Test 2] Testing 24h Cooldown...")
    res = core.process_faucet_request("0xUser1")
    print(f"  Request 2 (same address): {res}")

    # 3. Fast-forward to Tier 1 Exhaustion
    print("\n[Test 3] Simulating Tier 1 Exhaustion (10,000 users)...")
    core.faucet_users_count = 10000
    res = core.process_faucet_request("0xUser10001")
    print(f"  Request 10,001 (Tier 2 Not Active): {res}")

    # 4. Activate Tier 2 via AI Consensus
    print("\n[Test 4] Activating Tier 2 (50 BTQ)...")
    core.faucet_tier2_active = True
    res = core.process_faucet_request("0xUser10001")
    print(f"  Request 10,001 (Tier 2 Active): {res}")

    # 5. Fast-forward to Tier 2 Exhaustion
    print("\n[Test 5] Simulating Tier 2 Exhaustion (30,000 users)...")
    core.faucet_users_count = 30000
    core.faucet_tier2_active = False # Assume AI deactivates or it hits user limit
    res = core.process_faucet_request("0xUser30001")
    print(f"  Request 30,001 (Tier 3 Not Active): {res}")

    # 6. Activate Tier 3 via AI Consensus
    print("\n[Test 6] Activating Tier 3 (25 BTQ)...")
    core.faucet_tier3_active = True
    res = core.process_faucet_request("0xUser30001")
    print(f"  Request 30,001 (Tier 3 Active): {res}")

    print("\n=== FAUCET DIAGNOSTIC COMPLETE ===")

if __name__ == "__main__":
    run_diagnostic()
