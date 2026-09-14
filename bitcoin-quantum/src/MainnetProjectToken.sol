// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @dev Highly optimized standard token interface for mainnet deployment.
 */
interface IERC20 {
    function totalSupply() external view returns (uint256);
    function balanceOf(address account) external view returns (uint256);
    function transfer(address to, uint256 value) external returns (bool);
    function transferFrom(address from, address to, uint256 value) external returns (bool);
}

contract MainnetProjectToken {
    // --- ERC20 State Variables ---
    string public name = "Mainnet Project Token";
    string public symbol = "MPT";
    uint256 public decimals = 18;
    uint256 public totalSupply;

    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;

    // --- System Control ---
    address public owner;

    // --- Mining Faucet State ---
    uint256 public difficultyTarget = 0x0000FFFF00000000000000000000000000000000000000000000000000000000;
    uint256 public baseMiningReward = 50 * 10**18; // 50 MPT
    mapping(address => uint256) public userNonces; // Prevents replay attacks

    // --- Referral Tree State ---
    mapping(address => address) public referrers; // User -> Direct Referrer
    uint256[3] public tierPercentages = [10, 5, 2]; // 10%, 5%, 2%

    // --- Events ---
    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
    event Mined(address indexed miner, uint256 reward, uint256 nonce);
    event ReferralPaid(address indexed teamMember, address indexed referrer, uint256 tier, uint256 amount);
    event AirdropDispatched(uint256 totalRecipients, uint256 totalTokens);

    modifier onlyOwner() {
        require(msg.sender == owner, "Not authorized");
        _;
    }

    constructor() {
        owner = msg.sender;
        // Mint initial supply for airdrops and liquidity pools to owner (e.g., 1,000,000 tokens)
        _mint(owner, 1000000 * 10**18);
    }

    // --- ERC20 Engine ---
    function transfer(address to, uint256 value) public returns (bool) {
        _transfer(msg.sender, to, value);
        return true;
    }

    function approve(address spender, uint256 value) public returns (bool) {
        allowance[msg.sender][spender] = value;
        emit Approval(msg.sender, spender, value);
        return true;
    }

    function transferFrom(address from, address to, uint256 value) public returns (bool) {
        require(allowance[from][msg.sender] >= value, "Insufficient allowance");
        allowance[from][msg.sender] -= value;
        _transfer(from, to, value);
        return true;
    }

    function _transfer(address from, address to, uint256 value) internal {
        require(to != address(0), "Invalid target address");
        require(balanceOf[from] >= value, "Balance exceeded");
        balanceOf[from] -= value;
        balanceOf[to] += value;
        emit Transfer(from, to, value);
    }

    function _mint(address account, uint256 value) internal {
        totalSupply += value;
        balanceOf[account] += value;
        emit Transfer(address(0), account, value);
    }

    // --- ⚒️ On-Chain Mining Faucet with Integrated Referrals ---
    /**
     * @notice Submits a computed nonce to mine tokens.
     * @param nonce The solution to the proof of work problem.
     * @param referrer Optional address of the user who invited the miner.
     */
    function mineFaucet(uint256 nonce, address referrer) external {
        // Generate cryptographic proof combining user address and unique transaction sequence
        bytes32 hash = keccak256(abi.encodePacked(msg.sender, userNonces[msg.sender], nonce));

        // On-chain check: Target evaluation bypassing any need for OS binary checks
        require(uint256(hash) < difficultyTarget, "Invalid proof of work solution");

        // Set up referral link if it doesn't already exist
        if (referrer != address(0) && referrers[msg.sender] == address(0) && referrer != msg.sender) {
            referrers[msg.sender] = referrer;
        }

        userNonces[msg.sender] += 1; // Increment sequence to update mining input for next attempt

        // Mint the primary block reward to the miner
        _mint(msg.sender, baseMiningReward);
        emit Mined(msg.sender, baseMiningReward, nonce);

        // Distribute multi-tier referral bonuses
        address currentReferrer = referrers[msg.sender];
        for (uint256 i = 0; i < 3; i++) {
            if (currentReferrer == address(0)) break; // End of chain reached early

            uint256 referralReward = (baseMiningReward * tierPercentages[i]) / 100;
            _mint(currentReferrer, referralReward);
            emit ReferralPaid(msg.sender, currentReferrer, i + 1, referralReward);

            currentReferrer = referrers[currentReferrer]; // Travel up the network tree
        }
    }

    // --- 🪂 High-Efficiency Mainnet Airdrop Engine ---
    /**
     * @notice Batches multiple distributions into a single transaction to radically cut gas fees.
     */
    function dispatchAirdrop(address[] calldata recipients, uint256[] calldata amounts) external onlyOwner {
        require(recipients.length == amounts.length, "Array lengths mismatch");

        uint256 totalDistributed = 0;
        for (uint256 i = 0; i < recipients.length; i++) {
            _transfer(owner, recipients[i], amounts[i]);
            totalDistributed += amounts[i];
        }

        emit AirdropDispatched(recipients.length, totalDistributed);
    }

    // --- Admin Operations ---
    function updateDifficulty(uint256 newTarget) external onlyOwner {
        difficultyTarget = newTarget;
    }
}
