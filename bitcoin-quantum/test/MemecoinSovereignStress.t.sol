// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/BTQL2Factory.sol";
import "../src/BTQToken.sol";

contract MemecoinSovereignStressTest is Test {
    BTQL2Factory public factory;
    BTQToken public token;

    address wealth = 0x98A1961A67a42F80735E2D075677B7f68748366C;
    address empower = 0xA31c890737920A939E4366C34E3EEE75af79a744;
    address stability = 0x5df596fc9aba769926c1ee3f98181bbf3b7a54c4;
    address donation = 0x0b93082D9b3C7C97fAcd250082899BAcf3af3885;
    address airdropSource = 0x19a00be544d1e065f257a9230172779a74101605;
    address floatSource = 0x66d4c1d13d710b8bc4f3801cf0d41e5c2c2f3a34;

    function setUp() public {
        token = new BTQToken(wealth, empower, stability, donation, airdropSource, floatSource);
        factory = new BTQL2Factory(address(token));

        vm.prank(floatSource);
        token.approve(address(factory), 1000 * 1e18);
    }

    /**
     * @dev Test Multi-Asset Bootstrap Integrity.
     * Proves that all 8 memecoins follow the exact Sovereign split.
     */
    function test_Memecoin_Bootstrap_Integrity() public {
        factory.bootstrapInitial8();

        string[8] memory symbols = ["HOMIE", "SLUM", "CRAZY", "BOUJIE", "QMILE", "SOF", "5AVE", "POOKIE"];

        for (uint i = 0; i < 8; i++) {
            (address tokenAddr, address airdropAddr) = factory.assetPairs(symbols[i]);
            BTQL2Memecoin mc = BTQL2Memecoin(tokenAddr);

            // Check Supply
            assertEq(mc.totalSupply(), 1_000_000_000 * 1e18);

            // Check Stability Share (5.88%)
            assertEq(mc.balanceOf(stability), 58_823_529 * 1e18);

            // Check Airdrop Link
            assertEq(mc.balanceOf(airdropAddr), 117_647_059 * 1e18);
        }
    }

    /**
     * @dev Test L2 Whale Protection on Memecoins.
     */
    function test_Memecoin_WhaleTax() public {
        factory.bootstrapInitial8();
        (address tokenAddr, ) = factory.assetPairs("POOKIE");
        BTQL2Memecoin pookie = BTQL2Memecoin(tokenAddr);

        address user = address(0xDEADBEEF);
        uint256 float = pookie.tradeableFloat();
        uint256 dumpAmount = (float * 5) / 100; // 5% of float

        // Give user some tokens
        vm.prank(factory.owner());
        pookie.transfer(user, dumpAmount);

        address pair = address(0x999);
        pookie.setAMMPair(pair, true);

        uint256 initialStability = pookie.balanceOf(stability);

        vm.prank(user);
        pookie.transfer(pair, dumpAmount);

        assertGt(pookie.balanceOf(stability), initialStability, "Memecoin Whale Tax failed");
    }
}
