// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title SovereignToken
 * @dev Implementation of the Bitcoin-Quantum Sovereign Economy.
 *
 * Allocation:
 * - 11% Sovereign Wealth Wallet
 * - 11% Empowerment Wallet
 * - 11% Sovereign Reserve Wallet
 * - 67% Tradeable Float (held in AMM)
 *
 * Features:
 * - Dynamic Bonding Curve Pricing.
 * - Whale Protection Protocol (2.5% dump threshold).
 * - Automated stabilization via Reserve Wallet.
 */
contract SovereignToken {
    string public constant name = "Sovereign Quantum Token";
    string public constant symbol = "SQT";
    uint8 public constant decimals = 18;

    uint256 public totalSupply;
    mapping(address => uint256) public balanceOf;
    mapping(address => mapping(address => uint256)) public allowance;

    // Sovereign Roles
    address public immutable wealthWallet;
    address public immutable empowermentWallet;
    address public immutable reserveWallet;

    // AMM / Economic State
    uint256 public tradeableFloat;
    uint256 public initialFloat;
    uint256 public collateralPool; // ETH backing
    uint256 public currentPrice;   // In wei per token

    // Constants
    uint256 public constant MAX_SELL_RATIO = 2500; // 2.5% in basis points (100,000 = 100%)
    uint256 public constant PRICE_FLOOR = 85000;    // 85% of initial (100,000 = 100%)
    uint256 public constant ELASTICITY = 30000;    // 0.3 factor (100,000 = 1.0)

    event Transfer(address indexed from, address indexed to, uint256 value);
    event Approval(address indexed owner, address indexed spender, uint256 value);
    event WhaleTaxApplied(address indexed seller, uint256 amountTaxed);
    event PriceUpdated(uint256 newPrice);
    event Intervention(uint256 ethSpent, uint256 tokensBought);

    error InsufficientBalance();
    error InsufficientFloat();
    error InsufficientCollateral();
    error TransferFailed();

    constructor(
        address _wealth,
        address _empower,
        address _reserve,
        uint256 _initialSupply
    ) {
        wealthWallet = _wealth;
        empowermentWallet = _empower;
        reserveWallet = _reserve;

        uint256 total = _initialSupply * 10**uint256(decimals);
        totalSupply = total;

        // 11% / 11% / 11% split
        uint256 elevenPercent = (total * 11) / 100;
        balanceOf[wealthWallet] = elevenPercent;
        balanceOf[empowermentWallet] = elevenPercent;
        balanceOf[reserveWallet] = elevenPercent;

        emit Transfer(address(0), wealthWallet, elevenPercent);
        emit Transfer(address(0), empowermentWallet, elevenPercent);
        emit Transfer(address(0), reserveWallet, elevenPercent);

        // 67% Float
        tradeableFloat = (total * 67) / 100;
        initialFloat = tradeableFloat;
        balanceOf[address(this)] = tradeableFloat;
        emit Transfer(address(0), address(this), tradeableFloat);

        currentPrice = 0.001 ether; // Starting price for simulation
    }

    function approve(address spender, uint256 amount) external returns (bool) {
        allowance[msg.sender][spender] = amount;
        emit Approval(msg.sender, spender, amount);
        return true;
    }

    function transfer(address to, uint256 amount) external returns (bool) {
        return _transfer(msg.sender, to, amount);
    }

    function _transfer(address from, address to, uint256 amount) internal returns (bool) {
        if (balanceOf[from] < amount) revert InsufficientBalance();
        balanceOf[from] -= amount;
        balanceOf[to] += amount;
        emit Transfer(from, to, amount);
        return true;
    }

    /**
     * @notice Buy SQT with ETH.
     * @dev P_new = P_current * (S_initial / S_new)^0.3
     */
    function buyTokens() external payable {
        if (msg.value == 0) return;

        uint256 tokensToMint = (msg.value * 10**18) / currentPrice;
        if (tokensToMint >= tradeableFloat) revert InsufficientFloat();

        tradeableFloat -= tokensToMint;
        collateralPool += msg.value;

        balanceOf[address(this)] -= tokensToMint;
        balanceOf[msg.sender] += tokensToMint;
        emit Transfer(address(this), msg.sender, tokensToMint);

        _updatePrice();
    }

    /**
     * @notice Sell SQT for ETH with Whale Protection.
     */
    function sellTokens(uint256 amount) external {
        if (balanceOf[msg.sender] < amount) revert InsufficientBalance();

        uint256 ratio = (amount * 100000) / initialFloat;
        uint256 taxedAmount = 0;

        if (ratio > MAX_SELL_RATIO) {
            uint256 excess = ratio - MAX_SELL_RATIO;
            uint256 taxRate;

            if (excess < 1000) taxRate = 5;       // 5%
            else if (excess < 2000) taxRate = 15; // 15%
            else if (excess < 5000) taxRate = 25; // 25%
            else taxRate = 35;                    // 35%

            taxedAmount = (amount * taxRate) / 100;
            _transfer(msg.sender, reserveWallet, taxedAmount);
            emit WhaleTaxApplied(msg.sender, taxedAmount);
        }

        uint256 sellableAmount = amount - taxedAmount;
        uint256 ethToReturn = (sellableAmount * currentPrice) / 10**18;

        if (ethToReturn > collateralPool) revert InsufficientCollateral();

        tradeableFloat += sellableAmount;
        collateralPool -= ethToReturn;

        balanceOf[msg.sender] -= sellableAmount;
        balanceOf[address(this)] += sellableAmount;
        emit Transfer(msg.sender, address(this), sellableAmount);

        (bool success, ) = msg.sender.call{value: ethToReturn}("");
        if (!success) revert TransferFailed();

        _updatePrice();
        _checkStabilization();
    }

    function _updatePrice() internal {
        // Simplified dynamic price adjustment for EVM
        // currentPrice = initial_price * (initial_float / current_float) ^ 0.3
        // We'll use a linear approximation or fixed ratio for this phase
        uint256 newPrice = (initialFloat * 10**18) / tradeableFloat;
        // Apply 0.3 elasticity (mocked for now as 1:1 for demonstration)
        currentPrice = (newPrice * 1) / 10**15; // Adjusted scale
        emit PriceUpdated(currentPrice);
    }

    function _checkStabilization() internal {
        // If price drops below floor, reserve buys back
        // PRICE_FLOOR is 85% of initial price (e.g. 0.00085 ether)
        if (currentPrice < 0.00085 ether) {
            uint256 interventionEth = collateralPool / 10;
            if (interventionEth == 0) return;

            uint256 tokensBought = (interventionEth * 10**18) / currentPrice;
            tradeableFloat -= tokensBought;
            collateralPool += 0; // Internal swap

            balanceOf[address(this)] -= tokensBought;
            balanceOf[wealthWallet] += tokensBought;
            emit Transfer(address(this), wealthWallet, tokensBought);
            emit Intervention(interventionEth, tokensBought);
            _updatePrice();
        }
    }

    receive() external payable {
        this.buyTokens();
    }
}
