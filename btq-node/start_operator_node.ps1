# Operator L1 on this machine. Cloudflared Public Hostname must target http://127.0.0.1:8545
# LAN wallet: http://192.168.168.16:8545  GSM wallet: https://www.brahmnetwork.com
# Same ledger. Do not set BRAH_PUBLIC_RPC to loopback — phones on GSM cannot use it.
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$data = if ($env:BRAH_DATA_DIR) { $env:BRAH_DATA_DIR } elseif ($env:BTQ_DATA_DIR) { $env:BTQ_DATA_DIR } else { Join-Path $root "btq-data" }
$env:BRAH_DATA_DIR = $data
$env:BRAH_BIND = if ($env:BRAH_BIND) { $env:BRAH_BIND } elseif ($env:BTQ_BIND) { $env:BTQ_BIND } else { "0.0.0.0" }
$env:BRAH_ALLOW_LAN = if ($env:BRAH_ALLOW_LAN) { $env:BRAH_ALLOW_LAN } elseif ($env:BTQ_ALLOW_LAN) { $env:BTQ_ALLOW_LAN } else { "1" }
$env:BRAH_ALLOW_SECRETS_ON_DISK = if ($env:BRAH_ALLOW_SECRETS_ON_DISK) { $env:BRAH_ALLOW_SECRETS_ON_DISK } elseif ($env:BTQ_ALLOW_SECRETS_ON_DISK) { $env:BTQ_ALLOW_SECRETS_ON_DISK } else { "1" }
$env:BRAH_SERVE_APK = if ($env:BRAH_SERVE_APK) { $env:BRAH_SERVE_APK } elseif ($env:BTQ_SERVE_APK) { $env:BTQ_SERVE_APK } else { "1" }
$env:BRAH_EXPLORER = if ($env:BRAH_EXPLORER) { $env:BRAH_EXPLORER } elseif ($env:BTQ_EXPLORER) { $env:BTQ_EXPLORER } else { "1" }
$env:BRAH_RPC_PORT = if ($env:BRAH_RPC_PORT) { $env:BRAH_RPC_PORT } elseif ($env:BTQ_RPC_PORT) { $env:BTQ_RPC_PORT } else { "8545" }
Remove-Item Env:BRAH_PUBLIC_RPC -ErrorAction SilentlyContinue
Remove-Item Env:BTQ_PUBLIC_RPC -ErrorAction SilentlyContinue

$py = (Get-Command python -ErrorAction Stop).Source
Set-Location $PSScriptRoot
Write-Host "BRAHMNETWORK L1 operator node. Tunnel origin http://127.0.0.1:$($env:BRAH_RPC_PORT)"
Write-Host "LAN site+RPC http://192.168.168.16:$($env:BRAH_RPC_PORT)  GSM https://www.brahmnetwork.com"
Write-Host "Idle sleep is blocked while this process runs. Lid-close: run keep_operator_awake.ps1 once."
& $py -u l1_mainnet.py
