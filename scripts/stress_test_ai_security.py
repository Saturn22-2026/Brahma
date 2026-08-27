import sys
import os
import json

# Add the project root to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from ai_agent.council.supervisory_agent import SupervisoryAgent

def stress_test_supervisor():
    print("=========================================")
    print("AI COUNCIL SECURITY STRESS TEST: AGENT 8")
    print("=========================================\n")

    registry = {"Bitcoin-Quantum": {"token": "0x123"}}
    supervisor = SupervisoryAgent(registry)

    # Test 1: Airdrop Overflow Attack
    print("[Attack 1] Injecting Airdrop Overflow...")
    bad_report = {
        "proposed_distributions": {"UserA": 5000.0}, # Daily cap is ~2739
        "economic_audit": {"status": "GREEN"},
        "security_status": "STABLE",
        "defenses_active": False
    }
    approved = supervisor.audit_council_actions(bad_report)
    print(f"  Result: {'FAILED (Security Hole)' if approved else 'PASSED (Attack Blocked)'}\n")

    # Test 2: Economic Discrepancy Attack
    print("[Attack 2] Injecting Burn Discrepancy...")
    discrepancy_report = {
        "proposed_distributions": {"UserA": 100.0},
        "economic_audit": {"status": "RED"}, # Failed burn check
        "security_status": "STABLE",
        "defenses_active": False
    }
    approved = supervisor.audit_council_actions(discrepancy_report)
    print(f"  Result: {'FAILED (Security Hole)' if approved else 'PASSED (Attack Blocked)'}\n")

    # Test 3: Security Bypass Attack
    print("[Attack 3] Injecting Defenseless Critical State...")
    critical_report = {
        "proposed_distributions": {},
        "economic_audit": {"status": "GREEN"},
        "security_status": "CRITICAL",
        "defenses_active": False # Critical state but no defense deployed
    }
    approved = supervisor.audit_council_actions(critical_report)
    print(f"  Result: {'FAILED (Security Hole)' if approved else 'PASSED (Attack Blocked)'}\n")

    print("=== AI SECURITY STRESS COMPLETED ===")

if __name__ == "__main__":
    stress_test_supervisor()
