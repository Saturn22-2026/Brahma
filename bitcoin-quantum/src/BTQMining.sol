// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/access/Ownable.sol";
import "./BTQToken.sol";

/**
 * @title BTQMining
 * @dev Manages the 15M mining pool with continuous decay and a 2M/year cap.
 */
contract BTQMining is Ownable {
    BTQToken public btqToken;

    uint256 public constant INITIAL_REWARD = 50 * 10**18;
    uint256 public constant DECAY_FACTOR = 9999;
    uint256 public constant FLOOR_REWARD = 0.01 * 10**18;
    uint256 public constant MAX_SUPPLY = 15_000_000 * 10**18;

    uint256 public constant YEARLY_CAP = 2_000_000 * 10**18;
    uint256 public yearStart;
    uint256 public minedThisYear;

    uint256 public currentReward;
    uint256 public lastMinedBlock;
    uint256 public totalMined;

    constructor(address _btqToken) Ownable(msg.sender) {
        btqToken = BTQToken(_btqToken);
        currentReward = INITIAL_REWARD;
        lastMinedBlock = block.number;
        yearStart = block.timestamp;
    }

    /**
     * @dev Core mining function. Calculates current block reward with decay and mints.
     */
    function mine() external {
        // 1. Reset yearly cap if 365 days have passed
        if (block.timestamp >= yearStart + 365 days) {
            yearStart = block.timestamp;
            minedThisYear = 0;
        }

        // 2. Apply continuous decay for missed blocks
        uint256 blocksPassed = block.number - lastMinedBlock;
        if (blocksPassed > 0) {
            for(uint i = 0; i < blocksPassed; i++) {
                currentReward = (currentReward * DECAY_FACTOR) / 10000;
                if (currentReward < FLOOR_REWARD) {
                    currentReward = FLOOR_REWARD;
                    break;
                }
            }
            lastMinedBlock = block.number;
        }

        // 3. Verification
        require(totalMined + currentReward <= MAX_SUPPLY, "Total mining reserve exhausted");
        require(minedThisYear + currentReward <= YEARLY_CAP, "Yearly mining cap of 2M BTQ reached");

        // 4. Execution
        totalMined += currentReward;
        minedThisYear += currentReward;
        btqToken.mintMiningReward(msg.sender, currentReward);
    }
}
