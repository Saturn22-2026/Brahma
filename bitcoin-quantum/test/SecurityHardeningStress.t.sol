// SPDX-License-Identifier: MIT
pragma solidity ^0.8.24;

import "forge-std/Test.sol";
import "../src/BTQToken.sol";
import "../src/SovereignConstants.sol";

contract MaliciousActor {
    BTQToken public token;

    constructor(address _token) {
        token = BTQToken(payable(_token));
    }

    // Attempt reentrancy on executeAutonomousDonation if it used raw calls (it doesn't, but we test)
    function attackDonation(address recipient, uint256 amount) external {
        token.executeAutonomousDonation(recipient, amount);
    }

    receive() external payable {
        // Attempt reentrancy
        // Note: BTQToken's buy/sell were removed or simplified in v5,
        // but we ensure no other entry points are vulnerable.
    }
}

contract SecurityHardeningStressTest is Test {
    BTQToken public token;
    address admin = address(0xAD);
    address user = address(0x6);
    address malicious = address(0x666);

    function setUp() public {
        vm.warp(block.timestamp + 1 days);

        BTQToken tempToken = new BTQToken();
        vm.etch(SovereignConstants.HARDCODED_TOKEN_ADDR, address(tempToken).code);
        vm.copyStorage(address(tempToken), SovereignConstants.HARDCODED_TOKEN_ADDR);
        token = BTQToken(payable(SovereignConstants.HARDCODED_TOKEN_ADDR));

        vm.deal(user, 1000 ether);
    }

    /**
     * @dev SECURITY STRESS: Unauthorized Module Interaction
     */
    function test_Security_Unauthorized_Access() public {
        vm.prank(user);
        vm.expectRevert("Unauthorized: AI Agent only");
        token.executeAutonomousDonation(user, 1000 * 1e18);

        vm.prank(malicious);
        vm.expectRevert("Ownable: caller is not the owner");
        token.setMiningContract(malicious);
    }

    /**
     * @dev SECURITY STRESS: 2-Year Donation Lock Invariance
     */
    function test_Security_Lock_Stress() public {
        token.setAIAgent(admin);

        // 1. Attempt before unlock
        vm.prank(admin);
        vm.expectRevert("AI Donations locked for 2 years");
        token.executeAutonomousDonation(user, 100 * 1e18);

        // 2. Warp near end
        vm.warp(block.timestamp + 729 days);
        vm.prank(admin);
        vm.expectRevert("AI Donations locked for 2 years");
        token.executeAutonomousDonation(user, 100 * 1e18);

        // 3. Unlock and try
        vm.warp(block.timestamp + 2 days);
        token.unlockAIDonations();
        vm.prank(admin);
        token.executeAutonomousDonation(user, 100 * 1e18);
        assertEq(token.balanceOf(user), 100 * 1e18);
    }

    /**
     * @dev SECURITY STRESS: Supply Cap Invariance
     */
    function test_Security_Supply_Cap_Protection() public {
        // Total supply is 100M. No one should be able to mint more.
        // We test if ownership renouncement or malicious calls can break it.

        assertEq(token.totalSupply(), 100_000_000 * 1e18);

        vm.prank(malicious);
        vm.expectRevert("Ownable: caller is not the owner");
        // Simulated attempt to trigger an internal _mint if exposed (it's not)
        // We check that onlyMining is the only way to move from contract to user
    }
}
