import time
import random
import hashlib
import sys
import os
from unittest.mock import MagicMock

# Add the project root to sys.path so we can import from ai_agent
sys.path.append(os.path.abspath(os.path.join(os.getcwd(), "ai_agent")))

from scorer import MarketingAIScorer, CommunityUser
from executor import OnChainAirdropAgent
from council.supervisory_agent import SupervisoryAgent

def stress_test_scorer():
    print("--- Starting AI Scorer Stress Test ---")
    scorer = MarketingAIScorer()

    # Generate 5000 users with various profiles
    users = []
    for i in range(5000):
        # 10% high-quality, 30% hype, 60% low-activity/bots
        rand = random.random()
        if rand < 0.1:
            tweets = ["Deep dive into ML-DSA and Sovereign lattice security.",
                      "Whale Tax at 2.5% is the perfect stabilizer for the Bonding Curve."]
            users.append(CommunityUser(f"dev_{i}", f"0x{i:040x}", tweets, random.randint(50, 200), random.randint(30, 365)))
        elif rand < 0.4:
            tweets = ["LFG moon BTQ!", "Pumping hard!"]
            users.append(CommunityUser(f"hype_{i}", f"0x{i:040x}", tweets, random.randint(10, 50), random.randint(7, 30)))
        else:
            tweets = ["Nice", "Cool"]
            users.append(CommunityUser(f"bot_{i}", f"0x{i:040x}", tweets, random.randint(0, 4), random.randint(0, 6)))

    start_time = time.time()
    allocations = scorer.calculate_airdrop_allocation(users, 2739.0)
    end_time = time.time()

    print(f"Processed 5000 users in {end_time - start_time:.4f} seconds.")
    print(f"Approved recipients: {len(allocations)}")
    print(f"Total distributed: {sum(allocations.values()):.2f}")

    # Verifying specific edge case: Keyword stuffing
    spammer = CommunityUser("spammer", "0xBAD", ["ML-DSA ML-DSA ML-DSA Whale Tax Bonding Curve Sovereign" for _ in range(5)], 100, 100)
    res = scorer.calculate_airdrop_allocation([spammer], 1000.0)
    if not res:
        print("[Pass] Keyword stuffing bot rejected correctly (negative score).")
    else:
        print("[Fail] Keyword stuffing bot was not rejected.")

def stress_test_agent_quorum():
    print("\n--- Starting OnChain Agent Quorum Stress Test ---")
    # Mock Web3
    mock_w3 = MagicMock()
    mock_w3.eth.account.from_key.return_value = MagicMock(address="0xAgent")

    agent = OnChainAirdropAgent("http://localhost:8545", "0xContract", "0x" + "a" * 64)
    agent.w3 = mock_w3

    # Test quorum with duplicate signatures (should still only count unique)
    action = "0xACTION_HASH"
    for _ in range(10):
        agent.collect_agent_signature("agent_1", action)

    print("Collected 10 signatures from agent_1. Verifying quorum...")
    if not agent._verify_quorum():
        print("[Pass] Duplicate signatures correctly counted as 1 unique.")

    # Add unique ones until quorum reached
    for i in range(2, 6):
        agent.collect_agent_signature(f"agent_{i}", action)

    if agent._verify_quorum():
        print("[Pass] 5 unique signatures correctly achieved quorum.")

def stress_test_supervisor():
    print("\n--- Starting Supervisory AI Stress Test ---")
    supervisor = SupervisoryAgent({"BTQ": {}})

    # Large payload test
    large_distributions = {f"0x{i:040x}": 1.0 for i in range(3000)}
    report = {
        "proposed_distributions": large_distributions,
        "economic_audit": {"status": "GREEN"},
        "security_status": "STABLE",
        "defenses_active": False,
        "agent_attestations": {"a": "v", "b": "v"}
    }

    start_time = time.time()
    approved = supervisor.audit_council_actions(report)
    end_time = time.time()

    print(f"Audit of 3000 distributions took {end_time - start_time:.4f} seconds.")
    if not approved:
        print("[Pass] Correctly blocked distribution exceeding daily cap (3000 > 2739).")

if __name__ == "__main__":
    stress_test_scorer()
    stress_test_agent_quorum()
    stress_test_supervisor()
