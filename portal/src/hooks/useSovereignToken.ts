import { useReadContract, useWriteContract, useAccount } from 'wagmi';
import { parseEther, formatEther } from 'viem';

export const _TOKEN_ABI = [
    {
        "inputs": [],
        "name": "currentPrice",
        "outputs": [{"internalType": "uint256", "name": "", "type": "uint256"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "tradeableFloat",
        "outputs": [{"internalType": "uint256", "name": "", "type": "uint256"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "wealthWallet",
        "outputs": [{"internalType": "address", "name": "", "type": "address"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [{"internalType": "address", "name": "account", "type": "address"}],
        "name": "balanceOf",
        "outputs": [{"internalType": "uint256", "name": "", "type": "uint256"}],
        "stateMutability": "view",
        "type": "function"
    },
    {
        "inputs": [],
        "name": "buyTokens",
        "outputs": [],
        "stateMutability": "payable",
        "type": "function"
    },
    {
        "inputs": [{"internalType": "uint256", "name": "amount", "type": "uint256"}],
        "name": "sellTokens",
        "outputs": [],
        "stateMutability": "nonpayable",
        "type": "function"
    }
] as const;

export const CONTRACT_ADDRESS = '0x5FbDB2315678afecb367f032d93F642f64180aa3'; // Local Anvil address

export function useSovereignToken() {
    const { address } = useAccount();

    const price = useReadContract({
        address: CONTRACT_ADDRESS,
        abi: _TOKEN_ABI,
        functionName: 'currentPrice',
    });

    const float = useReadContract({
        address: CONTRACT_ADDRESS,
        abi: _TOKEN_ABI,
        functionName: 'tradeableFloat',
    });

    const userBalance = useReadContract({
        address: CONTRACT_ADDRESS,
        abi: _TOKEN_ABI,
        functionName: 'balanceOf',
        args: address ? [address] : undefined,
    });

    const { writeContract: buyTokens } = useWriteContract();
    const { writeContract: sellTokens } = useWriteContract();

    return {
        price: price.data ? formatEther(price.data) : '0',
        float: float.data ? formatEther(float.data) : '0',
        userBalance: userBalance.data ? formatEther(userBalance.data) : '0',
        buy: (ethAmount: string) => buyTokens({
            address: CONTRACT_ADDRESS,
            abi: _TOKEN_ABI,
            functionName: 'buyTokens',
            value: parseEther(ethAmount),
        }),
        sell: (sqtAmount: string) => sellTokens({
            address: CONTRACT_ADDRESS,
            abi: _TOKEN_ABI,
            functionName: 'sellTokens',
            args: [parseEther(sqtAmount)],
        })
    };
}
