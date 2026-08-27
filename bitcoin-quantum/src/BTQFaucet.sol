// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "@openzeppelin/contracts/access/Ownable.sol";
import "./BTQToken.sol";

/**
 * @title BTQFaucet
 * @dev Standalone community faucet for bootstrapping new users with recurring 100 BTQ claims.
 */
contract BTQFaucet is Ownable {
    BTQToken public btqToken;

    // Amount of tokens to drip per claim (100 BTQ)
    uint256 public claimAmount = 100 * 10**18;
    // Cooldown period (24 hours)
    uint256 public constant COOLDOWN = 1 days;

    mapping(address => uint256) public lastClaimTime;

    event TokensRequested(address indexed user, uint256 amount);

    constructor(address _btqToken) Ownable(msg.sender) {
        btqToken = BTQToken(_btqToken);
    }

    /**
     * @dev Allows users to request tokens every 24 hours.
     */
    function requestTokens() external {
        require(
            block.timestamp >= lastClaimTime[msg.sender] + COOLDOWN,
            "You can only claim once every 24 hours."
        );

        uint256 balance = btqToken.balanceOf(address(this));
        require(balance >= claimAmount, "Faucet is empty. Please contact admin.");

        lastClaimTime[msg.sender] = block.timestamp;
        btqToken.transfer(msg.sender, claimAmount);

        emit TokensRequested(msg.sender, claimAmount);
    }

    /**
     * @dev Allows the owner to update the claim amount.
     */
    function setClaimAmount(uint256 _newAmount) external onlyOwner {
        claimAmount = _newAmount;
    }

    /**
     * @dev Owner function to fund the faucet from their wallet.
     * Note: Owner must approve the faucet to spend tokens first.
     */
    function fundFaucet(uint256 _amount) external onlyOwner {
        btqToken.transferFrom(msg.sender, address(this), _amount);
    }

    /**
     * @dev Owner function to withdraw tokens in case of emergency.
     */
    function withdrawTokens(uint256 _amount) external onlyOwner {
        btqToken.transfer(msg.sender, _amount);
    }
}
