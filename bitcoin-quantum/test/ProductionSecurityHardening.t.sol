// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "forge-std/Test.sol";
import "../src/BTQToken.sol";
import "../src/BTQMining.sol";
import "../src/BTQFaucet.sol";
import "../src/SovereignConstants.sol";

contract ProductionSecurityHardeningTest is Test {
    BTQToken public token;
    BTQMining public mining;
    BTQFaucet public faucet;

    address owner = address(0x1);
    address user = address(0x2);
    address ammPair = address(0x3);

    function setUp() public {
        vm.startPrank(owner);
        token = new BTQToken();
        mining = new BTQMining(payable(address(token)));
        faucet = new BTQFaucet(address(token));

        token.setMiningContract(address(mining));
        token.setAMMPair(ammPair, true);

        // Fund Faucet
        token.transfer(address(faucet), 1000 * 1e18);
        vm.stopPrank();
    }

    /**
     * @dev Test Task 1: Whale Tax Enforcement
     */
    function test_WhaleTax_Enforcement() public {
        // Active supply = Initial dist + reserves (approx 20M initially active)
        // 100M total, some locked in contract.
        // Deployment mints to EMPOWER_ADDR, STRATEGIC_ADDR, FOUNDER_ADDR, wallets.
        // Total active approx = 10M (Wallets) + 5M + 3M + 2M = 20M.

        vm.startPrank(owner);
        token.transfer(user, 1_000_000 * 1e18); // Give user 1M tokens
        vm.stopPrank();

        uint256 userInitialBalance = token.balanceOf(user);
        uint256 amountToSell = 600_000 * 1e18; // > 2.5% of ~20M active supply

        vm.prank(user);
        token.transfer(ammPair, amountToSell);

        uint256 tax = (amountToSell * 2500) / 10000;
        uint256 received = amountToSell - tax;

        assertEq(token.balanceOf(ammPair), received, "AMM should receive taxed amount");
        assertEq(token.balanceOf(address(0)), tax, "Tax should be burned");
    }

    /**
     * @dev Test Task 3: PoW Mining Security
     */
    function test_PoW_Mining() public {
        uint256 nonce = 0;
        // Search for a valid nonce
        while (true) {
            bytes32 hash = keccak256(abi.encode(user, mining.userNonces(user), nonce));
            if (uint256(hash) < mining.difficulty()) {
                break;
            }
            nonce++;
        }

        vm.prank(user);
        mining.mine(nonce);

        assertEq(token.balanceOf(user), 0.1 * 1e18, "Miner should receive reward");

        // Try same nonce again - should fail because userNonce updated
        vm.expectRevert("Invalid Proof of Work: Hash does not meet difficulty");
        vm.prank(user);
        mining.mine(nonce);
    }

    /**
     * @dev Test Task 2: Faucet Cooldown and Reentrancy
     */
    function test_Faucet_Security() public {
        vm.startPrank(user);
        faucet.requestTokens();
        assertEq(token.balanceOf(user), 100 * 1e18);

        vm.expectRevert("24h cooldown active");
        faucet.requestTokens();
        vm.stopPrank();
    }
}
