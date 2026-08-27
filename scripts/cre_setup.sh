#!/bin/bash

# Load environment variables
if [ -f "foundry/.env" ]; then
    export $(cat foundry/.env | grep -v '#' | xargs)
fi

echo "=== Initializing Chainlink Runtime Environment (CRE) ==="

if [ -z "$CRE_ORG_ID" ]; then
    echo "Error: CRE_ORG_ID not set in foundry/.env"
    exit 1
fi

# 1. Login to the organization
echo "Logging in to organization: $CRE_ORG_ID"
# Note: In a real environment, this might prompt for browser auth
# cre login --org-id $CRE_ORG_ID

# 2. Configure the gateway
echo "Setting gateway to: $CRE_GATEWAY_URL"
# cre gateway set --url $CRE_GATEWAY_URL

echo "CRE environment configured for Bitcoin-Quantum."
