# Progress

**What works**

- L1 private node: `btq-node/l1_mainnet.py` + `genesis.json` (network **BRAHMNETWORK**, 100M split, first-mine +1 Brahma, **0.00014 Brahma/block** through 10,000 joins then half every 100,000, **20-bit** client PoW, retarget 18–24, 5M/year mine cap)
- HTTP `HEAD` on the same routes as `GET` (Cloudflare health probes)
- Dilithium3 on every spend; protocol pools cannot be `from`; integer units; checksummed `BRM1G` (new) and `BTQ1G` (live) from/to; positive amounts; canonical tx hash (client hash ignored)
- Same-body `BRM1G` / `BTQ1G` resolve to one balance bucket (`resolve_balance_key`)
- Mesh ingest: BLOCK linear extension; competing same-parent tips reorg by work then hash; **TRANSACTION** packets run `commit_signed`
- 100M Brahma outstanding cap (`balances["0"]` + burned) on every `save_state`
- `brah_getStats.mainnet_ready`: ≥1 P2P peer **and** named HTTPS RPC (not trycloudflare). `public_launch` only if that is true **and** `BRAH_PUBLIC_LAUNCH=1`
- Wallet **1.0.28**: Brahma / BRAHMNETWORK copy; after one online `brah_getAccount`, mine/join sign offline; queue + gossip TRANSACTION on LAN TCP, Bluetooth RFCOMM, Wi-Fi Aware (chunked). Flush when RPC is live. Share stays LAN on operator Wi‑Fi. Deep links: `brahm://claim` and `btq://claim`. Claim hosts: `www.brahmnetwork.com`, `brahmnetwork.com`
- Same-origin site: `GET /` is the BRAHMNETWORK explainer (`btq-node/static/index.html` + `/static/site.js`). `/claim` and `/status` stay the invite/APK page. `/explorer`, `/rpc`. JSON-RPC is `POST /`. Site copy matches the node: first mine is block reward +1 (1.00014 while 0.00014), join calculator uses the post-35k band, L2 names are ledger names, airdrop is joined **or** mined. CORS: localhost + `BRAH_CORS_ORIGIN` + `BRAH_PUBLIC_RPC` host + operator LAN when `BRAH_ALLOW_LAN=1`. Explorer: loopback or `BRAH_EXPLORER=1`
- RPC: default bind `127.0.0.1`, `BRAH_RPC_PORT` (default 8545), 256 KiB body. `/rpc` `"network": "BRAHMNETWORK"`
- P2P bind follows `BRAH_BIND`; HELLO Dilithium + 90s ts; HELLO `rpcPort` follows the actual RPC port; UDP port is `BRAH_UDP_PORT` or P2P+2; peer cap 64. Startup sends SYNC after HELLO can land
- `BRAH_PUBLIC_RPC` written to `<data>/public_rpc.txt` (now `https://www.brahmnetwork.com`). Loopback, dashboard, `that-host`, and dRPC are refused. Named tunnel: `start_named_tunnel.ps1`. Windows service: `install_cloudflared_service.ps1`. Public Hostname API: `publish_public_hostname.ps1`. Quick tunnel: `start_gsm_tunnel.ps1` (URL changes on restart)
- Always-on helpers: `start_operator_node.ps1`, `install_l1_windows_task.ps1` (this PC), `keep_operator_awake.ps1` (lid/sleep off), `btq-l1.service` + `install_l1_linux.sh` (Linux VPS). Origin for cloudflared is `http://127.0.0.1:8545`. Node process also requests Windows stay-awake. GSM still dies if this PC is off or has no internet.
- Replica catch-up: `start_replica.ps1` (`btq-data-replica`, P2P 18546, RPC 8546, UDP 18548, peer 18545). Not consensus. Same-host replica has matched live height when peering works
- Independent operator: `start_independent_operator.ps1` refuses this repo’s `btq-data` / `btq-data-replica`. Needs another machine
- Wallet **1.0.33**: GSM Mine/Share use `https://www.brahmnetwork.com`; operator LAN unchanged. L2 amounts one line. Hide balances. Mine entry is `mineBrahma()`. Cap error says Brahma Coin. `public_launch` false. DNS/tunnel for brahmnetwork.com still operator work.
- Genesis L2: 5B whole units of each genesis token to the operator grant address (once). At every mine-schedule halving, a **shared 750M pool** of each genesis L2 is **split** across joined/mined wallets (not 750M each). Join memo dev:<phoneId> binds one claim per phone; 6th distinct inbound sender to one address burns that hub. Site Join serves /wallet.apk. Wallet hide L1/L2 separately.
- `BRAH_ALLOW_LAN=1` does not 429 routed clients
- Firewall script uses all Windows profiles (including Public)
- Node `brah_getStats` / genesis no longer carry a USD display field
- Node: APK only with `BRAH_SERVE_APK=1`; refuse secrets on disk; GET rate limit; `brah_getBlocks` + ahead-block SYNC
- `test_launch_gates.py`: hardening + replica catch-up + mine-schedule + L2 + CORS/explorer + public site copy + dual HRP
- `test_miner_stress.py`: isolated temp `BRAH_DATA_DIR` miner load — 24 sequential mines (first-bonus once), stale/invalid `pow_nonce` rejected, 6 miners under lock to height 18, 12-bit search heat plus one 20-bit header. Never the live `:8545` ledger. Run: `python test_miner_stress.py` from `btq-node/`
- Bugbot review on `fix/gradle-sdk-issues`: composeApp `build.gradle.kts` parses; `gradle.properties` no longer pins a machine JDK; `generate_genesis_keys.py` imports `Path`/`json`/`secrets`/`Dilithium3`/`address_from_pubkey`; genesis `engine.js` persists seed and restores keys from the displayed phrase; `mineBrahma()` refuses a second mine while busy instead of unlocking after 90s
- `canonical_rpc_method` maps `brah_*` → `brah_*` and does not double-prefix `brah_*` (wallet mine was failing with Method not found)

- Phone mine PoW uses leading SHA-256 bits (not BigInteger per hash) and a canonical BRM1G header so 20-bit work can finish and match `brah_submitTx`

- Join without a referrer can later bind one (50 Brahma Coin from the airdrop pool); wallet drops protocol-rejected mesh retries so mine/join no longer race on nonce

- Isolated `polymarket-hft/`: FastAPI on `127.0.0.1:8787`, Gamma market search, CLOB book poll, paper broker, live CLOB adapter behind `POLY_MODE=live`, risk/kill switch, autonomous two-sided scalper plus paper set-builder (sweep / phase quotes / rebalance / paper merge) on BTC/ETH/SOL 5m windows. Composite + calibrator gate directional size. Tests: `pytest` from that folder.

**Not started / backlog**

- Second independent operator machine (required before any mainnet claim)
- Cloudflare Public Hostname: `www.brahmnetwork.com` GET and `POST /rpc` returned **200** (2026-09-19 18:54 UTC), same ledger height as localhost. Tunnel Healthy on this PC.
- Second phone USB debugging
- Named-firm review (user action to hire; do not forge)

**Known issues**

- Operator with filesystem still owns the chain unless a second replica is catching up — and even then one operator disk can still rewrite the primary
- 20-bit PoW is not a public hashrate market
- Independent audit still not done; packet is `docs/INTERNAL_REVIEW.md`
- `www.brahmnetwork.com` HTTPS reaches this operator node (2026-09-19). GSM Mine works while the laptop, node, and Cloudflared stay up. LAN `http://192.168.168.16:8545` unchanged.
- dRPC / Ethereum RPC providers do not speak `brah_submitTx`
- Name collision note: “Brahma” is also used by a DeFi firm / BrahmaOS ERC-20; BRAHMNETWORK as an L1 name was unused

_Keep bullets factual and small; link issues or PRs when useful._
