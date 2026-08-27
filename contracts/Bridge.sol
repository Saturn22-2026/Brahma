// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title BTQBridge
 * @dev Handles locking and unlocking assets for the BTQ L1.
 */
contract BTQBridge {
    address public owner;

    event Deposit(address indexed user, uint256 amount, string btqAddress);
    event Withdrawal(address indexed user, uint256 amount);

    constructor() {
        owner = msg.sender;
    }

    /**
     * @dev User deposits ETH to be bridged to BTQ L1.
     * @param btqAddress The destination address on the BTQ L1.
     */
    function deposit(string calldata btqAddress) external payable {
        require(msg.value > 0, "Amount must be > 0");
        emit Deposit(msg.sender, msg.value, btqAddress);
    }

    /**
     * @dev Relayer calls this to unlock ETH when bridging back.
     */
    function withdraw(address payable user, uint256 amount) external {
        require(msg.sender == owner, "Only relayer/owner");
        require(address(this).balance >= amount, "Insufficient bridge liquidity");
        user.transfer(amount);
        emit Withdrawal(user, amount);
    }

    receive() external payable {}
}
