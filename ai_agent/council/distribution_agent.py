import time
from typing import Dict, List
from ..scorer import MarketingAIScorer, CommunityUser
from ..executor import OnChainAirdropAgent

class DistributionAgent:
    """
    Agent 1: Handles Airdrops, Allocations, and Drips across all assets.
    """
    def __init__(self, agent_executor: OnChainAirdropAgent, scorer: MarketingAIScorer):
        self.executor = agent_executor
        self.scorer = scorer

    def run_cycle(self, asset_name: str, asset_data: Dict):
        print(f"  [Distribution AI] Processing {asset_name}...")

        # 1. Linear Airdrop Drip
        airdrop_addr = asset_data.get("airdrop")
        if airdrop_addr:
            try:
                last_claim = self.executor.get_last_claim_time(airdrop_addr)
                if int(time.time()) >= last_claim + 86400:
                    self.executor.trigger_daily_drip(airdrop_addr)
            except Exception as e:
                print(f"    [Error] Drip failed: {e}")

        # 2. Community Engagement Grants (Monthly)
        token_addr = asset_data.get("token")
        if token_addr:
            # Simulated community data fetch
            mock_users = [CommunityUser("tech_lead", "0x123", ["BTQ PQC is the future"], 20, 100)]
            allocations = self.scorer.calculate_airdrop_allocation(mock_users, 1000.0)
            if allocations:
                self.executor.execute_community_grant(token_addr, allocations)
