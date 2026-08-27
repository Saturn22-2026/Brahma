// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/BTQToken.sol";
import "../src/BTQAirdrop.sol";
import "../src/BTQMining.sol";

contract EconomicConstraintsTest is Test {
    BTQToken public token;
    BTQAirdrop public airdrop;
    BTQMining public mining;

    address wealth = address(0x1);
    address empower = address(0x2);
    address stability = address(0x3);
    address aiDonation = address(0xD1);
    address airdropSource = address(0xA1);
    address floatSource = address(0xF1);
    address aiAgent = address(0x4);
    address user = address(0x6);

    function setUp() public {
        // 1. Deploy Core Token
        token = new BTQToken(wealth, empower, stability, aiDonation, airdropSource, floatSource);

        // 2. Deploy Modules
        airdrop = new BTQAirdrop(address(token), aiAgent);
        mining = new BTQMining(address(token));

        // 3. Link Modules
        token.setMiningContract(address(mining));
        token.setAIAgent(aiAgent);

        // Seed Airdrop Pool
        vm.prank(airdropSource);
        token.transfer(address(airdrop), 10_000_000 * 1e18);

        vm.deal(user, 1000 ether);
    }

    /**
     * @dev Test 2-Year AI Lock: Try to transfer BTQ out of the aiDonationWallet address.
     * The transaction will revert with "AI Donations locked for 2 years".
     * Fast-forward time on your testnet (or wait 730 days) and call unlockAIDonations() to free the funds.
     */
    function test_AI_Donation_Lock() public {
        // Attempt transfer directly from the donation wallet
        vm.prank(aiDonation);
        vm.expectRevert("AI Donations locked for 2 years");
        token.transfer(user, 100 * 1e18);

        // Attempt autonomous donation through the AI Agent
        vm.prank(aiAgent);
        vm.expectRevert("AI Donations locked for 2 years");
        token.executeAutonomousDonation(user, 100 * 1e18);

        // Warp 2 years
        vm.warp(block.timestamp + 731 days);

        // Unlock
        token.unlockAIDonations();

        // Now it should work
        vm.prank(aiDonation);
        token.transfer(user, 100 * 1e18);
        assertEq(token.balanceOf(user), 100 * 1e18);
    }

    /**
     * @dev Test Linear Airdrop: Call claimDailyAirdrop() on the BTQAirdrop contract.
     * You will see exactly 2,739.72 BTQ transferred to the AI Agent wallet.
     * If you try to call it again immediately, it reverts.
     */
    function test_Linear_Airdrop_Constraint() public {
        uint256 expectedDrip = 2739726027397260273972; // ~2739.72 BTQ

        // First claim (allowed since lastClaimTime initialized to -1 day)
        airdrop.claimDailyAirdrop();
        assertEq(token.balanceOf(aiAgent), expectedDrip);

        // Immediate second claim fails
        vm.expectRevert("Already claimed today");
        airdrop.claimDailyAirdrop();

        // Warp 1 day and claim again
        vm.warp(block.timestamp + 1 days + 1);
        airdrop.claimDailyAirdrop();
        assertEq(token.balanceOf(aiAgent), expectedDrip * 2);
    }

    /**
     * @dev Test Yearly Mining Cap: Spam the mine() function on the BTQMining contract.
     * After 40,000 calls (which would mint 2,000,000 BTQ), the contract will revert with
     * "Yearly mining cap of 2M BTQ reached".
     * It will only resume working when block.timestamp advances past 365 days from deployment.
     */
    function test_Yearly_Mining_Cap_Constraint() public {
        uint256 rewardPerBlock = 50 * 1e18;
        uint256 cap = 2_000_000 * 1e18;
        uint256 iterations = cap / rewardPerBlock; // 40,000

        // Perform 40,000 mining operations (simulated)
        // We prank the mining contract because the 'mine' function calls 'mintMiningReward' on token
        for(uint i = 0; i < iterations; i++) {
            mining.mine();
            vm.roll(block.number + 1); // Keep block number moving for decay logic if needed
        }

        // The 40,001st call should revert
        vm.expectRevert("Yearly mining cap of 2M BTQ reached");
        mining.mine();

        // Warp 1 year
        vm.warp(block.timestamp + 366 days);

        // Now it should resume
        mining.mine();
        assertGt(token.totalMiningDistributed(), cap);
    }
}
