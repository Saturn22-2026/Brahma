import os
import httpx
from typing import List, Dict

class XAPIClient:
    """
    Handles communication with the X (Twitter) API to fetch community engagement data.
    """
    def __init__(self, api_key: str):
        self.api_key = api_key
        self.base_url = "https://api.twitter.com/2"
        self.headers = {
            "Authorization": f"Bearer {self.api_key}"
        }

    def search_mentions(self, query: str = "Bitcoin-Quantum OR SQT") -> List[Dict]:
        """
        Searches for recent tweets mentioning the project.
        (Note: In production, this requires the Search Tweets endpoint permissions)
        """
        print(f"[X API] Searching for mentions: {query}")
        # Simulation of API response for the purpose of the airdrop flow
        # In a real environment, we would use: httpx.get(f"{self.base_url}/tweets/search/recent", params={"query": query})
        return [
            {"author_id": "12345", "text": "Analyzing the Sovereign bonding curve. The 2.5% Whale Tax is brilliant! #SQT"},
            {"author_id": "67890", "text": "Just bridged my BTQ to Base via CCIP. Smooth! #BitcoinQuantum"},
            {"author_id": "11111", "text": "LFG MOON SOON! #SQT #BTC"}
        ]

    def get_user_metrics(self, user_id: str) -> Dict:
        """
        Fetches account metrics (age, follower count) for sybil resistance.
        """
        # Mocking user metrics
        metrics = {
            "12345": {"created_at": "2020-01-01", "followers": 1500, "tweets": 450},
            "67890": {"created_at": "2021-06-15", "followers": 300, "tweets": 120},
            "11111": {"created_at": "2026-07-28", "followers": 2, "tweets": 5} # Suspected Bot
        }
        return metrics.get(user_id, {})
