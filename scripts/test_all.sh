#!/bin/bash

echo -e "\033[0;36m=== Starting Bitcoin-Quantum Comprehensive Protocol Test Suite ===\033[0m"

# 1. C++ Core Tests
echo -e "\n\033[0;33m[1/4] Running C++ Core Tests (Quantum Primitives)...\033[0m"
if [ -f "bitcoin/build/src/test/test_bitcoin" ]; then
    ./bitcoin/build/src/test/test_bitcoin --run_test=crypto_envelope_tests,key_tests,pubkey_tests
else
    echo "Warning: test_bitcoin not found. Ensure the project is built with CMake."
fi

# 2. Go L2 Sequencer Tests
echo -e "\n\033[0;33m[2/4] Running Go L2 Sequencer Tests (Smooth Emission)...\033[0m"
if command -v go &> /dev/null; then
    cd sequencer && go test -v ./... && cd ..
else
    echo "Warning: Go not found. Skipping L2 tests."
fi

# 3. Python Quantum Relay Tests
echo -e "\n\033[0;33m[3/4] Running Python Quantum Relay Tests (Z-K Routing)...\033[0m"
if command -v pytest &> /dev/null; then
    pytest quantum_relay/tests/ -v
elif command -v python3 &> /dev/null; then
    python3 -m pytest quantum_relay/tests/ -v
else
    echo "Warning: Python/Pytest not found. Skipping Relay tests."
fi

# 4. Solidity Foundry Tests
echo -e "\n\033[0;33m[4/4] Running Solidity Foundry Tests (On-Chain Economy)...\033[0m"
if command -v forge &> /dev/null; then
    cd foundry && forge test --fuzz-runs 1000 && cd ..
else
    echo "Warning: Foundry (forge) not found. Skipping Smart Contract tests."
fi

echo -e "\n\033[0;36m=== Test Suite Execution Complete ===\033[0m"
