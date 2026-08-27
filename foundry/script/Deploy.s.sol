// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "forge-std/Script.sol";
import "../src/SovereignToken.sol";

contract DeploySovereign is Script {
    function run() external {
        uint256 deployerPrivateKey = vm.envOr("PRIVATE_KEY", uint256(0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80));

        vm.startBroadcast(deployerPrivateKey);

        // Dummy addresses for deployment demonstration
        address wealth = 0x70997970C51812dc3A010C7d01b50e0d17dc79C8;
        address empower = 0x3C44CdDdB6a900fa2b585dd299e03d12FA4293BC;
        address reserve = 0x90F79bf6EB2c4f870365E785982E1f101E93b906;

        new SovereignToken(wealth, empower, reserve, 1_000_000);

        vm.stopBroadcast();
    }
}
