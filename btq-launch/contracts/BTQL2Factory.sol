// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "./BTQToken.sol";

/**
 * @title BTQL2Memecoin
 * @dev Standard high-supply memecoin for the Bitcoin-Quantum L2 ecosystem.
 */
contract BTQL2Memecoin is ERC20 {
    constructor(string memory name, string memory symbol, address creator) ERC20(name, symbol) {
        _mint(creator, 1_000_000_000 * 10**18); // 1 Billion fixed supply
    }
}

/**
 * @title BTQL2Factory
 * @dev Deploys 8 strategic memecoins to bootstrap the L2 economy.
 * Requires a small BTQ burn to ensure value accrual.
 */
contract BTQL2Factory is Ownable {
    BTQToken public immutable btqToken;
    address[] public deployedMemecoins;

    uint256 public constant BURN_AMOUNT = 100 * 10**18; // 100 BTQ to deploy a memecoin

    event MemecoinLaunched(address indexed tokenAddress, string name, string symbol);

    constructor(address _btq) Ownable(msg.sender) {
        btqToken = BTQToken(_btq);
    }

    /**
     * @dev Deploys a new memecoin. Requires BTQ approval.
     */
    function launchMemecoin(string calldata name, string calldata symbol) external returns (address) {
        // 1. Burn requirement (Value accrual for L1 BTQ)
        // Note: BTQToken must implement a burn function or transfer to dead address
        btqToken.transferFrom(msg.sender, address(0x000000000000000000000000000000000000dEaD), BURN_AMOUNT);

        // 2. Deploy
        BTQL2Memecoin newToken = new BTQL2Memecoin(name, symbol, msg.sender);
        deployedMemecoins.push(address(newToken));

        emit MemecoinLaunched(address(newToken), name, symbol);
        return address(newToken);
    }

    /**
     * @dev Automated bootstrap of the first 8 strategic memecoins.
     */
    function bootstrapInitial8() external onlyOwner {
        require(deployedMemecoins.length == 0, "Already bootstrapped");

        string[8] memory names = ["QuantumPepe", "SovereignShiba", "LatticeLlama", "DilithiumDog", "KyberKitty", "DecayDoge", "BondingBear", "WhaleWatcher"];
        string[8] memory symbols = ["QPEPE", "SOVSHIB", "LLAMA", "DDOG", "KITTY", "DECAY", "BEAR", "WATCH"];

        for (uint i = 0; i < 8; i++) {
            BTQL2Memecoin newToken = new BTQL2Memecoin(names[i], symbols[i], msg.sender);
            deployedMemecoins.push(address(newToken));
            emit MemecoinLaunched(address(newToken), names[i], symbols[i]);
        }
    }

    function getDeployedCount() external view returns (uint256) {
        return deployedMemecoins.length;
    }
}
