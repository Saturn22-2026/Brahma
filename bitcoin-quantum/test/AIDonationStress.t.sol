// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/BTQToken.sol";

contract AIDonationStressTest is Test {
    BTQToken public token;
    address wealth = address(0x1111);
    address empower = address(0x2222);
    address stability = address(0x3333);
    address aiDonation = address(0xD001);
    address airdropSource = address(0xA001);
    address floatSource = address(0xF001);
    address aiAgent = address(0x4004);
    address recipient = address(0x999);

    function setUp() public {
        token = new BTQToken(wealth, empower, stability, aiDonation, airdropSource, floatSource);
        token.setAIAgent(aiAgent);
    }

    /**
     * @dev Verify the 2-Year Lock is unbreachable via the AI Agent.
     */
    function test_AI_Donation_Lock_Enforcement() public {
        vm.prank(aiAgent);
        // Attempt donation on Day 1
        vm.expectRevert("AI Donations locked for 2 years");
        token.executeAutonomousDonation(recipient, 1000 * 1e18);

        // Attempt donation on Day 365 (1 year)
        vm.warp(block.timestamp + 365 days);
        vm.prank(aiAgent);
        vm.expectRevert("AI Donations locked for 2 years");
        token.executeAutonomousDonation(recipient, 1000 * 1e18);
    }

    /**
     * @dev Verify AI can successfully distribute funds after the 730-day threshold.
     */
    function test_AI_Donation_Unlock_And_Execute() public {
        // Warp to Year 3
        vm.warp(block.timestamp + 731 days);

        // 1. Unlock the wallet logic
        token.unlockAIDonations();

        // 2. Execute donation via AI Agent
        uint256 amount = 50_000 * 1e18;
        uint256 initialReserve = token.donationReserve();

        vm.prank(aiAgent);
        token.executeAutonomousDonation(recipient, amount);

        assertEq(token.balanceOf(recipient), amount);
        assertEq(token.donationReserve(), initialReserve - amount);
        println("[AI Governance] Success: Donation executed post-lock.");
    }

    /**
     * @dev Verify unauthorized addresses cannot trigger donations even after unlock.
     */
    function test_AI_Donation_Unauthorized_Rejection() public {
        vm.warp(block.timestamp + 731 days);
        token.unlockAIDonations();

        vm.prank(address(0xBEEF));
        vm.expectRevert("Unauthorized: AI Agent only");
        token.executeAutonomousDonation(recipient, 1000 * 1e18);
    }
}
