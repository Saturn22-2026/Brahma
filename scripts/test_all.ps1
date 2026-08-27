Write-Host "=== Starting Bitcoin-Quantum Comprehensive Protocol Test Suite ===" -ForegroundColor Cyan

# 1. C++ Core Tests
Write-Host "`n[1/4] Running C++ Core Tests (Quantum Primitives)..." -ForegroundColor Yellow
if (Test-Path "bitcoin/build/src/test/test_bitcoin.exe") {
    & "bitcoin/build/src/test/test_bitcoin.exe" --run_test=crypto_envelope_tests,key_tests,pubkey_tests
} else {
    Write-Warning "test_bitcoin.exe not found. Ensure the project is built with CMake."
}

# 2. Go L2 Sequencer Tests
Write-Host "`n[2/4] Running Go L2 Sequencer Tests (Smooth Emission)..." -ForegroundColor Yellow
if (Get-Command go -ErrorAction SilentlyContinue) {
    Push-Location sequencer
    go test -v ./...
    Pop-Location
} else {
    Write-Warning "Go (golang) not found. Skipping L2 tests."
}

# 3. Python Quantum Relay Tests
Write-Host "`n[3/4] Running Python Quantum Relay Tests (Z-K Routing)..." -ForegroundColor Yellow
if (Get-Command pytest -ErrorAction SilentlyContinue) {
    pytest quantum_relay/tests/ -v
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    python -m pytest quantum_relay/tests/ -v
} else {
    Write-Warning "Python or Pytest not found. Skipping Relay tests."
}

# 4. Solidity Foundry Tests
Write-Host "`n[4/4] Running Solidity Foundry Tests (On-Chain Economy)..." -ForegroundColor Yellow
if (Get-Command forge -ErrorAction SilentlyContinue) {
    Push-Location foundry
    forge test --fuzz-runs 1000
    Pop-Location
} else {
    Write-Warning "Foundry (forge) not found. Skipping Smart Contract tests."
}

Write-Host "`n=== Test Suite Execution Complete ===" -ForegroundColor Cyan
