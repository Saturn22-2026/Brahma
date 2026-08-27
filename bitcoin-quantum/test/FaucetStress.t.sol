// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/BTQToken.sol";
import "../src/BTQFaucet.sol";

contract FaucetStressTest is Test {
    BTQToken public token;
    BTQFaucet public faucet;

    address wealth = address(0x1);
    address empower = address(0x2);
    address stability = address(0x3);
    address aiDonation = address(0xD);
    address airdropSource = address(0xA);
    address floatSource = address(0xF);
    address user = address(0x6);

    function setUp() public {
        token = new BTQToken(wealth, empower, stability, aiDonation, airdropSource, floatSource);
        faucet = new BTQFaucet(address(token));

        // Fund the faucet with 10,000 BTQ
        vm.prank(floatSource);
        token.approve(address(faucet), 10_000 * 1e18);
        vm.prank(floatSource);
        faucet.fundFaucet(10_000 * 1e18);
    }

    function test_Faucet_InitialClaim() public {
        uint256 initialBalance = token.balanceOf(user);
        vm.prank(user);
        faucet.requestTokens();

        assertEq(token.balanceOf(user), initialBalance + (100 * 1e18));
        assertEq(faucet.lastClaimTime(user), block.timestamp);
    }

    function test_Faucet_CooldownEnforcement() public {
        vm.prank(user);
        faucet.requestTokens();

        // Immediate second claim fails
        vm.prank(user);
        vm.expectRevert("You can only claim once every 24 hours.");
        faucet.requestTokens();

        // 23 hours later fails
        vm.warp(block.timestamp + 23 hours);
        vm.prank(user);
        vm.expectRevert("You can only claim once every 24 hours.");
        faucet.requestTokens();

        // 24 hours later succeeds
        vm.warp(block.timestamp + 1 hours + 1);
        vm.prank(user);
        faucet.requestTokens();
        assertEq(token.balanceOf(user), 200 * 1e18);
    }

    function test_Faucet_EmptyPool() public {
        address user2 = address(0x7);

        // Drain faucet by setting claim amount to 11,000
        faucet.setClaimAmount(11_000 * 1e18);

        vm.prank(user2);
        vm.expectRevert("Faucet is empty. Please contact admin.");
        faucet.requestTokens();
    }

    function test_Faucet_Withdrawal() public {
        uint256 ownerInitial = token.balanceOf(address(this));
        faucet.withdrawTokens(1000 * 1e18);
        assertEq(token.balanceOf(address(this)), ownerInitial + (1000 * 1e18));
    }
}
