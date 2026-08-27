# Bitcoin-Quantum: Sovereign Node & Wallet Bridge
# This script initializes the L1 Core and bridges the Physical Wallet via USB.

Write-Host "🚀 Initializing Bitcoin-Quantum Mainnet Bridge..." -ForegroundColor Cyan

# 1. Establish ADB Bridge
Write-Host "🔗 Bridging Port 8545 to Android Device..." -ForegroundColor Yellow
& "adb" reverse tcp:8545 tcp:8545
if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Bridge Active: http://127.0.0.1:8545" -ForegroundColor Green
} else {
    Write-Host "❌ ADB Error: Ensure your phone is connected via USB and 'File Transfer' is enabled." -ForegroundColor Red
}

# 2. Start the L1 Node
Write-Host "⛓️  Starting L1 Sovereign Node..." -ForegroundColor Yellow
Set-Location "$PSScriptRoot/../btq-node"
cargo run --bin btq-node --release

Write-Host "🏁 Node Session Terminated." -ForegroundColor Gray
