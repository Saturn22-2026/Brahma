// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/SovereignToken.sol";

contract SovereignV2Test is Test {
    SovereignToken public token;
    address wealth = address(0x1);
    address empower = address(0x2);
    address reserve = address(0x3);
    address aiAgent = address(0x4);
    address miner = address(0x5);
    address user = address(0x6);

    function setUp() public {
        token = new SovereignToken(wealth, empower, reserve, 100_000_000, address(0x123));
        token.setAIAgent(aiAgent);
        vm.deal(user, 1000 ether);
    }

    function test_InitialSupplyV2() public {
        assertEq(token.totalSupply(), 100_000_000 * 1e18);
        assertEq(token.balanceOf(wealth), 11_000_000 * 1e18);
        assertEq(token.balanceOf(empower), 11_000_000 * 1e18);
        assertEq(token.balanceOf(reserve), 11_000_000 * 1e18);

        // 62M Float + 5M Mining Reserve
        assertEq(token.balanceOf(address(token)), 67_000_000 * 1e18);
        assertEq(token.tradeableFloat(), 62_000_000 * 1e18);
        assertEq(token.miningReserve(), 5_000_000 * 1e18);
    }

    function test_AirdropBudgetTimeline() public {
        // Month 1
        assertEq(token.getCurrentAirdropBudget(), 35_000 * 1e18);

        // Advance to Month 2
        vm.warp(block.timestamp + 31 days);
        assertEq(token.getCurrentAirdropBudget(), 25_000 * 1e18);

        // Advance to Year 2
        vm.warp(block.timestamp + 365 days);
        assertEq(token.getCurrentAirdropBudget(), 15_000 * 1e18);

        // Advance to Year 13 (End of 144 months)
        vm.warp(block.timestamp + 12 * 365 days);
        assertEq(token.getCurrentAirdropBudget(), 0);
    }

    function test_ExecuteAutonomousAirdrop() public {
        address[] memory recipients = new address[](2);
        recipients[0] = address(0xA);
        recipients[1] = address(0xB);

        uint256[] memory amounts = new uint256[](2);
        amounts[0] = 10_000 * 1e18;
        amounts[1] = 20_000 * 1e18;

        uint256 initialEmpowerBalance = token.balanceOf(empower);

        vm.prank(aiAgent);
        token.executeAutonomousAirdrop(recipients, amounts);

        assertEq(token.balanceOf(address(0xA)), 10_000 * 1e18);
        assertEq(token.balanceOf(address(0xB)), 20_000 * 1e18);
        assertEq(token.balanceOf(empower), initialEmpowerBalance - (30_000 * 1e18));
    }

    function test_MintMiningReward() public {
        uint256 initialMinerBalance = token.balanceOf(miner);
        uint256 initialMiningReserve = token.miningReserve();
        uint256 reward = 50 * 1e18;

        token.mintMiningReward(miner, reward);

        assertEq(token.balanceOf(miner), initialMinerBalance + reward);
        assertEq(token.miningReserve(), initialMiningReserve - reward);
    }

    function test_AirdropUnauthorized() public {
        address[] memory recipients = new address[](1);
        uint256[] memory amounts = new uint256[](1);

        vm.prank(user);
        vm.expectRevert(SovereignToken.Unauthorized.selector);
        token.executeAutonomousAirdrop(recipients, amounts);
    }

    function test_AirdropOverBudget() public {
        address[] memory recipients = new address[](1);
        recipients[0] = address(0xA);
        uint256[] memory amounts = new uint256[](1);
        amounts[0] = 40_000 * 1e18; // Over 35,000 budget

        vm.prank(aiAgent);
        vm.expectRevert("Exceeds monthly budget");
        token.executeAutonomousAirdrop(recipients, amounts);
    }
}
