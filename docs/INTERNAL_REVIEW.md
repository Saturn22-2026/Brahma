# Internal review (war-game)

This is an **internal** packet so a real reviewer can start. It is **not** an audit by Trail of Bits, NCC, OpenZeppelin, Spearbit, or any named firm. Do not present it as one.

**Verdict:** private testnet only. `public_launch` remains **false**. Brah has **no market price**. Hardening: signed INVITE, APK flag-only, secrets-on-disk refuse, read RPCs do not create accounts, replica `brah_getBlocks` / SYNC catch-up. Covered by `test_launch_gates.py`.

## Scope a reviewer should use

In: `btq-node/l1_mainnet.py`, `crypto_mldsa.py`, `p2p.py`, `units.py`, Android wallet sign/submit/PoW.

Out of consensus: Solidity, unfinished Rust, deleted mock nodes.

## Gates

1. **One canonical ledger** — Python `btq-node/l1_mainnet.py`.
2. **Spend authorized by signature** — Dilithium3 over `BRAH1|…` (parser still accepts `BTQ1|`). Unsigned submit fails.
3. **Treasury not publicly spendable** — `POOL_*` cannot originate txs.
4. **Integer money** — 1 Brah = 1e8 units. `to_units` always scales Brah amounts (JSON ints included). Load refuses if `units_per_brah` **or** `units_per_btq` is not 1e8.
5. **Client PoW** — `pow_nonce` required. Genesis 20 bits, retarget 18–24. Tests use 8 bits.
6. **Blocks include txs** — RPC and mesh ingest both parse the same signed tx. Mesh ingest is one tx per block; reward only if `kind==mine`, `miner==from`, and `reward==block_reward`. Caps apply.
7. **Authenticated P2P** — HELLO Dilithium over `BRAHHELLO|{node}|{port}|{ts}` ±90s (parser still accepts `BTQHELLO`). INVITE Dilithium over `BRAHINVITE|…`. `brah_getStats.peers` / `mainnet_ready` count **live signed HELLO** only — a `BRAH_PEERS` seed does not count. P2P bind follows `BRAH_BIND`. Peer table capped at 64. Behind peers request SYNC / `brah_getBlocks` (replica catch-up, not finished public consensus).
8. **Genesis keys** — `generate_genesis_keys.py`. Secrets gitignored; RPC never loads them. Node refuses to start if `genesis_secrets.json` sits in `BRAH_DATA_DIR` unless `BRAH_ALLOW_SECRETS_ON_DISK=1`.
9. **Wallet** — EncryptedSharedPreferences; 24 unique words; checksummed `BRM1G` (legacy `BTQ1G` still resolves). Biometric on send/reveal. Share is `https://www.brahmnetwork.com/claim?ref=<checksummed BRM1G>`; unsigned mesh invites are dropped.
10. **TLS + RPC** — optional TLS 1.2+ (`BRAH_REQUIRE_TLS=1` to force); default bind localhost; 256 KiB body; GET+POST rate limit; localhost CORS; explorer and `/wallet.apk` hidden on Cloudflare-forwarded requests even if `BRAH_EXPLORER` / `BRAH_SERVE_APK` are set; unknown paths 404. JSON-RPC Pay rail (except `brah_getFeeSchedule`) requires a partner API key, same as REST `/v1` list/create. Public `www.brahmnetwork.com` uses Cloudflare Universal SSL (reissued ~yearly; not a 2100-dated cert). Origin sends HSTS on HTTPS-forwarded responses and 301s `X-Forwarded-Proto: http` to HTTPS. Daily probe: `python check_public_tls.py` (fails if fewer than 21 days left). Windows task `install_tls_watch_task.ps1`; Linux `install_tls_watch_linux.sh`. Optional `CF_API_TOKEN` keeps Universal SSL, Full (strict), Always HTTPS, and HSTS on.
11. **Docs match code** — this file + root README. `public_launch: false`. Brah has no market price.
12. **Independent audit** — **not done**. Hand a reviewer this file and `python test_launch_gates.py`.

## Consensus bugs closed in this packet

- Negative send cannot inflate supply.
- Client `tx.hash` is ignored; applied set and tx root use the canonical message.
- Checksummed addresses only for `from` / send `to` (no dual-account join).
- Mesh cannot pay an uncapped reward to an unsigned miner string.

## How to re-run

```bash
cd btq-node
python -m pip install -r requirements.txt
python test_launch_gates.py
```

## Still not a public mainnet

Operator disk still owns a single process unless a second **independent machine** completes a live signed HELLO. A `BRAH_PEERS` seed is not that. Same-PC replica catch-up is not a second operator. 20-bit PoW is not a public hashrate market. Phone BouncyCastle vs `dilithium-py` still needs a live signed submit. No named-firm report exists. Do not set `BRAH_PUBLIC_LAUNCH=1`. This is not unbreachable and not beyond the law.
