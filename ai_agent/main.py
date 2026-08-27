import os
import json
from scorer import MarketingAIScorer, CommunityUser
from executor import OnChainAirdropAgent

def load_mock_community_data():
    """
    Simulates fetching data from Twitter and Discord APIs.
    """
    return [
        CommunityUser(
            handle="whale_bot_1",
            wallet_address="0xAb5801a7D398351b8bE11C439e05C5B3259aeC9B",
            tweets=["LFG MOON SOV!", "BUY SOV NOW!"],
            discord_messages=2,
            on_chain_loyalty_days=2
        ),
        CommunityUser(
            handle="quantum_analyst",
            wallet_address="0x1234567890123456789012345678901234567890",
            tweets=[
                "The Sovereign bonding curve creates a natural price floor. This is true quantum resistance.",
                "ML-DSA implementation in BTQ is a game changer for security."
            ],
            discord_messages=150,
            on_chain_loyalty_days=45
        ),
        CommunityUser(
            handle="early_miner",
            wallet_address="0x4567890123456789012345678901234567890123",
            tweets=[
                "Just set up my L2 node. The smooth emission model is much better than Bitcoin's halvings.",
                "Sovereign Network is growing fast."
            ],
            discord_messages=80,
            on_chain_loyalty_days=30
        )
    ]

def run_airdrop_cycle():
    print("=== STARTING AUTONOMOUS AIRDROP CYCLE ===")

    # 1. Configuration (Mocked for local development)
    rpc_url = "http://127.0.0.1:8545"
    contract_addr = "0x5FbDB2315678afecb367f032d93F642f64180aa3"
    private_key = os.getenv("AI_AGENT_PRIVATE_KEY", "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80")

    # 2. Data Collection
    users = load_mock_community_data()

    # 3. AI Scoring
    scorer = MarketingAIScorer()
    # Initial budget for Month 1: 35,000 SQT
    monthly_budget = 35000.0
    allocations = scorer.calculate_airdrop_allocation(users, monthly_budget)

    print("\n--- FINAL ALLOCATIONS ---")
    for addr, amt in allocations.items():
        print(f"  {addr}: {amt:,.2f} SQT")

    # 4. On-Chain Execution
    agent = OnChainAirdropAgent(rpc_url, contract_addr, private_key)
    agent.execute_airdrop(allocations)

    print("\n=== AIRDROP CYCLE COMPLETE ===")

if __name__ == "__main__":
    run_airdrop_cycle()
