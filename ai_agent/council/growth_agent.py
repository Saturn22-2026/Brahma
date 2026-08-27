from ..x_client import XAPIClient

class GrowthAgent:
    """
    Agent 4: Active growth engine.
    Analyzes sentiment and adjusts incentives.
    """
    def __init__(self, x_client: XAPIClient):
        self.x_client = x_client

    def analyze_global_sentiment(self):
        print("  [Growth AI] Scanning social sentiment for adoption triggers...")
        mentions = self.x_client.search_mentions("Bitcoin-Quantum")
        # analyze mentions...
        return len(mentions)

    def suggest_incentive_shift(self, sentiment_score: float):
        if sentiment_score < 0.2:
            print("  [Growth AI] Action: Increasing faucet multiplier due to low sentiment.")
            return 2.0 # Double rewards
        return 1.0
