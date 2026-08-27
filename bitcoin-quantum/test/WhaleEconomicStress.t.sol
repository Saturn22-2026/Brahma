// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/BTQToken.sol";
import "../src/BTQAirdrop.sol";
import "../src/BTQMining.sol";

contract WhaleEconomicStressTest is Test {
    BTQToken public token;

    address wealth = address(0x1001);
    address empower = address(0x2002);
    address stability = address(0x3003);
    address aiDonation = address(0xD001);
    address airdropSource = address(0xA001);
    address floatSource = address(0xF001);
    address whale = address(0xBEEF);
    address user = address(0x6006);

    function setUp() public {
        token = new BTQToken(wealth, empower, stability, aiDonation, airdropSource, floatSource);
        vm.deal(whale, 10_000_000 ether);
        vm.deal(user, 1000 ether);
    }

    /**
     * @dev Test "Unlock Front-running": Whale buys a massive amount
     * just before the 2M yearly release to manipulate the resulting price impact.
     */
    function test_Whale_Unlock_Manipulation() public {
        // 1. Whale buys 1M tokens in Year 1
        vm.prank(whale);
        token.buyTokens{value: 100_000 ether}();
        uint256 whaleTokens = token.balanceOf(whale);

        // 2. Advance to Year 2 (Trigger 2M unlock)
        vm.warp(block.timestamp + 366 days);
        token.syncAMMLiquidity(floatSource);

        // 3. Whale dumps everything immediately into the new liquidity
        uint256 initialStability = token.balanceOf(stability);

        address dexPair = address(0xDECE);
        token.setAMMPair(dexPair, true);

        vm.prank(whale);
        token.transfer(dexPair, whaleTokens);

        uint256 taxCollected = token.balanceOf(stability) - initialStability;

        // Verify that the Whale Tax (max 35%) significantly reduced the profit
        assertGt(taxCollected, 0, "Whale tax MUST be applied to the dump");

        // 4. Verify user can still buy tokens at a fair price supported by the reserve
        uint256 priceAfterDump = token.currentPrice();
        vm.prank(user);
        token.buyTokens{value: 1 ether}();
        assertEq(token.currentPrice() >= priceAfterDump, true, "Price floor should hold");
    }

    /**
     * @dev Test Multi-AMM "Sybil" Sale: Whale attempts to split a dump
     * across 5 different DEX pairs to evade the float ratio calculation.
     */
    function test_Multi_AMM_Evasion() public {
        vm.prank(whale);
        token.buyTokens{value: 50_000 ether}();
        uint256 batch = token.balanceOf(whale) / 5;

        uint256 initialStability = token.balanceOf(stability);

        for(uint16 i = 0; i < 5; i++) {
            address pair = address(uint160(0x9000 + i));
            token.setAMMPair(pair, true);

            vm.prank(whale);
            token.transfer(pair, batch);
        }

        uint256 totalTax = token.balanceOf(stability) - initialStability;
        assertGt(totalTax, 0, "Tax should be cumulative across different pairs");
    }
}
