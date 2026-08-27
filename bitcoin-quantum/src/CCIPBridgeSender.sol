// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import {IRouterClient} from "@chainlink/contracts-ccip/ccip/interfaces/IRouterClient.sol";
import {Client} from "@chainlink/contracts-ccip/ccip/libraries/Client.sol";
import {IERC20} from "@openzeppelin/contracts/token/ERC20/IERC20.sol";
import {Ownable} from "@openzeppelin/contracts/access/Ownable.sol";
import {BitcoinQuantum} from "./BitcoinQuantum.sol";

contract CCIPBridgeSender is Ownable {
    IRouterClient public immutable router;
    IERC20 public immutable linkToken;
    BitcoinQuantum public immutable btqToken;

    constructor(address _router, address _link, address _btq) Ownable(msg.sender) {
        router = IRouterClient(_router);
        linkToken = IERC20(_link);
        btqToken = BitcoinQuantum(_btq);
    }

    /**
     * @notice Locks BTQ on this chain and sends a message to mint on destination.
     */
    function bridgeBTQ(
        uint64 destinationChainSelector,
        address receiver,
        uint256 amount
    ) external returns (bytes32 messageId) {
        require(amount > 0, "Amount must be > 0");

        // 1. Lock BTQ from user (Requires user approval)
        btqToken.transferFrom(msg.sender, address(this), amount);

        // 2. Prepare CCIP Message (Data-only for initial bootstrap)
        // In a full implementation, we might use CCIP Token Transfers too.
        Client.EVM2AnyMessage memory message = Client.EVM2AnyMessage({
            receiver: abi.encode(receiver),
            data: abi.encode(amount), // Telling the other side to mint this much
            tokenAmounts: new Client.EVMTokenAmount[](0),
            feeToken: address(linkToken),
            extraArgs: Client._argsToBytes(Client.EVMExtraArgsV1({gasLimit: 200_000}))
        });

        // 3. Handle Fees
        uint256 fees = router.getFee(destinationChainSelector, message);
        require(linkToken.balanceOf(address(this)) >= fees, "Insufficient LINK for fees");
        linkToken.approve(address(router), fees);

        // 4. Send
        messageId = router.ccipSend(destinationChainSelector, message);
    }
}
