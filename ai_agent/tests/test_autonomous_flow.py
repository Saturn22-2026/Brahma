import pytest
from ..executor import OnChainAirdropAgent
from ..main import run_airdrop_cycle

def test_executor_budget_check():
    """
    Verify that the executor correctly halts if the calculated distribution exceeds the on-chain budget.
    """
    # Mocking a scenario with a tiny budget
    rpc_url = "http://localhost:8545" # Assume Anvil is running
    contract_addr = "0x5FbDB2315678afecb367f032d93F642f64180aa3"
    private_key = "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"

    agent = OnChainAirdropAgent(rpc_url, contract_addr, private_key)

    # Impossible allocation (1 Trillion SQT)
    impossible_allocations = {"0x123": 1_000_000_000_000.0}

    # This should log an error and return None (no tx broadcasted)
    tx = agent.execute_airdrop(impossible_allocations)
    assert tx is None
    print("\n[AI Flow] Test Passed: Executor correctly blocked over-budget distribution.")

if __name__ == "__main__":
    test_executor_budget_check()
