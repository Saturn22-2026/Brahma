// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "./BTQToken.sol";
import "./BTQL2Factory.sol";

/**
 * @title LaunchFactory
 * @notice Deploys BTQToken and coordinates the Sovereign L2 deployment sequence.
 * @dev Phase 1 of the Quick Launch Order.
 */
contract LaunchFactory {

    // State variables
    address public owner;
    address public tokenAddress;
    address public factoryAddress;
    bool public isDeployed;
    uint256 public deployTimestamp;

    // Events
    event TokenDeployed(address indexed token, uint256 timestamp);
    event FactoryConfigured(address indexed factory, uint256 timestamp);
    event DeploymentComplete(uint256 phase);

    // Modifiers
    modifier onlyOwner() {
        require(msg.sender == owner, "Not authorized");
        _;
    }

    constructor() {
        owner = msg.sender;
        isDeployed = false;
    }

    /**
     * @notice Deploy the Sovereign BTQ Token.
     * Integrates the 10-year drip, AI Council locks, and Sovereign allocations.
     */
    function deploySovereignToken() external onlyOwner returns (address) {
        require(tokenAddress == address(0), "Token already deployed");

        // Deploy the high-fidelity BTQToken v5
        BTQToken newToken = new BTQToken();

        tokenAddress = address(newToken);
        emit TokenDeployed(tokenAddress, block.timestamp);

        return tokenAddress;
    }

    /**
     * @notice Deploy and Configure the L2 Factory.
     * Links the token to the Memecoin expansion layer.
     */
    function deployAndConfigureFactory() external onlyOwner {
        require(tokenAddress != address(0), "Token not deployed");
        require(factoryAddress == address(0), "Factory already deployed");

        // 1. Deploy the L2 Factory
        BTQL2Factory newFactory = new BTQL2Factory(payable(tokenAddress), owner);
        factoryAddress = address(newFactory);

        // 2. Grant initial roles (if applicable)
        BTQToken(payable(tokenAddress)).transferOwnership(owner);

        isDeployed = true;
        deployTimestamp = block.timestamp;

        emit FactoryConfigured(factoryAddress, block.timestamp);
        emit DeploymentComplete(1); // Phase 1 (Core Ecosystem) complete
    }

    /**
     * @notice Emergency functions
     */
    function transferOwnership(address newOwner) external onlyOwner {
        require(newOwner != address(0), "Invalid address");
        owner = newOwner;
    }
}
