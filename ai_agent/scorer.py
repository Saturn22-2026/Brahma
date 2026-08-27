import hashlib
from dataclasses import dataclass
from typing import List, Dict, Tuple

@dataclass
class CommunityUser:
    handle: str
    wallet_address: str
    tweets: List[str]
    discord_messages: int
    on_chain_loyalty_days: int

class MarketingAIScorer:
    """
    Uses AI heuristics to score user engagement and filter out bots.
    """
    def __init__(self):
        # In production, this would load an LLM/NLP model.
        # For this implementation, we use keyword-based scoring.
        self.technical_keywords = ["Whale Tax", "Bonding Curve", "Quantum-Resistant", "ML-DSA", "Sovereign"]
        self.hype_keywords = ["LFG", "moon", "pumping", "rocket"]

    def _ai_content_score(self, tweets: List[str]) -> float:
        """
        Rewards technical knowledge and penalizes generic low-effort hype.
        """
        score = 0.0
        for tweet in tweets:
            if len(tweet) < 20:
                continue # Ignore very short spam

            # Technical rewards
            for kw in self.technical_keywords:
                if kw.lower() in tweet.lower():
                    score += 5.0

            # Hype penalties
            for kw in self.hype_keywords:
                if kw.lower() in tweet.lower():
                    score -= 1.0

        return max(score, 0.0)

    def _sybil_resistance_check(self, user: CommunityUser) -> bool:
        """
        Filters out low-loyalty accounts and suspected bots.
        """
        if user.discord_messages < 5:
            return False # Not enough activity
        if user.on_chain_loyalty_days < 7:
            return False # Account too new
        return True

    def calculate_airdrop_allocation(self, users: List[CommunityUser], total_budget: float) -> Dict[str, float]:
        """
        Distributes the monthly budget proportionally based on engagement scores.
        """
        print(f"[AI Scorer] Analyzing {len(users)} potential recipients...")

        scored_users = []
        total_network_score = 0.0

        for user in users:
            if not self._sybil_resistance_check(user):
                print(f"  [AI Filter] Rejected {user.handle} (Sybil/Low Activity)")
                continue

            content_score = self._ai_content_score(user.tweets)
            # Weighted Final Score: 60% Content, 30% On-chain Loyalty, 10% Discord
            final_score = (content_score * 0.6) + (user.on_chain_loyalty_days * 0.3) + (user.discord_messages * 0.1)

            if final_score > 0:
                scored_users.append((user.wallet_address, final_score))
                total_network_score += final_score

        if total_network_score == 0:
            return {}

        # Calculate proportional shares
        allocations = {}
        for addr, score in scored_users:
            share = score / total_network_score
            allocations[addr] = share * total_budget

        print(f"[AI Scorer] Approved {len(allocations)} users. Total Network Score: {total_network_score:.2f}")
        return allocations
