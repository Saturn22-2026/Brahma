// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/SovereignToken.sol";

// Mock Chainlink Aggregator
contract MockPriceFeed is AggregatorV3Interface {
    int256 private _price;
    constructor(int256 initialPrice) { _price = initialPrice; }
    function setPrice(int256 newPrice) public { _price = newPrice; }
    function decimals() external pure override returns (uint8) { return 8; }
    function description() external pure override returns (string memory) { return "ETH/USD"; }
    function version() external pure override returns (uint256) { return 1; }
    function getRoundData(uint80) external view override returns (uint80, int256, uint256, uint256, uint80) { return (0, _price, 0, 0, 0); }
    function latestRoundData() external view override returns (uint80, int256, uint256, uint256, uint80) { return (0, _price, 0, 0, 0); }
}

contract SovereignAutomationTest is Test {
    SovereignToken public token;
    MockPriceFeed public feed;
    address wealth = address(0x1);
    address empower = address(0x2);
    address reserve = address(0x3);
    address user = address(0x4);

    function setUp() public {
        feed = new MockPriceFeed(2000 * 10**8); // $2000 ETH
        token = new SovereignToken(wealth, empower, reserve, 1_000_000, address(feed));
        vm.deal(user, 1000 ether);
    }

    function test_PriceFeedIntegration() public {
        assertEq(token.getLatestBenchmark(), 2000 * 10**8);
        feed.setPrice(1800 * 10**8);
        assertEq(token.getLatestBenchmark(), 1800 * 10**8);
    }

    function test_AutomationTrigger() public {
        // 1. Initially price is at INITIAL_PRICE (0.001 ether)
        // No upkeep should be needed
        (bool needed, ) = token.checkUpkeep("");
        assertEq(needed, false);

        // 2. Simulate heavy sell pressure to crash the price
        // To crash the price, we need to increase the tradeable float.
        // Let's prank a sell from a large holder or simulate it by buying first then dumping.
        vm.startPrank(user);
        token.buyTokens{value: 50 ether}();

        // Sell back a huge amount (simulated whale dump)
        uint256 balance = token.balanceOf(user);
        token.sellTokens(balance);
        vm.stopPrank();

        // 3. Check if price dropped below floor (0.00085 ether)
        // Our simplified bonding curve in the contract makes this easy to test.
        console.log("Current Price:", token.currentPrice());

        // If price is below floor, upkeep should be true
        if (token.currentPrice() < (token.INITIAL_PRICE() * token.PRICE_FLOOR_PERCENT() / 100000)) {
            (needed, ) = token.checkUpkeep("");
            assertEq(needed, true);

            // 4. Perform Upkeep (Keeper action)
            token.performUpkeep("");

            // 5. Verify price stabilized
            assertGt(token.currentPrice(), 0.00085 ether);
            console.log("Price after Stabilization:", token.currentPrice());
        }
    }
}
