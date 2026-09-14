# Same-machine replica catch-up. This is not a second operator and not consensus.
# Primary must already be running on 127.0.0.1:18545 (default P2P).
# Uses a separate data dir so the live tip in btq-data is not rewritten.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$primary = if ($env:BRAH_DATA_DIR) { $env:BRAH_DATA_DIR } elseif ($env:BTQ_DATA_DIR) { $env:BTQ_DATA_DIR } else { Join-Path $root "btq-data" }
$replica = Join-Path $root "btq-data-replica"
New-Item -ItemType Directory -Force -Path $replica | Out-Null

foreach ($name in @("genesis_accounts.json")) {
    $src = Join-Path $primary $name
    if (-not (Test-Path $src)) { $src = Join-Path $PSScriptRoot $name }
    if (Test-Path $src) {
        Copy-Item $src (Join-Path $replica $name) -Force
    }
}

$env:BRAH_DATA_DIR = $replica
$env:BRAH_BIND = "127.0.0.1"
$env:BRAH_P2P_PORT = "18546"
$env:BRAH_RPC_PORT = "8546"
$env:BRAH_PEERS = "127.0.0.1:18545"
$env:BRAH_UDP_PORT = "18548"
Remove-Item Env:BRAH_ALLOW_SECRETS_ON_DISK -ErrorAction SilentlyContinue
Remove-Item Env:BRAH_ALLOW_LAN -ErrorAction SilentlyContinue
Remove-Item Env:BRAH_SERVE_APK -ErrorAction SilentlyContinue
Remove-Item Env:BTQ_ALLOW_SECRETS_ON_DISK -ErrorAction SilentlyContinue
Remove-Item Env:BTQ_ALLOW_LAN -ErrorAction SilentlyContinue
Remove-Item Env:BTQ_SERVE_APK -ErrorAction SilentlyContinue

Write-Host "Replica catch-up (not consensus). RPC 127.0.0.1:8546 P2P 18546 -> 18545"
Write-Host "Data: $replica"
Set-Location $PSScriptRoot
python -u l1_mainnet.py
