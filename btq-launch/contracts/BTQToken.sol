// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";

/**
 * @title BTQToken
 * @dev Finalized production version (v4) of the Bitcoin-Quantum token.
 * Features DEX compatibility, Whale Extinguisher, and 2-Year AI lock.
 */
contract BTQToken is ERC20, Ownable {
    address public reserveWallet;
    address public miningContract;

    // Strategic Supply Constants
    uint256 public constant TOTAL_SUPPLY = 100_000_000 * 10**18;
    uint256 public constant FLOAT_TOTAL = 40_000_000 * 10**18;
    uint256 public constant MINING_TOTAL = 15_000_000 * 10**18;

    // Current released float used as the basis for Whale Tax
    uint256 public tradeableFloat;
    uint256 public totalFloatReleased;
    uint256 public immutable deploymentTime;

    // Whale Extinguisher thresholds (in basis points)
    uint256 public constant THRESHOLD_2_5 = 2500;
    uint256 public constant THRESHOLD_3_5 = 3500;
    uint256 public constant THRESHOLD_4_5 = 4500;
    uint256 public constant THRESHOLD_7_5 = 7500;

    mapping(address => bool) public isAMMPair;

    // AI Donation Lock variables
    address public aiDonationWallet;
    uint256 public aiDonationUnlockTime;
    bool public aiLockActive;

    modifier onlyMining() {
        require(msg.sender == miningContract, "Only mining contract can mint");
        _;
    }

    constructor(
        address _wealthWallet,
        address _empowermentWallet,
        address _stabilityWallet,
        address _aiDonationWallet,
        address _airdropWallet,
        address _floatWallet
    ) ERC20("Bitcoin-Quantum", "BTQ") Ownable(msg.sender) {
        deploymentTime = block.timestamp;
        reserveWallet = _stabilityWallet; // Initialize reserve wallet to stability pool

        // 1. Sovereign Reserves (25M BTQ)
        _mint(_wealthWallet, 10_000_000 * 10**18);
        _mint(_empowermentWallet, 10_000_000 * 10**18);
        _mint(_stabilityWallet, 5_000_000 * 10**18);

        // 2. Initial AMM Release (2M out of 40M)
        totalFloatReleased = 2_000_000 * 10**18;
        tradeableFloat = totalFloatReleased;
        _mint(_floatWallet, totalFloatReleased);

        // 3. 10-Year Linear Airdrop Pool (10M)
        _mint(_airdropWallet, 10_000_000 * 10**18);

        // 4. AI-Governed Donations (10M) - Locked for 2 years
        _mint(_aiDonationWallet, 10_000_000 * 10**18);
        aiDonationWallet = _aiDonationWallet;
        aiDonationUnlockTime = block.timestamp + 730 days;
        aiLockActive = true;

        // 5. Mint remaining unreleased float + mining pool to the contract
        // Remainder = (40M - 2M) + 15M = 53M
        _mint(address(this), (FLOAT_TOTAL - totalFloatReleased) + MINING_TOTAL);
    }

    /**
     * @dev Releases 2M BTQ per year from the contract to the float wallet.
     * This increases the tradeableFloat, which is the basis for Whale Tax calculations.
     */
    function syncAMMLiquidity(address floatWallet) external onlyOwner {
        uint256 yearsElapsed = (block.timestamp - deploymentTime) / 365 days;
        uint256 totalEligible = (yearsElapsed + 1) * 2_000_000 * 10**18;
        if (totalEligible > FLOAT_TOTAL) totalEligible = FLOAT_TOTAL;

        if (totalEligible > totalFloatReleased) {
            uint256 amountToRelease = totalEligible - totalFloatReleased;
            _transfer(address(this), floatWallet, amountToRelease);
            totalFloatReleased = totalEligible;
            tradeableFloat = totalFloatReleased;
        }
    }

    function setMiningContract(address _miningContract) external onlyOwner {
        require(miningContract == address(0), "Already set");
        miningContract = _miningContract;
    }

    function setAMMPair(address pair, bool value) external onlyOwner {
        isAMMPair[pair] = value;
    }

    function unlockAIDonations() external {
        require(aiLockActive, "Lock not active");
        require(block.timestamp >= aiDonationUnlockTime, "Lock time not expired");
        aiLockActive = false;
    }

    /**
     * @dev The Whale Extinguisher Protocol
     * Intercepts transfers to AMM pairs (sells) and applies progressive taxation.
     */
    function _update(address from, address to, uint256 amount) internal override {
        // 1. Enforce 2-year AI Donation lock
        if (aiLockActive && from == aiDonationWallet) {
            revert("AI Donations locked for 2 years");
        }

        // 2. Whale Extinguisher (applied on Sells to identified AMM Pairs)
        if (isAMMPair[to] && from != reserveWallet && from != miningContract && from != owner()) {
            uint256 percentOfFloat = (amount * 10000) / tradeableFloat;
            uint256 taxRate = 0;

            if (percentOfFloat > THRESHOLD_2_5 && percentOfFloat <= THRESHOLD_3_5) {
                taxRate = 500; // 5%
            } else if (percentOfFloat > THRESHOLD_3_5 && percentOfFloat <= THRESHOLD_4_5) {
                taxRate = 1500; // 15%
            } else if (percentOfFloat > THRESHOLD_4_5 && percentOfFloat <= THRESHOLD_7_5) {
                taxRate = 2500; // 25%
            } else if (percentOfFloat > THRESHOLD_7_5) {
                taxRate = 3500; // 35%
            }

            if (taxRate > 0) {
                uint256 taxAmount = (amount * taxRate) / 10000;
                uint256 remainder = amount - taxAmount;
                super._update(from, reserveWallet, taxAmount);
                super._update(from, to, remainder);
                emit WhaleTaxApplied(from, taxAmount);
                return;
            }
        }
        super._update(from, to, amount);
    }

    function mintMiningReward(address to, uint256 amount) external onlyMining {
        _transfer(address(this), to, amount);
    }

    event WhaleTaxApplied(address indexed seller, uint256 taxedAmount);
}
