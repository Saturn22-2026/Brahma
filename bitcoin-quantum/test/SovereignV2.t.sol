// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Test.sol";
import "../src/BTQToken.sol";
import "../src/BTQAirdrop.sol";
import "../src/BTQMining.sol";
import "../src/SovereignConstants.sol";

contract BTQModularTest is Test {
    BTQToken public token;
    BTQAirdrop public airdrop;
    BTQMining public mining;

    address aiAgent = SovereignConstants.EMPOWER_ADDR;
    address user = address(0x6);

    function setUp() public {
        vm.warp(block.timestamp + 1 days);

        BTQToken tempToken = new BTQToken();
        vm.etch(SovereignConstants.HARDCODED_TOKEN_ADDR, address(tempToken).code);
        token = BTQToken(payable(SovereignConstants.HARDCODED_TOKEN_ADDR));

        airdrop = new BTQAirdrop();
        mining = new BTQMining(payable(address(token)));

        token.setMiningContract(address(mining));
        token.setAirdropContract(address(airdrop));
        token.setAIAgent(aiAgent);

        vm.deal(user, 1000 ether);
    }

    function test_InitialSupplyV4_Modular() public view {
        assertEq(token.totalSupply(), 100_000_000 * 1e18);
        assertEq(token.balanceOf(SovereignConstants.WEALTH_ADDR), 10_000_000 * 1e18);
    }

    function test_WhaleTax_Modular_Burn() public {
        vm.deal(user, 200_000 ether);
        vm.prank(user);
        token.buyTokens{value: 100_000 ether}();

        uint256 initialBurned = token.balanceOf(address(0));
        uint256 dumpAmount = 100_000 * 1e18;

        address pair = address(0xDECE);
        token.setAMMPair(pair, true);

        vm.prank(user);
        token.sellTokens(dumpAmount, 0);

        assertGt(token.balanceOf(address(0)), initialBurned);
    }
}
