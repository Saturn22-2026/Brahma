# Active context

**Current focus** (one short paragraph): Public origin is the VPS again. 2026-10-14 ~09:35 UTC: `https://www.brahmnetwork.com` HEAD and `POST /rpc` **200** (CF-RAY JNB), height **49**, tip `000000d837619491`, peers 0. SSH `opc@84.12.123.117` (`brahm-l1`, key `ssh-key-2026-09-21.key`) is open; `btq-l1` and `cloudflared` **active** since 2026-10-12 05:45 GMT, RPC matches the public tip. `84.12.75.158` still times out. This PC’s ledger is a **different chain** (height **67**, tip `000011bbe13dfeb1`). Laptop task `Brahm-Cloudflared` had 4 HA connections on that same tunnel (`dup_edge_conn`, 0 requests proxied); **stopped and Disabled** 2026-10-14 ~09:49 UTC. Windows service `Cloudflared` stays **Stopped / Manual**. **Did not** set `BRAH_PUBLIC_LAUNCH=1`. Apex `brahmnetwork.com` is SOA only (no address). Do not re-enable the laptop tunnel while its chain differs from the VPS.

Coin is **Brahma Coin**, L1 is **BRAHMNETWORK**. Parallel, isolated work: `polymarket-hft/` localhost paper scalper for Polymarket (port 8787). GitHub listing/docs use Brahma Coin (not Brahma). Repo URL is still `Saturn22-2026/Brahma` until renamed on GitHub. Operator node `0.0.0.0:8545`. Phone LAN `192.168.168.65` → `192.168.168.16:8545`.

**Latest (policy + download)**:

- Site Join download is `/wallet.apk` (direct APK), not fake Play Store toast.
- **Wallet 1.0.36 (2026-09-28):** signed `brahma-wallet.apk` ~3,290,749 bytes on `/var/lib/btq/wallet.apk` and `/opt/btq/btq-node/wallet.apk`; site link `/wallet.apk?v=36`. Home lists nine L2s (Alesha/Bonn included). Release Mine falls through to `https://www.brahmnetwork.com` on operator LAN (cleartext LAN is refused).
- **Restore blocked (2026-10-06 ~16:40 UTC ACT):** HEAD `https://www.brahmnetwork.com` **530** / 1033. SSH timeouts: `opc@84.12.123.117:22` and `opc@84.12.75.158:22`. **No ledger host** — neither `/opt/btq` nor `/var/lib/btq` inspected. Services started: **none**. Windows `cloudflared` still **Stopped / Manual**. Exact next click: Compute → Boot Volumes → **Brahm-L1 (Boot Volume)** → **Create instance** from that volume (do not launch a new Oracle Linux image / instance-pool clone). Then SSH that public IP and start systemd `btq-l1` + `cloudflared`.
- **Origin outage (2026-10-01 ~14:27 UTC):** public GET and `/rpc` Cloudflare **530** / error 1033. Laptop `cloudflared` remains Stopped/Manual. No OCI CLI on this PC (`~/.oci` missing). Reboot the Ampere instance from the Oracle console, then SSH and confirm `btq-l1` + `cloudflared` active.
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
- [ ] Public GSM origin: 2026-10-14 VPS `brahm-l1` (`84.12.123.117`) serves `www.brahmnetwork.com` height **49**. Laptop chain is height **67** and must not publish. Task `Brahm-Cloudflared` **Disabled**; Windows service stays Stopped/Manual. Peers still 0. Do not set `BRAH_PUBLIC_LAUNCH=1`.
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
