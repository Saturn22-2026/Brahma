// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/access/Ownable.sol";
import "./BTQToken.sol";

/**
 * @title BTQAirdrop
 * @dev Manages the 10-year daily drip of BTQ tokens to the AI Agent wallet.
 */
contract BTQAirdrop is Ownable {
    BTQToken public btqToken;
    address public aiAgentWallet;

    uint256 public constant TOTAL_AIRDROP = 10_000_000 * 10**18;
    uint256 public constant DURATION_DAYS = 3650; // 10 Years
    uint256 public constant DAILY_DRIP = TOTAL_AIRDROP / DURATION_DAYS; // ~2,739 BTQ

    uint256 public lastClaimTime;
    uint256 public totalClaimed;

    event DailyAirdropClaimed(uint256 amount);

    constructor(address _btqToken, address _aiAgentWallet) Ownable(msg.sender) {
        btqToken = BTQToken(_btqToken);
        aiAgentWallet = _aiAgentWallet;
        lastClaimTime = block.timestamp - 1 days; // Allow immediate first claim
    }

    /**
     * @dev Releases the daily allocation to the AI Agent. Can be called by anyone every 24h.
     */
    function claimDailyAirdrop() external {
        require(block.timestamp >= lastClaimTime + 1 days, "Already claimed today");
        require(totalClaimed < TOTAL_AIRDROP, "Airdrop complete");

        lastClaimTime = block.timestamp;

        uint256 amountToRelease = DAILY_DRIP;

        // Handle rounding dust on the final claim
        if (totalClaimed + amountToRelease > TOTAL_AIRDROP) {
            amountToRelease = TOTAL_AIRDROP - totalClaimed;
        }

        totalClaimed += amountToRelease;
        btqToken.transfer(aiAgentWallet, amountToRelease);
        emit DailyAirdropClaimed(amountToRelease);
    }
}
