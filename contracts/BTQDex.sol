// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "./SovereignEconomy.sol";

/**
 * @title BTQDex
 * @dev A decentralized exchange natively integrated with the BTQ Whale Extinguisher.
 */
contract BTQDex {
    SovereignEconomy public token;
    uint256 public reserveETH;
    uint256 public reserveTokens;

    event LiquidityAdded(uint256 eth, uint256 tokens);
    event Swapped(address indexed user, uint256 ethIn, uint256 tokensOut);

    constructor(address _token) {
        token = SovereignEconomy(payable(_token));
    }

    /**
     * @dev Add liquidity to the pool.
     */
    function addLiquidity(uint256 tokenAmount) external payable {
        require(msg.value > 0 && tokenAmount > 0, "Zero liquidity");

        token.transferFrom(msg.sender, address(this), tokenAmount);
        reserveETH += msg.value;
        reserveTokens += tokenAmount;

        emit LiquidityAdded(msg.value, tokenAmount);
    }

    /**
     * @dev Swap ETH for Tokens.
     * Integrates with SovereignEconomy's whale protection automatically.
     */
    function swapEthForTokens() external payable {
        require(msg.value > 0, "Must send ETH");

        // Constant Product Formula: x * y = k
        uint256 tokensOut = (reserveTokens * msg.value) / (reserveETH + msg.value);
        require(tokensOut < reserveTokens, "Insufficient liquidity");

        reserveETH += msg.value;
        reserveTokens -= tokensOut;

        // The 'transfer' call to the user will trigger the dump checks if it were a sell,
        // but here it's a buy from the AMM's perspective.
        token.transfer(msg.sender, tokensOut);

        emit Swapped(msg.sender, msg.value, tokensOut);
    }

    /**
     * @dev Note: The 'sellTokens' function in SovereignEconomy.sol
     * ALREADY implements the Whale Extinguisher tax.
     * When a user sells to this DEX, the tax is applied at the token contract level.
     */
}
