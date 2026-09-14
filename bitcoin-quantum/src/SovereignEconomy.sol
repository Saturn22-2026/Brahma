// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/access/Ownable.sol";
import "@openzeppelin/contracts/utils/ReentrancyGuard.sol";

/**
 * @title SovereignEconomy v2.0
 * @dev World Reserve Evolution: BTQ-backed Stablecoin (QUSD) & Predatory Tax Defense.
 */
contract SovereignEconomy is ERC20, Ownable, ReentrancyGuard {
    // --- STABLECOIN STATE (QUSD) ---
    uint256 public constant COLLATERAL_RATIO = 150; // 150% over-collateralization
    uint256 public totalBTQCollateral;
    uint256 public totalQUSDSupply;

    // --- PREDATORY DEFENSE STATE ---
    uint256 public whaleTaxBasisPoints = 2500; // 25% Base
    bool public predatoryDefenseActive;
    uint256 public constant PREDATORY_TAX = 9900; // 99%

    // --- REFERRAL ENGINE ---
    mapping(address => address) public referrers;
    uint256[3] public referralPrizes = [500, 300, 200]; // 5%, 3%, 2%

    event QUSDMinted(address indexed user, uint256 btqAmount, uint256 qusdAmount);
    event PredatoryDefenseTriggered(bool active);
    event ReferralRegistered(address indexed user, address indexed referrer);

    constructor() ERC20("Sovereign Quantum Dollar", "QUSD") Ownable(msg.sender) {}

    /**
     * @dev Mint QUSD by locking BTQ as collateral.
     * Calculated using a constant price model for this phase.
     */
    function mintQUSD(uint256 btqAmount, address btqToken) external nonReentrant {
        require(btqAmount > 0, "Amount must be > 0");

        // Transfer BTQ from user to this contract
        IERC20(btqToken).transferFrom(msg.sender, address(this), btqAmount);

        // Calculate QUSD value (Simulated BTQ price: 1 BTQ = 10 USD)
        // Ratio 150% -> 1000 BTQ ($10,000) can mint 6666 QUSD
        uint256 qusdToMint = (btqAmount * 10 * 100) / COLLATERAL_RATIO;

        totalBTQCollateral += btqAmount;
        totalQUSDSupply += qusdToMint;

        _mint(msg.sender, qusdToMint);
        emit QUSDMinted(msg.sender, btqAmount, qusdToMint);
    }

    /**
     * @dev AI Sentinel Trigger for Predatory Defense.
     * Can only be called by the Governance AI Agent.
     */
    function setPredatoryDefense(bool status) external onlyOwner {
        predatoryDefenseActive = status;
        emit PredatoryDefenseTriggered(status);
    }

    /**
     * @dev Logic to be integrated into BTQToken.sol _update override.
     */
    function calculateTransferTax(uint256 amount, uint256 totalSupply) public view returns (uint256) {
        if (predatoryDefenseActive) return (amount * PREDATORY_TAX) / 10000;

        // Standard Whale Tax: if transfer > 2.5% of supply
        if (amount > (totalSupply * 250) / 10000) {
            return (amount * whaleTaxBasisPoints) / 10000;
        }
        return 0;
    }

    /**
     * @dev Permissionless Referral Registry.
     */
    function registerReferral(address referrer) external {
        require(referrers[msg.sender] == address(0), "Referrer already set");
        require(referrer != msg.sender, "Cannot self-refer");
        referrers[msg.sender] = referrer;
        emit ReferralRegistered(msg.sender, referrer);
    }
}

interface IERC20 {
    function transferFrom(address from, address to, uint256 amount) external returns (bool);
    function transfer(address to, uint256 amount) external returns (bool);
}
