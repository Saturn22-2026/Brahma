// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/token/ERC20/IERC20.sol";

/**
 * @title BTQL2Airdrop
 * @dev Generic 10-year daily drip for Layer 2 memecoins.
 */
contract BTQL2Airdrop is Ownable {
    IERC20 public immutable token;
    address public immutable aiAgentWallet;

    uint256 public constant TOTAL_AIRDROP = 117_647_059 * 10**18; // 11.76% of 1B
    uint256 public constant DURATION_DAYS = 3650;
    uint256 public constant DAILY_DRIP = TOTAL_AIRDROP / DURATION_DAYS;

    uint256 public lastClaimTime;
    uint256 public totalClaimed;

    event DailyAirdropClaimed(uint256 amount);

    constructor(address _token, address _aiAgent, address _owner) Ownable(_owner) {
        token = IERC20(_token);
        aiAgentWallet = _aiAgent;
        lastClaimTime = 0;
    }

    function claimDailyAirdrop() external {
        require(lastClaimTime == 0 || block.timestamp >= lastClaimTime + 1 days, "Already claimed today");
        require(totalClaimed < TOTAL_AIRDROP, "Airdrop complete");

        lastClaimTime = block.timestamp;
        uint256 amountToRelease = DAILY_DRIP;
        if (totalClaimed + amountToRelease > TOTAL_AIRDROP) {
            amountToRelease = TOTAL_AIRDROP - totalClaimed;
        }

        totalClaimed += amountToRelease;
        token.transfer(aiAgentWallet, amountToRelease);
        emit DailyAirdropClaimed(amountToRelease);
    }
}
