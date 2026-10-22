<p align="center">
  <img src="logo.png" width="300" alt="Brahma Coin Logo">
</p>

# Brahma Coin · BRAHMNETWORK

Private post-quantum **Layer 1** testnet. This is not Bitcoin, not an Ethereum token as consensus, and **not a public mainnet**.

The running ledger is the Python node in `btq-node/l1_mainnet.py`. There is no Solidity or Rust consensus tree in this repo. The public site is that node: `GET /` (home), `/claim`, `/explorer`, `/rpc`, and JSON-RPC `POST /`. That is not an Ethereum RPC provider (not dRPC).

`public_launch` is **false**. Treat every balance, APK, and RPC as a private network.

Brahma Coin has **no market price**. The node does not quote USD, and the wallet must not invent a dollar value.

## What the node actually enforces

- **One ledger**: JSON-RPC on port 8545 (`brah_submitTx`, `brah_getAccount`, `brah_getStats`, …).
- **Spends**: Dilithium3 over `BRAH1|{chain_id}|{nonce}|{kind}|{from}|{to}|{asset}|{amount_units}|{memo}`. `from` and send `to` must be checksummed `BRM1G` (33 chars; legacy `BTQ1G` still resolves). Amounts must be positive. Client `hash` is ignored; the node hashes the canonical message.
- **Protocol organs cannot spend**: mining, airdrop, and other `POOL_*` treasuries cannot be `from`.
- **Integer money**: 1 Brahma Coin = 100,000,000 units. Consensus never uses float amounts.
- **Client PoW**: the wallet/node client solves SHA-256 leading bits on `{height}|{tip}|{miner}|{reward}|{tx_root}` and sends `pow_nonce`. Genesis starts at **20 bits** (retarget floor 18, cap 24). The node does not search nonces for RPC submits.
- **Blocks contain txs**. Mesh ingest accepts **one** signed tx per block. Reward is paid only for a signed `mine` from the miner, equal to `block_reward`, under yearly/pool caps.
- **Authenticated P2P**: Sentinel DTN (TCP 18545, UDP 18547). HELLO must carry a Dilithium3 signature over `BRAHHELLO|{node}|{tcpPort}|{ts}` (legacy `BTQHELLO` still verifies) with `|now-ts| ≤ 90s`. Unsigned HELLO is dropped.
- **L2 memecoins**: nine genesis coins use **whole token units** (not ×1e8). A one-time 5B grant of each goes to the operator wallet. At each mine-schedule halving (after 10k joins, then every 100k), a **shared 750M pool** of each genesis L2 is **split** across joined/mined wallets (not 750M each). When `POOL_MINING` is empty, remaining operator-grant L2 is released once and split the same way (`l2_mining_empty_released`). New L2 launches are closed — `brah_launchL2` / `launch_l2` refuse; there is no burn-to-launch.
- **Whale tax**: 25% (`2500` bps) when a Brah send is larger than **10%** of circulating.
- **Adoption rewards**: 30M `POOL_AIRDROP`; join tiers 100/50 then 50/25 then half every 25k joins. Joins are capped (`BRAH_JOIN_MAX_PER_HOUR`, default 20). One claim per phone (`dev:<phoneId>[:referrer]`).
- **No donation pool**: economics v2 sets `donation_pool` to **0**.
- **RPC bind**: default `127.0.0.1`. Phone LAN needs `BRAH_ALLOW_LAN=1` and `BRAH_BIND=0.0.0.0`. CORS is localhost-only (not `*`).
- **TLS**: set `BRAH_TLS_CERT` and `BRAH_TLS_KEY`. APK download at `/wallet.apk` is off unless `BRAH_SERVE_APK=1`.
- **Dilithium verify (operator)**: wallet signs with BouncyCastle **1.72** (3293-byte Dilithium3). Point `BRAH_JAVA` at JBR 17 and `BRAH_BCPROV_JAR` at `btq-node/bcprov-jdk18on-1.72.jar`. BC **1.78.1 rejects** those signatures.

## Supply (100,000,000 Brahma Coin)

Canonical numbers live in `btq-node/genesis.json` (economics v2).

| Pool | Amount |
| --- | ---: |
| Mining (`POOL_MINING`, 0.00014 Brah/block, 5M/year cap) | 50,000,000 |
| Adoption rewards (`POOL_AIRDROP`) | 30,000,000 |
| 50 distribution wallets (200,000 each) | 10,000,000 |
| Empower first 100 wallets (30,000 each when mining empties) | 3,000,000 |
| Empower next 100 wallets (20,000 each at that same moment) | 2,000,000 |
| Strategic burn (100,000 on the first Friday of each month, UTC) | 3,000,000 |
| Founder (pays wallet 1 when mining empties) | 2,000,000 |
| Donation | 0 |

Genesis founder / empower / strategic / council / 50 distribution wallets are Dilithium3 keys from `python generate_genesis_keys.py` → `genesis_accounts.json` (public) and `genesis_secrets.json` (operator-only, gitignored).

## Run a private node

```bash
cd btq-node
python -m pip install -r requirements.txt
python generate_genesis_keys.py
python l1_mainnet.py
```

Optional:

- `BRAH_DATA_DIR` — state + chain + node identity (default: `btq-node/`). Legacy float/`GENESIS_*` state is refused; use a new directory.
- `BRAH_BIND` / `BRAH_ALLOW_LAN=1` — default bind is localhost. LAN/phone tests must set both (`BRAH_BIND=0.0.0.0`).
- `BRAH_JOIN_MAX_PER_HOUR` — adoption join cap (default 20).
- `BRAH_P2P_PORT` / `BRAH_PEERS=host:18545` — mesh
- `BRAH_RPC_PORT` — HTTP RPC (default 8545). A same-host replica uses 8546.
- `BRAH_PUBLIC_RPC` — stable HTTPS hostname. Written to `<data>/public_rpc.txt`. Wallet debug APK bakes `local.properties` `brah.publicRpc=` or that file.
- `BRAH_TLS_CERT` / `BRAH_TLS_KEY` — HTTPS RPC. `BRAH_REQUIRE_TLS=1` refuses HTTP.
- `BRAH_SERVE_APK=1` — required to serve `/wallet.apk` on loopback/LAN. Cloudflare-forwarded GSM requests never get the APK, even with this flag.
- `BRAH_JAVA` / `BRAH_BCPROV_JAR` — Dilithium checker (JBR 17 + bcprov **1.72**). See `repair_windows_signature_checker.ps1`.
- `BRAH_ALLOW_SECRETS_ON_DISK=1` — only local exception if `genesis_secrets.json` is still next to node data. Default is refuse to start.
- Replica catch-up (not consensus; two processes on one PC are not independent operators): `.\start_replica.ps1` — separate `btq-data-replica`, P2P 18546, RPC 8546, `BRAH_PEERS=127.0.0.1:18545`. Behind peers request `SYNC` / `brah_getBlocks`.
- Second physical peer (Tailscale): `.\start_second_peer_tailscale.ps1` then `.\check_peer_status.ps1`.
- Public GSM: `.\start_named_tunnel.ps1` (Cloudflare named tunnel + hostname). `.\start_gsm_tunnel.ps1` is the ephemeral trycloudflare fallback.

War-game checks (not a firm audit):

```bash
cd btq-node
python test_launch_gates.py
```

## Android wallet

Debug APK prefers LAN `192.168.168.16:8545` when the phone is on `192.168.168.*` (1s connect), then the public HTTPS host (`brah.publicRpc` / `public_rpc.txt`), then USB. **Share** is one public link: `https://www.brahmnetwork.com/claim?ref=<checksummed BRM1G>`. Mesh `INVITE` packets must be Dilithium-signed by that address. `/wallet.apk` needs `BRAH_SERVE_APK=1` on the origin that serves it. Release builds disable cleartext; they need TLS. New identities are **24 words** (plus checksum word). Send / burn / mnemonic reveal use the wallet password when send-lock is on. New L2 launches are closed (no burn-to-launch). Mining only reports sealed after `brah_submitTx` or a confirmed height increase. There is no QUSD. Current tree: wallet **1.0.54** (`versionCode` 60).

```bash
cd wallet-android
.\gradlew :app:assembleDebug
```

## What this repo does not claim

- No Rust L1 in production.
- No EMP / VLF / military-grade product.
- No branded third-party audit. See [docs/INTERNAL_REVIEW.md](docs/INTERNAL_REVIEW.md).
- No public Ethereum mainnet deploy as Brah L1.
- Hybrid Ed25519+ML-DSA is not what the running node verifies. The node verifies Dilithium3.

## License

MIT. See [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE).
