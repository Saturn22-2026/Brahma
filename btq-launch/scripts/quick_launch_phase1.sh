#!/bin/bash

# ╔══════════════════════════════════════════════════════╗
# ║  PHASE 1:  L2 DEPLOYMENT (BTQ ECOSYSTEM)  ║
# ╚══════════════════════════════════════════════════════╝

set -e

echo "🚀 Initiating BTQ Quick Launch: Phase 1..."
echo "=========================================="

# Configuration (Defaults to Anvil/Local)
NETWORK=${NETWORK:-"localhost"}
RPC_URL=${RPC_URL:-"http://127.0.0.1:8545"}
PRIVATE_KEY=${PRIVATE_KEY:-"0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80"}

# Step 1: Compile Sovereign Contracts
echo ""
echo "📦 Step 1: Compiling high-fidelity contracts..."
forge build --optimize --runs 200

# Step 2: Deploy LaunchFactory Coordinator
echo ""
echo "🏭 Step 2: Deploying LaunchFactory Coordinator..."
FACTORY_ADDRESS=$(forge create \
    contracts/LaunchFactory.sol:LaunchFactory \
    --rpc-url "$RPC_URL" \
    --private-key "$PRIVATE_KEY" \
    | grep "Deployed to:" | awk '{print $3}')

if [ -z "$FACTORY_ADDRESS" ]; then
    echo "❌ Error: LaunchFactory deployment failed."
    exit 1
fi

echo "✅ LaunchFactory active at: $FACTORY_ADDRESS"
echo "$FACTORY_ADDRESS" > factory_address.txt

# Step 3: Trigger Sovereign Token Deployment via Factory
echo ""
echo "🪙 Step 3: Deploying Sovereign BTQ Token (100M Supply)..."
cast send \
    "$FACTORY_ADDRESS" \
    "deploySovereignToken()" \
    --rpc-url "$RPC_URL" \
    --private-key "$PRIVATE_KEY"

TOKEN_ADDRESS=$(cast call "$FACTORY_ADDRESS" "tokenAddress()(address)" --rpc-url "$RPC_URL")
echo "✅ BTQToken active at: $TOKEN_ADDRESS"
echo "$TOKEN_ADDRESS" > token_address.txt

# Step 4: Deploy & Configure L2 Factory
echo ""
echo "⚙️ Step 4: Configuring Memecoin Expansion Layer (L2)..."
cast send \
    "$FACTORY_ADDRESS" \
    "deployAndConfigureFactory()" \
    --rpc-url "$RPC_URL" \
    --private-key "$PRIVATE_KEY"

L2_FACTORY_ADDRESS=$(cast call "$FACTORY_ADDRESS" "factoryAddress()(address)" --rpc-url "$RPC_URL")

# Step 5: Verification
echo ""
echo "✅ Step 5: Finalizing Phase 1 Verification..."
IS_DEPLOYED=$(cast call "$FACTORY_ADDRESS" "isDeployed()(bool)" --rpc-url "$RPC_URL")

if [ "$IS_DEPLOYED" = "true" ]; then
    echo ""
    echo "╔═══════════════════════════════════════════════╗"
    echo "║  ✅ PHASE 1 COMPLETE: ECOSYSTEM INITIALIZED   ║"
    echo "╠═══════════════════════════════════════════════╣"
    echo "║  BTQ Token:   $TOKEN_ADDRESS"
    echo "║  L2 Factory:  $L2_FACTORY_ADDRESS"
    echo "║  Coordinator: $FACTORY_ADDRESS"
    echo "║  Network:     $NETWORK"
    echo "╚═══════════════════════════════════════════════╝"

    # Generate deployment manifest for L1 Node & AI Council
    cat > deployment_phase1.json << EOF
{
    "phase": 1,
    "status": "_READY",
    "token": "$TOKEN_ADDRESS",
    "l2_factory": "$L2_FACTORY_ADDRESS",
    "coordinator": "$FACTORY_ADDRESS",
    "network": "$NETWORK",
    "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
    "next_phase": "L1_NODE_INITIALIZATION"
}
EOF

    echo "💾 Sovereign Manifest saved to: deployment_phase1.json"
    exit 0
else
    echo "❌ Verification failed: Ecosystem state is inconsistent."
    exit 1
fi
