# Active context

**Current focus** (one short paragraph): Public APK download. Node streams `/wallet.apk` in 64KB chunks in source. Release wallet minify is on. This PC has no `JAVA_HOME`, so the smaller APK is not built. VPS patch file is `/tmp/brahm-apk-patch.py` and has not been applied; live process still serves the 21,479,030-byte file.

Coin is **Brahma Coin**, L1 is **BRAHMNETWORK**. Parallel, isolated work: `polymarket-hft/` localhost paper scalper for Polymarket (port 8787). GitHub listing/docs use Brahma Coin (not Bitcoin-Quantum). Repo URL is still `Saturn22-2026/Bitcoin-Quantum` until renamed on GitHub. Operator node `0.0.0.0:8545`. Phone LAN `192.168.168.65` → `192.168.168.16:8545`.

**Latest (policy + download)**:

- Site Join download is `/wallet.apk` (direct APK), not fake Play Store toast.
- **VPS APK live (2026-09-22):** `BRAH_SERVE_APK=1`, `/var/lib/btq/wallet.apk` on origin; public `https://www.brahmnetwork.com/wallet.apk` → 200 (~21MB).
- **GSM outage (2026-09-22 ~20:00):** Cloudflare **530** + SSH timeout — VPS unreachable/hung. Reboot restored `btq-l1` + `cloudflared`. Health cron every 2m restarts L1 if local RPC ≠ 200. Instance is **498Mi RAM** + 945Mi swap. Caps set 2026-09-23: L1 MemoryMax **180M**, cloudflared MemoryMax **80M** (previous 400M+300M exceeded RAM). Real fix is an Ampere shape with **8 GB**.
- L2 mine-halving airdrop: **750M shared pool** split across active wallets (not 750M each).
- Join: **100 Brahma** (first tier) + memo `dev:<phoneId>[:referrer]` — **one claim per phone**.
- Anti scrap-phone farm: **6th distinct inbound sender** to one address burns all L1+L2 on that hub.
- Wallet: separate **Hide L1** / **Hide L2** toggles (Home + Settings; site sim too).

**In progress**:

- [x] Dilithium3 required on send/mine/join/launch/donate
- [x] Protocol pools unspendable as `from`
- [x] Integer units + txs in blocks + client PoW
- [x] Mesh TRANSACTION ingest + wallet offline queue
- [x] Bluetooth + Wi-Fi Aware discovery/chunking
- [x] Longest-work sibling reorg
- [x] 100M outstanding cap
- [x] Brand: Brahma / BRAHMNETWORK / www.brahmnetwork.com
- [x] Isolated miner stress (`btq-node/test_miner_stress.py`, temp ledger only)
- [x] Operator keep-awake helpers (idle sleep + lid script). Not a VPS.
- [x] Cloudflared Windows service reinstalled 2026-09-19 20:32 with the new tunnel token (`ha_connections=4`). Old Sept-9 token replaced. Desktop token file removed.
- [x] Cloudflare Public Hostname: `www.brahmnetwork.com` resolves (Cloudflare proxy). Public GET and `POST /rpc` **200**, same height as local (43). Tunnel Healthy, `config_version=1`, requests reaching this PC. GSM Mine can use this URL. Keep laptop + Cloudflared + node running. Do not set `BRAH_PUBLIC_LAUNCH=1`.
- [x] Oracle VPS `brahm-l1`: L1 + Cloudflared connector **both running**. Same tunnel token as laptop. Cutover verified 2026-09-21: laptop Cloudflared **Stopped**, public `/rpc` still **200** height 44 via VPS. Laptop L1 stopped to avoid split ledger. Laptop `btq-data` kept. Do not set `BRAH_PUBLIC_LAUNCH=1`.
- [ ] Second independent operator machine (`BRAH_PEERS` both ways)
- [ ] `BRAH_PUBLIC_LAUNCH=1` only after the two items above
- [x] Isolated Polymarket paper API + maker scalper in `polymarket-hft/` (`127.0.0.1:8787`, `POLY_MODE=paper`)
- [x] Universe resolver for Bitcoin vs USDT / Ethereum / Solana short windows (`GET /universe`, `POST /bot/start`)
- [x] Live CLOB adapter (`place_limit_order` via `polymarket-client`; needs `POLY_MODE=live` and `POLYMARKET_PRIVATE_KEY`)
- [x] Paper set-builder engine: composite signals, YES+NO sweep/quotes/rebalance, paper CTF merge, calibrator Kelly gate, `/prices-history` bars. Directional off until `n≥30` and Wilson ≥53%. Kill −15% from peak. Tweet stub 0.

**Decisions (recent)**:

- Coin display name is Brahma. Network name is BRAHMNETWORK. Public HTTPS is `https://www.brahmnetwork.com`.
- Keep `brah_*` RPC, `BTQ1|` messages, `com.brah.wallet`, `BRAH_*` env, mesh prefixes (`BTQINVITE`, `BTQHELLO`, `BrahF`).
- New wallets `BRM1G`. Existing `BTQ1G` balances stay. Same body under either HRP is one account.
- 100M cap stays. Users grow Brahma by mining at **0.00014 / block** (then half every 100,000 joins after 10,000). Referrals + join tiers + L2 memecoin halvings stay for adoption.
- Offline: sign + PoW locally after one online sync; gossip until a node seals.
- `mainnet_ready` in `brah_getStats` is peers≥1 **and** named HTTPS (not trycloudflare). That is not a market, not a price.

**Open questions**:

- In Cloudflare Zero Trust, attach Public Hostname `www.brahmnetwork.com` → `http://127.0.0.1:8545` to the tunnel that shows **Healthy on this PC**. Or put a token with Tunnel **Edit** + DNS **Edit** in User env `CF_API_TOKEN` (do not paste in chat) plus `CF_ACCOUNT_ID` / `CF_HOSTNAME` / `CF_ZONE_NAME` / `CF_TUNNEL_ID`. The all-Read token cannot publish. Then a second machine with `start_independent_operator.ps1`.
