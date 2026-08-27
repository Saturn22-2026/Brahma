import pytest
from ..scorer import MarketingAIScorer, CommunityUser

def test_ai_scoring_heuristics():
    """
    Verify that the AI Scorer correctly distinguishes between technical content and hype.
    """
    scorer = MarketingAIScorer()

    # 1. High-Quality Technical Analyst
    analyst = CommunityUser(
        handle="quant_dev",
        wallet_address="0x1",
        tweets=[
            "Deep dive into BTQ's ML-DSA implementation. The lattice-based security is a major upgrade over ECDSA.",
            "The 2.5% Whale Tax threshold ensures the bonding curve remains stable during high volume events."
        ],
        discord_messages=50,
        on_chain_loyalty_days=100
    )

    # 2. Low-Effort Hype Bot
    hype_bot = CommunityUser(
        handle="moon_boy",
        wallet_address="0x2",
        tweets=["LFG BTQ TO THE MOON!!! 🚀🚀🚀", "PUMP IT UP! #BTQ #CRYPTO"],
        discord_messages=20,
        on_chain_loyalty_days=10
    )

    # 3. Sybil Farmer (New account, low activity)
    farmer = CommunityUser(
        handle="farmer_joe",
        wallet_address="0x3",
        tweets=["Nice project.", "I like Bitcoin-Quantum."],
        discord_messages=2,
        on_chain_loyalty_days=1
    )

    users = [analyst, hype_bot, farmer]
    allocations = scorer.calculate_airdrop_allocation(users, 1000.0)

    # Assertions
    assert "0x1" in allocations, "Analyst should be approved"
    assert "0x2" in allocations, "Hype bot might be approved but with lower score"
    assert "0x3" not in allocations, "Farmer should be rejected by Sybil filter"

    assert allocations["0x1"] > allocations["0x2"], "Technical analyst should receive higher allocation than hype bot"

    print("\n[AI Intelligence] Test Passed: Technical content prioritized over hype.")

def test_budget_proportionality():
    """
    Ensure the distribution is perfectly proportional to the scores.
    """
    scorer = MarketingAIScorer()
    user_a = CommunityUser("a", "0xA", ["Sovereign Network is great."], 10, 20)
    user_b = CommunityUser("b", "0xB", ["Sovereign Network is great."], 10, 20)

    # They have identical engagement, should get identical allocation
    users = [user_a, user_b]
    allocations = scorer.calculate_airdrop_allocation(users, 1000.0)

    assert allocations["0xA"] == 500.0
    assert allocations["0xB"] == 500.0
    print("[AI Intelligence] Test Passed: Budget distribution is mathematically proportional.")

if __name__ == "__main__":
    test_ai_scoring_heuristics()
    test_budget_proportionality()
