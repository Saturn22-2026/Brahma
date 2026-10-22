# Progress

**What works**

- L1 private node: `btq-node/l1_mainnet.py` + `genesis.json` (network **BRAHMNETWORK**, 100M split: mining 50M, adoption 30M, 50×200k distribution, 3M first-100 and 2M next-100 after mining empties, 2M founder to wallet 1 then, 3M strategic burn of 100k on the first Friday UTC. No donation pool. Nine L2 memecoins stay. First-mine +1 Brahma, **0.00014 Brahma/block** through 10,000 joins then half every 100,000, **20-bit** client PoW, retarget 18–24, 5M/year mine cap)
- HTTP `HEAD` on the same routes as `GET` (Cloudflare health probes)
- Dilithium3 on every spend; protocol pools cannot be `from`; integer units; checksummed `BRM1G` (new) and `BTQ1G` (live) from/to; positive amounts; canonical tx hash (client hash ignored)
- Same-body `BRM1G` / `BTQ1G` resolve to one balance bucket (`resolve_balance_key`)
- Mesh ingest: BLOCK linear extension; competing same-parent tips reorg by work then hash; **TRANSACTION** packets run `commit_signed`
- 100M Brahma outstanding cap (`balances["0"]` + burned) on every `save_state`
- `brah_getStats.mainnet_ready`: ≥1 P2P peer **and** named HTTPS RPC (not trycloudflare). `public_launch` only if that is true **and** `BRAH_PUBLIC_LAUNCH=1`
- Wallet **1.0.28**: Brahma / BRAHMNETWORK copy; after one online `brah_getAccount`, mine/join sign offline; queue + gossip TRANSACTION on LAN TCP, Bluetooth RFCOMM, Wi-Fi Aware (chunked). Flush when RPC is live. Share stays LAN on operator Wi‑Fi. Deep links: `brahm://claim` and `btq://claim`. Claim hosts: `www.brahmnetwork.com`, `brahmnetwork.com`
- Same-origin site: `GET /` is the BRAHMNETWORK explainer (`btq-node/static/index.html` + `/static/site.js` + brand `/static/brahma-brand.jpg` / `brahma-mark.png`). Economics v2 copy (50M/30M, no donation, nine stay, **750M shared L2 halving airdrop**, new L2 launches closed, join invite UI, wallet 1.0.53 / APK `?v=59`). `/claim` and `/status` serve that same page; join view reads `?ref=` and keeps `brahm://claim`. `/explorer` and `/wallet.apk` are localhost/LAN only and hidden on Cloudflare-forwarded GSM requests. `/rpc`. JSON-RPC is `POST /`. CORS: localhost + `BRAH_CORS_ORIGIN` + `BRAH_PUBLIC_RPC` host + operator LAN when `BRAH_ALLOW_LAN=1`. Pay rail RPC (except `brah_getFeeSchedule`) needs a Bearer partner key. Origin sends HSTS when `X-Forwarded-Proto: https`. `static_asset` serves js/css/html/svg/png/jpg/webp/ico.
- L2 mine-halving airdrop: **750M shared pool** of each genesis memecoin split across joined/mined wallets at each mine-schedule band (`maybe_l2_halving_airdrop`, `l2_halving_band_paid`). Not 750M each.
- When `POOL_MINING` is empty: remaining operator-grant L2 is split across joined/mined wallets once (`maybe_release_l2_on_mining_empty`, `l2_mining_empty_released`).
- New L2 launches closed: no burn-to-launch; `brah_launchL2` / `launch_l2` refuse.
- Wallet launcher icons: density mipmaps from the Brahma Coin medallion; `AndroidManifest` uses `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round`.
- Wallet **1.0.53** (`versionCode` 59): a new, added, or newly recovered address signs its own join. A saved referrer is included. The open wallet stays the primary when adding. One claim per phone remains. Installed on SM-A055F `R92X30JKZTY` 2026-10-19. The same APK is now `btq-node/wallet.apk` and `btq-data/wallet.apk`. This PC’s node serves it (24,270,464 bytes). `https://www.brahmnetwork.com/wallet.apk` is still 1.0.36 (3,290,749 bytes). Oracle SSH is still publickey denied.
- Wallet **1.0.52** (`versionCode` 58): BouncyCastle 1.72. Mine, send, and burn sign 3,293-byte Dilithium3, the size the live chain accepted on block 116. Installed on SM-A055F `R92X30JKZTY` 2026-10-18.
- Wallet **1.0.51** (`versionCode` 57): mine retries once with a `BTQ1|` signature after `signature invalid`. Installed on SM-A055F `R92X30JKZTY` 2026-10-18. Those signatures are 3,309 bytes and the live node rejects them.
- Node checker: `bouncycastle_java()` / `bouncycastle_jar()` prefer `BRAH_JAVA` + `BRAH_BCPROV_JAR`, then JBR 17 + **bcprov 1.72** beside the node (not 1.78.1). Windows: `repair_windows_signature_checker.ps1`; `start_operator_node.ps1` sets both env vars. Oracle script `repair_vps_signature_checker.sh` still mentions 1.78.1 — VPS must use 1.72 to accept wallet 1.0.52+ mines. Classpath uses `os.pathsep`.
- Wallet **1.0.50** (`versionCode` 56): recovered phrase opens that wallet; operator grant `BRM1Ge19737…` becomes first when stored; wallet list and import stay available. Installed on SM-A055F `R92X30JKZTY` 2026-10-18.
- Wallet **1.0.49** (`versionCode` 55): mine names the active address; no signing key → fail closed; RPC User-Agent. Installed on SM-A055F `R92X30JKZTY` 2026-10-17. Five 200k+1B mnemonic wallets are on the laptop ledger only. Live HTTPS `BRM1Gad764b7e…` is 0. DIST live-funding skipped (no genesis secrets; VPS SSH denied).
- Wallet **1.0.48** (`versionCode` 54): leftover first-id is the on-screen placeholder; recovering a phrase adds to the 60-wallet vault and does not replace Home. Extra wallets cannot add. Unit tests passed. Installed on SM-A055F `R92X30JKZTY` 2026-10-17.
- RPC: default bind `127.0.0.1`, `BRAH_RPC_PORT` (default 8545), 256 KiB body. `/rpc` `"network": "BRAHMNETWORK"`
- P2P bind follows `BRAH_BIND`; HELLO Dilithium + 90s ts; HELLO `rpcPort` follows the actual RPC port; UDP port is `BRAH_UDP_PORT` or P2P+2; peer cap 64. Startup sends SYNC after HELLO can land
- `BRAH_PUBLIC_RPC` written to `<data>/public_rpc.txt` (now `https://www.brahmnetwork.com`). Loopback, dashboard, `that-host`, and dRPC are refused. Named tunnel: `start_named_tunnel.ps1`. Windows service: `install_cloudflared_service.ps1`. Public Hostname API: `publish_public_hostname.ps1`. Quick tunnel: `start_gsm_tunnel.ps1` (URL changes on restart)
- Always-on helpers: `start_operator_node.ps1`, `install_l1_windows_task.ps1` (this PC), `keep_operator_awake.ps1` (lid/sleep off), `btq-l1.service` + `install_l1_linux.sh` (Linux VPS). Origin for cloudflared is `http://127.0.0.1:8545`. Node process also requests Windows stay-awake. GSM still dies if this PC is off or has no internet.
- Replica catch-up: `start_replica.ps1` (`btq-data-replica`, P2P 18546, RPC 8546, UDP 18548, peer 18545). Not consensus. Same-host replica has matched live height when peering works
- Independent operator: `start_independent_operator.ps1` refuses this repo’s `btq-data` / `btq-data-replica`. Needs another machine
- Tailscale second peer: `start_second_peer_tailscale.ps1` (defaults `BRAH_PEERS=100.65.174.24:18545`, data `btq-data-peer`); `check_peer_status.ps1` reads `brah_getStats.peers`. Tailscale link to thirosha is live; Brahma HELLO peering not yet
- README supply table aligned to economics v2 (2026-10-21): mining 50M, adoption 30M, no donation, 50×200k dist, empower 3M+2M, strategic 3M, founder 2M
- Public TLS watch: `check_public_tls.py` + daily Windows task / Linux timer. Cloudflare Universal SSL reissues the edge cert; the probe exits 2 if fewer than 21 days remain.
- Wallet **1.0.46** (`versionCode` 52): `FLAG_SECURE`; Settings RPC must be `https://` on `brahmnetwork.com` (debug still allows private LAN HTTP). Installed on SM-A055F `R92X30JKZTY`.
- Wallet **1.0.47** (`versionCode` 53): leftover first-id placeholder; mnemonic always persisted on extra-wallet open; public HTTPS preferred for RPC. Built, not confirmed installed.
- Wallet **1.0.45** (`versionCode` 51): Send 25% / 50% / 75% / Max fills the selected asset. Installed on SM-A055F `R92X30JKZTY`.
- Wallet **1.0.44** (`versionCode` 50): debug APK installed on SM-A055F `R92X30JKZTY` 2026-10-17. Burn and L2 launch use the same send-lock password gate as L1/L2 send.
- Wallet **1.0.43** (`versionCode` 49): debug APK built 2026-10-17 13:04 local (~21.4 MB) and installed on SM-A055F `R92X30JKZTY`. Send and lock-on-open use one wallet password (PBKDF2 verifier, never stored). Settings switches default on after the password is created. Unit tests passed.
- Wallet **1.0.42** (`versionCode` 48): debug APK built 2026-10-16 20:29 local, installed on SM-A055F `R92X30JKZTY` 2026-10-17. Share/claim URLs bake `https://www.brahmnetwork.com/claim?ref=`. Gradle files are Groovy; wrapper 8.11.1; JDK 17 pin; isolated `.android-sdk` (API 34).
- Wallet **1.0.36**: shrunk release (~3.3 MB); nine L2 balances on Home; Send asset picker; Hide L1/Hide L2; Mine uses public HTTPS when LAN HTTP is refused. Public download was `/wallet.apk?v=36` while origin was up.
- Genesis L2: 5B whole units of each genesis token to the operator grant address (once). At every mine-schedule halving, a **shared 750M pool** of each genesis L2 is **split** across joined/mined wallets (not 750M each). Join memo dev:<phoneId> binds one claim per phone; 6th distinct inbound sender to one address burns that hub. Site Join serves /wallet.apk. Wallet hide L1/L2 separately.
- `BRAH_ALLOW_LAN=1` does not 429 routed clients
- Firewall script uses all Windows profiles (including Public)
- Node `brah_getStats` / genesis no longer carry a USD display field
- Node: APK only with `BRAH_SERVE_APK=1`; refuse secrets on disk; GET rate limit; `brah_getBlocks` + ahead-block SYNC
- `test_launch_gates.py`: hardening + replica catch-up + mine-schedule + L2 + CORS/explorer + public site copy + dual HRP
- `test_miner_stress.py`: isolated temp `BRAH_DATA_DIR` miner load — 24 sequential mines (first-bonus once), stale/invalid `pow_nonce` rejected, 6 miners under lock to height 18, 12-bit search heat plus one 20-bit header. Never the live `:8545` ledger. Run: `python test_miner_stress.py` from `btq-node/`
- Bugbot review on `fix/gradle-sdk-issues`: composeApp `build.gradle.kts` parses; `gradle.properties` no longer pins a machine JDK; `generate_genesis_keys.py` imports `Path`/`json`/`secrets`/`Dilithium3`/`address_from_pubkey`; genesis `engine.js` persists seed and restores keys from the displayed phrase; `mineBrahma()` refuses a second mine while busy instead of unlocking after 90s
- `canonical_rpc_method` maps `brm_*` / `btq_*` → `brah_*` and does not double-prefix `brah_*` (wallet mine was failing with Method not found)
- BTQ→BRM non-breaking aliases (2026-10-22): env `BRM_*`, mesh `BRMHELLO`/`BRMINVITE`/`BRMF`/`BRM1|`, systemd `brm-l1.service`, inventory `docs/BRM_RENAME.md`. Disk paths `btq-node`/`btq-data` unchanged.
- Compliance gap pack (2026-10-22): EU/US/SA refreshed; **China (PRC)** added as prohibition / out-of-market (`COMPLIANCE_GAP_PACK.md`, `AUDIT_READINESS` #9 + §H, canvas). Geo/ToS blocks not shipped.

- Phone mine PoW uses leading SHA-256 bits (not BigInteger per hash) and a canonical BRM1G header so 20-bit work can finish and match `brah_submitTx`

- Join without a referrer can later bind one (50 Brahma Coin from the airdrop pool); wallet drops protocol-rejected mesh retries so mine/join no longer race on nonce

- Isolated `polymarket-hft/`: FastAPI on `127.0.0.1:8787`, Gamma market search, CLOB book poll, paper broker, live CLOB adapter behind `POLY_MODE=live`, risk/kill switch, autonomous two-sided scalper plus paper set-builder (sweep / phase quotes / rebalance / paper merge) on BTC/ETH/SOL 5m windows. Composite + calibrator gate directional size. Tests: `pytest` from that folder.

**Not started / backlog**

- Second independent operator machine (required before any mainnet claim)
- Cloudflare Public Hostname: `www.brahmnetwork.com` GET and `POST /rpc` returned **200** (2026-09-19 18:54 UTC), same ledger height as localhost. Tunnel Healthy on this PC.
- Second phone USB debugging
- Named-firm review (user action to hire; do not forge)

**Known issues**

- **2026-10-14:** `https://www.brahmnetwork.com` is **200** from VPS `brahm-l1` (`84.12.123.117`), height 49 tip `000000d837619491`. Laptop origin is a divergent height 67. Task `Brahm-Cloudflared` was connected (4 HA) and is now **Disabled**; Windows `cloudflared` service stays Stopped/Manual. `84.12.75.158` SSH still times out. Apex `brahmnetwork.com` has no address record. Peers 0, so `public_launch` stays false.

- Operator with filesystem still owns the chain unless a second replica is catching up — and even then one operator disk can still rewrite the primary
- 20-bit PoW is not a public hashrate market
- Independent audit still not done; packet is `docs/INTERNAL_REVIEW.md`
- `www.brahmnetwork.com` HTTPS reaches this operator node (2026-09-19). GSM Mine works while the laptop, node, and Cloudflared stay up. LAN `http://192.168.168.16:8545` unchanged.
- dRPC / Ethereum RPC providers do not speak `brah_submitTx`
- Name collision note: “Brahma” is also used by a DeFi firm / BrahmaOS ERC-20; BRAHMNETWORK as an L1 name was unused

_Keep bullets factual and small; link issues or PRs when useful._
