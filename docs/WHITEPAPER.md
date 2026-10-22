# Brahma Coin (BRAHMNETWORK) — Technical White Paper

**Version:** 1.0-draft  
**Date:** 2026-10-22  
**Status:** Draft for engineering and counsel review. **Not** a MiCA-notified crypto-asset white paper. **Not** an offering prospectus. **Not** investment advice.

**Network name:** BRAHMNETWORK  
**Native asset:** Brahma Coin  
**Public host (when operated):** `https://www.brahmnetwork.com`  
**Canonical ledger:** Python node `btq-node/l1_mainnet.py`

---

## 1. Disclaimer and launch posture

Brahma Coin is described in this repository as a **private post-quantum Layer 1 testnet**. It is **not** Bitcoin, **not** an Ethereum / EVM consensus system, and **not** a public mainnet claim solely because a hostname exists.

`public_launch` is an **explicit operator flag**. It may only read true when protocol gates say the network is ready **and** the operator sets `BRAH_PUBLIC_LAUNCH=1` (or equivalent). Marketing, this paper, and the public site must stay aligned with the **live** `brah_getStats.public_launch` value. A mismatched “private testnet” slogan against a live `true` flag is a compliance defect — see `docs/AUDIT_READINESS.md`.

**Units only.** The node and wallet must not quote a USD or other fiat market price.

**Nothing in this document is an offer to sell securities, an invitation to invest, or a promise of profit.**

---

## 2. Purpose

BRAHMNETWORK exists to let users:

1. Create or recover a Dilithium3 (ML-DSA-65) identity (`BRM1G…` addresses; legacy `BTQ1G…` still resolves to the same key body).  
2. Hold **Brahma Coin** and nine genesis **L2 memecoins** on the same L1 blocks.  
3. **Mine** with phone-grade proof of work and seal over JSON-RPC or Sentinel mesh.  
4. **Claim** a one-time adoption reward (join), optionally with a referrer.  
5. **Send** and **burn** Brahma Coin under protocol rules (including a whale tax on oversized L1 sends).

Non-goals of this phase: exchange listings, dollar oracles, Solidity consensus, unfinished Rust as the ledger of record.

---

## 3. System architecture

| Layer | Implementation |
| --- | --- |
| Consensus / state | Python L1 (`l1_mainnet.py`), integer balances, blocks with txs |
| Signatures | Dilithium3 over canonical `BRAH1|…` messages (parser still accepts `BTQ1|`) |
| RPC | JSON-RPC `brah_*` on HTTP(S); default bind `127.0.0.1:8545` |
| P2P | Sentinel DTN — TCP 18545, UDP discovery; signed HELLO |
| Wallet | Android app `com.brahmnetwork.wallet` — self-custody signing |
| Mesh offline path | Sign + PoW locally; gossip `TRANSACTION`; node seals when heard |
| Public explainer | Same-origin `GET /` from `btq-node/static/` |

Protocol organs (`POOL_*`) cannot originate user spends. Client-supplied tx hashes are ignored; the node hashes the canonical message.

---

## 4. Cryptography and addresses

- **Algorithm:** Dilithium3 (ML-DSA-65).  
- **Message:** `BRAH1|{chain_id}|{nonce}|{kind}|{from}|{to}|{asset}|{amount_units}|{memo}`.  
- **Addresses:** checksummed Bech-style `BRM1G` (33 characters). Same pubkey body under `BTQ1G` shares one balance bucket.  
- **Operator verify:** BouncyCastle **1.72** (3293-byte signatures). BC 1.78.1 is incompatible with current wallet signatures.

Recovery phrases in the app are **not** BIP-39 and are **not** genesis Dilithium hex dumps.

---

## 5. Consensus and proof of work

- The **wallet or client** searches SHA-256 leading zero bits on  
  `{height}|{tip}|{miner}|{reward}|{tx_root}` and submits `pow_nonce`.  
- Genesis target: **~20 bits**; retarget keeps bits in **18–24** toward ~8 second blocks.  
- The node does **not** search nonces for RPC submits.  
- Competing tips at the same height reorg by accumulated work, then hash.  
- Mesh ingest accepts **one** signed tx per block; mine rewards pay only for a valid signed `mine` from the miner under yearly and pool caps.

**This is not a public hashrate market.** 20-bit phone PoW is a testnet-grade work function.

---

## 6. Monetary policy (economics v2 — code-enforced)

**Hard cap:** 100,000,000 Brahma Coin.  
**Base unit:** 1 Brahma = 100,000,000 integer units.

| Pool | Amount | Notes |
| --- | ---: | --- |
| Mining (`POOL_MINING`) | 50,000,000 | 0.00014 / block through 10,000 joins; then halves every 100,000 joins; 5,000,000 / year mine cap |
| Adoption (`POOL_AIRDROP`) | 30,000,000 | Join + referrer schedule |
| 50 distribution wallets | 10,000,000 | 200,000 each |
| Empower first 100 | 3,000,000 | 30,000 each when mining empties |
| Empower next 100 | 2,000,000 | 20,000 each at that moment |
| Strategic burn | 3,000,000 | 100,000 burned on first Friday UTC each month |
| Founder | 2,000,000 | Pays wallet 1 when mining empties |
| Donation | 0 | Removed in economics v2 |

**First mine** on an address: current block reward **+ 1** Brahma (once).

**Whale tax (L1 Brahma only):** if a single send exceeds **10%** of circulating supply, **25%** of that send is burned. L2 transfers are not taxed this way.

**When `POOL_MINING` reaches zero:** remaining operator-grant L2 balances are split once across joined/mined wallets (`l2_mining_empty_released`).

---

## 7. Adoption rewards (join)

- Paid from the adoption pool while it has balance.  
- Tier I (joins 1–10,000): **100** to joiner, **50** to referrer (L1).  
- Then 50/25, then 25/12.5, then halving bands every 25,000 joins.  
- Memo: `dev:<phoneId>[:referrer]` — **one claim per phone** and one per address.  
- Optional: **1,000** of each genesis L2 on successful first join (protocol constant).  
- Node may rate-limit joins (`BRAH_JOIN_MAX_PER_HOUR`, default 20).

Referrer payments are L1 only (not a cash out).

---

## 8. Layer-2 memecoins

- **Nine** genesis tokens on the same L1 (whole token units, not ×1e8).  
- One-time operator grant: **5,000,000,000** of each to the grant address.  
- At each **mine-schedule halving**, a **shared 750,000,000** pool of each genesis L2 is **split** across wallets that joined or mined (not 750M each).  
- **New L2 launches are closed** — no burn-to-launch; `brah_launchL2` / `launch_l2` refuse.

---

## 9. Fees

There is **no gas market**.

Published pay-rail schedule (`brah_getFeeSchedule`):

- `payment_fee_bps`: **0**  
- `refund_fee_bps`: **0**  
- Sealing cost is **client PoW**, not a validator fee.

---

## 10. Wallet and distribution

- Android wallet; self-custody Dilithium keys.  
- Distribution today: **direct APK** (e.g. `https://www.brahmnetwork.com/wallet.apk`), not a Play Store listing in this pack.  
- Share / claim: `https://www.brahmnetwork.com/claim?ref=<checksummed BRM1G>`.  
- Password gates send, burn, lock-on-open, extra wallets, mnemonic reveal when configured.

---

## 11. Network launch gates

Before any honest **mainnet** claim, this project’s own gates require:

1. **Two independent operators** (separate humans and machines — not two processes on one PC).  
2. **Named public host** actually reaching the node origin.  
3. **Explicit launch flip** (`BRAH_PUBLIC_LAUNCH=1` only when `mainnet_ready` is true).

`mainnet_ready` requires ≥1 live signed P2P peer **and** named HTTPS RPC (not trycloudflare).

Internal engineering review: `docs/INTERNAL_REVIEW.md` (not a named-firm audit).

---

## 12. Risks (non-exhaustive)

- Operator disk / single-process control until true multi-operator consensus.  
- Testnet-grade PoW; chain can be reorganized under the stated rules.  
- Regulatory classification of rewards, referrals, and public distribution is **unsettled** and jurisdiction-specific — see `docs/COMPLIANCE_GAP_PACK.md` and `docs/AUDIT_READINESS.md`.  
- Sideload APKs carry malware and update risks vs store distribution.  
- No deposit insurance; no redemption for fiat.

---

## 13. Privacy

See `docs/PRIVACY_POLICY.md` and the public page `/static/privacy.html`. Join memos may include a device-bound `phoneId` for anti-farm enforcement.

---

## 14. Document control

| Field | Value |
| --- | --- |
| Title | Brahma Coin (BRAHMNETWORK) Technical White Paper |
| Classification | Draft / not notified |
| Canonical economics | Economics v2 as enforced by node code + README |
| Related | `README.md`, `docs/INTERNAL_REVIEW.md`, `docs/AUDIT_READINESS.md` |

Counsel must review before any EU public-offer notification, US offering, or SA FAIS/FIC registration claim. **Publishing this file does not complete MiCA Article 8 notification.**
