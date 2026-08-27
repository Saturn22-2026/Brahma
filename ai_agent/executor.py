import json
import os
from web3 import Web3
from typing import Dict

class OnChainAirdropAgent:
    """
    Interfaces with the BitcoinQuantum contract to execute airdrops.
    """
    def __init__(self, rpc_url: str, contract_address: str, private_key: str):
        self.w3 = Web3(Web3.HTTPProvider(rpc_url))
        self.account = self.w3.eth.account.from_key(private_key)
        self.contract_address = contract_address

        # In production, load real ABI from build artifacts
        self.abi = self._get_funtion_abi()
        self.contract = self.w3.eth.contract(address=contract_address, abi=self.abi)

    def _get_funtion_abi(self):
        # Only the necessary fragment for executeAutonomousAirdrop
        return [
            {
                "inputs": [],
                "name": "getCurrentAirdropBudget",
                "outputs": [{"internalType": "uint256", "name": "", "type": "uint256"}],
                "stateMutability": "view",
                "type": "function"
            },
            {
                "inputs": [
                    {"internalType": "address[]", "name": "recipients", "type": "address[]"},
                    {"internalType": "uint256[]", "name": "amounts", "type": "uint256[]"}
                ],
                "name": "executeAutonomousAirdrop",
                "outputs": [],
                "stateMutability": "nonpayable",
                "type": "function"
            }
        ]

    def execute_airdrop(self, allocations: Dict[str, float]):
        """
        Signs and broadcasts the airdrop transaction.
        """
        if not allocations:
            print("[Agent] No allocations to distribute.")
            return

        recipients = list(allocations.keys())
        # Convert float SQT to 18-decimal integer
        amounts = [int(amt * 10**18) for amt in allocations.values()]

        total_dist = sum(amounts)
        print(f"[Agent] Attempting to distribute {total_dist / 1e18:,.2f} SQT to {len(recipients)} recipients...")

        # 1. Verify Budget
        try:
            budget = self.contract.functions.getCurrentAirdropBudget().call()
            if total_dist > budget:
                print(f"[Agent] Error: Distribution ({total_dist}) exceeds contract budget ({budget})")
                return
        except Exception as e:
            print(f"[Agent] Warning: Could not verify budget on-chain: {e}")

        # 2. Build Transaction
        nonce = self.w3.eth.get_transaction_count(self.account.address)
        tx = self.contract.functions.executeAutonomousAirdrop(
            recipients, amounts
        ).build_transaction({
            'from': self.account.address,
            'nonce': nonce,
            'gas': 200000 + (len(recipients) * 50000), # Rough estimate
            'gasPrice': self.w3.eth.gas_price
        })

        # 3. Sign and Send
        signed_tx = self.w3.eth.account.sign_transaction(tx, private_key=self.account.key)
        # tx_hash = self.w3.eth.send_raw_transaction(signed_tx.rawTransaction)
        # print(f"[Agent] ✅ Distribution Broadcasted! Hash: {tx_hash.hex()}")
        print("[Agent] [DRY RUN] Transaction signed. Ready for mainnet broadcasting.")
        return signed_tx.rawTransaction.hex()
