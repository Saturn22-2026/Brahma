// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/SovereignToken.sol";

/**
 * @title SovereignFormalTest
 * @dev Symbolic execution tests for Halmos.
 * These tests prove properties that must hold for ALL possible inputs.
 */
contract SovereignFormalTest is Test {
    SovereignToken public token;
    address wealth = address(0x1);
    address empower = address(0x2);
    address reserve = address(0x3);

    function setUp() public {
        // Initializing with a mock price feed address
        token = new SovereignToken(wealth, empower, reserve, 1_000_000, address(0x123));
    }

    /**
     * @dev Property: Solvency Invariant.
     * The collateral pool must always be sufficient to back the tradeable float
     * relative to its current market price.
     */
    function check_solvency_invariant(uint256 buyAmount) public {
        vm.assume(buyAmount > 0 && buyAmount < 1000 ether);

        token.buyTokens{value: buyAmount}();

        uint256 pool = token.collateralPool();
        uint256 price = token.currentPrice();
        uint256 soldSupply = token.initialFloat() - token.tradeableFloat();

        // In theory, Pool >= SoldSupply * Price
        // We use 1e18 scale for price
        assert(pool >= (soldSupply * price) / 1e18);
    }

    /**
     * @dev Property: Whale Tax Escapability.
     * Proves that any sell larger than the threshold ALWAYS results in a tax.
     */
    function check_tax_is_inescapable(uint256 sellAmount) public {
        // Setup user with balance
        vm.deal(address(this), 100 ether);
        token.buyTokens{value: 10 ether}();

        uint256 balance = token.balanceOf(address(this));
        vm.assume(sellAmount > 0 && sellAmount <= balance);

        uint256 ratio = (sellAmount * 100000) / token.initialFloat();
        uint256 initialReserve = token.balanceOf(reserve);

        token.sellTokens(sellAmount);

        if (ratio > token.MAX_SELL_RATIO()) {
            assert(token.balanceOf(reserve) > initialReserve);
        }
    }

    /**
     * @dev Property: Supply Conservation.
     * Total supply must equal the sum of all balances.
     */
    function check_supply_conservation() public view {
        uint256 sum = token.balanceOf(address(token)) +
                     token.balanceOf(wealth) +
                     token.balanceOf(empower) +
                     token.balanceOf(reserve);

        // Note: address(this) might have tokens if it bought some
        sum += token.balanceOf(address(this));

        assert(sum == token.totalSupply());
    }

    // Allow receiving ETH for sellTokens tests
    receive() external payable {}
}
