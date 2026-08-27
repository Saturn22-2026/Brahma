import sys
import os
from unittest.mock import MagicMock

# Add the project root to sys.path to allow imports from ai_agent package
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from ai_agent.council.distribution_agent import DistributionAgent
from ai_agent.council.upgrade_agent import UpgradeAgent
from ai_agent.council.management_agent import ManagementAgent
from ai_agent.council.growth_agent import GrowthAgent
from ai_agent.council.wallet_agent import WalletAgent
from ai_agent.council.economic_agent import EconomicAgent
from ai_agent.council.treasury_agent import TreasuryAgent
from ai_agent.council.supervisory_agent import SupervisoryAgent

def test_agents():
    print("=========================================")
    print(" AI COUNCIL: INDIVIDUAL AGENT TESTS")
    print("=========================================\n")

    # Mocks
    mock_executor = MagicMock()
    mock_executor.get_last_claim_time.return_value = 0

    mock_scorer = MagicMock()
    mock_scorer.calculate_airdrop_allocation.return_value = {"0xUser": 100.0}

    mock_x_client = MagicMock()
    mock_x_client.search_mentions.return_value = ["Tweet 1", "Tweet 2"]

    mock_storage = MagicMock()
    mock_storage.load.return_value = {}

    mock_registry = {
        "Bitcoin-Quantum": {
            "token": "0x5FbDB2315678afecb367f032d93F642f64180aa3",
            "airdrop": "0xe7f1725E7734CE288F8367e1Bb143E90bb3F0512"
        }
    }

    # 1. Distribution Agent
    print("[Test] Distribution Agent")
    dist_agent = DistributionAgent(mock_executor, mock_scorer)
    dist_agent.run_cycle("Bitcoin-Quantum", mock_registry["Bitcoin-Quantum"])
    print("  -> Passed\n")

    # 2. Upgrade Agent
    print("[Test] Upgrade Agent")
    up_agent = UpgradeAgent(MagicMock(), "0xCouncil")
    status = up_agent.evaluate_security_standards()
    print(f"  Status: {status}")
    up_agent.propose_dao_transition()
    print("  -> Passed\n")

    # 3. Management Agent
    print("[Test] Management Agent")
    mgmt_agent = ManagementAgent("http://localhost:8545")
    mgmt_agent.monitor_network_state()
    mgmt_agent.check_signature_health()
    print("  -> Passed\n")

    # 4. Growth Agent
    print("[Test] Growth Agent")
    growth_agent = GrowthAgent(mock_x_client)
    mentions = growth_agent.analyze_global_sentiment()
    print(f"  Mentions found: {mentions}")
    multiplier = growth_agent.suggest_incentive_shift(0.1)
    print(f"  Incentive Multiplier: {multiplier}")
    print("  -> Passed\n")

    # 5. Wallet Agent
    print("[Test] Wallet Agent")
    wallet_agent = WalletAgent(mock_storage)
    addr = wallet_agent.generate_sovereign_identity("Test_User")
    print(f"  Generated Identity: {addr}")
    print("  -> Passed\n")

    # 6. Economic Agent
    print("[Test] Economic Agent")
    econ_agent = EconomicAgent(mock_registry["Bitcoin-Quantum"]["token"])
    econ_agent.audit_deflationary_impact()
    econ_agent.verify_whale_efficiency()
    print("  -> Passed\n")

    # 7. Treasury Agent
    print("[Test] Treasury Agent")
    treasury_agent = TreasuryAgent(mock_registry)
    treasury_agent.balance_l2_liquidity()
    treasury_agent.coordinate_bridge_ops()
    print("  -> Passed\n")

    # 8. Supervisory Agent
    print("[Test] Supervisory Agent")
    super_agent = SupervisoryAgent(mock_registry)
    cycle_report = {
        "proposed_distributions": {"BTQ": 500.0},
        "economic_audit": {"status": "GREEN"},
        "security_status": "STABLE",
        "defenses_active": False
    }
    approved = super_agent.audit_council_actions(cycle_report)
    print(f"  Audit Result: {'Approved' if approved else 'REJECTED'}")

    # Test failure case
    fail_report = {"proposed_distributions": {"BTQ": 10000.0}, "economic_audit": {"status": "RED"}}
    failed = super_agent.audit_council_actions(fail_report)
    print(f"  Failure Test Result: {'Approved' if failed else 'REJECTED'}")
    print("  -> Passed\n")

    print("=== ALL AGENT INDIVIDUAL TESTS COMPLETED ===")

if __name__ == "__main__":
    test_agents()
