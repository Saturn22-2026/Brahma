// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/SovereignToken.sol";

contract SovereignFuzzTest is Test {
    SovereignToken public token;
    address wealth = address(0x1);
    address empower = address(0x2);
    address reserve = address(0x3);
    address user = address(0x4);

    function setUp() public {
        token = new SovereignToken(wealth, empower, reserve, 1_000_000);
        vm.deal(user, 1000 ether);
    }

    /**
     * @dev Fuzz test for buying tokens.
     * Ensures price always increases and float decreases correctly.
     */
    function testFuzz_BuyTokens(uint256 amount) public {
        vm.assume(amount > 0 && amount < 100 ether);

        uint256 initialPrice = token.currentPrice();
        uint256 initialFloat = token.tradeableFloat();

        vm.prank(user);
        token.buyTokens{value: amount}();

        assertGt(token.currentPrice(), initialPrice, "Price must increase after buy");
        assertLt(token.tradeableFloat(), initialFloat, "Float must decrease after buy");
    }

    /**
     * @dev Fuzz test for the Whale Tax logic.
     * Ensures large sells are taxed more than small ones.
     */
    function testFuzz_WhaleTax(uint256 sellAmount) public {
        // First buy some tokens so the user has a balance
        vm.prank(user);
        token.buyTokens{value: 10 ether}();
        uint256 userBalance = token.balanceOf(user);

        vm.assume(sellAmount > 0 && sellAmount <= userBalance);

        uint256 initialReserveBalance = token.balanceOf(reserve);
        uint256 ratio = (sellAmount * 100000) / token.initialFloat();

        vm.prank(user);
        token.sellTokens(sellAmount);

        if (ratio > token.MAX_SELL_RATIO()) {
            assertGt(token.balanceOf(reserve), initialReserveBalance, "Reserve should receive tax from whale sell");
        } else {
            assertEq(token.balanceOf(reserve), initialReserveBalance, "No tax for small sells");
        }
    }

    /**
     * @dev Invariant test: Collateral pool + ETH returned must be consistent.
     */
    function test_Invariant_CollateralBacking() public {
        vm.prank(user);
        token.buyTokens{value: 10 ether}();

        uint256 pool = token.collateralPool();
        assertEq(pool, 10 ether, "Collateral pool mismatch");
    }
}
