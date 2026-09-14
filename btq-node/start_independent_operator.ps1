# Run this copy on a SECOND PHYSICAL MACHINE. Same-PC replica is start_replica.ps1 (catch-up only).
# This script refuses to start if it would use this repo's live btq-data folder.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$thisLive = Join-Path $root "btq-data"
$data = if ($env:BRAH_DATA_DIR) { $env:BRAH_DATA_DIR } else { $env:BTQ_DATA_DIR }

if (-not $data) {
    throw "Set BRAH_DATA_DIR to a folder on THIS machine (not $thisLive). A second operator is a second disk."
}
$resolved = [IO.Path]::GetFullPath($data)
$live = [IO.Path]::GetFullPath($thisLive)
if ($resolved.TrimEnd('\') -eq $live.TrimEnd('\')) {
    throw "Refusing: BRAH_DATA_DIR is the live operator folder. That is not a second operator."
}
if ($resolved -like (Join-Path $root "btq-data-replica*")) {
    throw "Refusing: btq-data-replica on this PC is catch-up, not an independent operator."
}
$peers = if ($env:BRAH_PEERS) { $env:BRAH_PEERS } else { $env:BTQ_PEERS }
if (-not $peers) {
    throw "Set BRAH_PEERS=host:18545 to the first operator (LAN IP or public host). Two processes on one PC are not this."
}

$env:BRAH_DATA_DIR = $resolved
$env:BRAH_PEERS = $peers
$env:BRAH_BIND = if ($env:BRAH_BIND) { $env:BRAH_BIND } elseif ($env:BTQ_BIND) { $env:BTQ_BIND } else { "0.0.0.0" }
$env:BRAH_P2P_PORT = if ($env:BRAH_P2P_PORT) { $env:BRAH_P2P_PORT } elseif ($env:BTQ_P2P_PORT) { $env:BTQ_P2P_PORT } else { "18545" }
$env:BRAH_RPC_PORT = if ($env:BRAH_RPC_PORT) { $env:BRAH_RPC_PORT } elseif ($env:BTQ_RPC_PORT) { $env:BTQ_RPC_PORT } else { "8545" }
Remove-Item Env:BRAH_ALLOW_SECRETS_ON_DISK -ErrorAction SilentlyContinue
Remove-Item Env:BTQ_ALLOW_SECRETS_ON_DISK -ErrorAction SilentlyContinue

Write-Host "Independent operator candidate"
Write-Host "Data: $resolved"
Write-Host "Peers: $peers"
Write-Host "This is still not mainnet. public_launch stays false until separate machines actually peer."
Set-Location $PSScriptRoot
python -u l1_mainnet.py
