// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {CCIPReceiver} from "@chainlink/contracts-ccip/ccip/applications/CCIPReceiver.sol";
import {Client} from "@chainlink/contracts-ccip/ccip/libraries/Client.sol";
import {BitcoinQuantum} from "./BitcoinQuantum.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";

contract CCIPBridgeReceiver is CCIPReceiver, Ownable {
    BitcoinQuantum public immutable btqToken;

    constructor(address _router, address _btq) CCIPReceiver(_router) Ownable(msg.sender) {
        btqToken = BitcoinQuantum(_btq);
    }

    /**
     * @notice Handles incoming CCIP messages to mint tokens.
     */
    function _ccipReceive(Client.Any2EVMMessage memory message) internal override {
        // Decode the amount to mint
        (uint256 amount) = abi.decode(message.data, (uint256));

        // Recover the intended recipient from the message
        address recipient = abi.decode(message.sender, (address));

        // Mint/Release tokens to the user on this chain
        // Requires this contract to be an authorized bridge in BitcoinQuantum
        btqToken.bridgeTransfer(recipient, amount);
    }
}
