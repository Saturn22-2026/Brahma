# Brahma Coin — scoped compliance gap pack

**Status:** counsel preparation only.  
**Not** legal advice. **Not** a licensed audit. **Not** coverage of every jurisdiction on Earth.

**Scope (refreshed 2026-10-22):**

| Dimension | Coverage |
| --- | --- |
| Regions | European Union · United States · South Africa · **China (PRC)** |
| Angles | Securities / public offering · Money transmission / CASP · Sanctions / AML · Consumer ads / marketing · Privacy / data · **PRC prohibition / mining / offshore→境内** |
| Artifacts reviewed | README · `docs/INTERNAL_REVIEW.md` · `docs/AUDIT_READINESS.md` · `btq-node/static/` · `brahm_rail.py` · wallet sideload · genesis/supply · memory-bank launch notes |

Interactive matrix: Cursor canvas `compliance-gap-pack.canvas.tsx`.

---

## Executive summary

Brahma Coin / BRAHMNETWORK ships **units-only** display and **no USD price**. Draft white paper and privacy pages exist. That does **not** equal licences.

The public surface also:

- Serves named HTTPS + Android APK  
- Pays **join / referral** rewards and L2 airdrops  
- Documents large **pre-allocated** pools  
- Offers a **partner payment rail**  
- Uses **client PoW mining**  

**US / EU / SA:** open counsel questions (Howey, MSB/OFAC, MiCA CASP/offer, FAIS/FIC). AML/KYC/Travel Rule/OFAC remain **scaffolds only**.

**China (PRC):** not a licence-gap market. Onshore virtual-currency business and mining remain **illegal financial activity** under the multi-year ban, restated in the Feb 2026 eight-department notice. Offshore providers must not illegally serve 境内 subjects. Treat Mainland China as **out of market** until counsel says otherwise — geo-exclusion is the control, not a CASP application.

See `docs/AUDIT_READINESS.md` for the scoreboard (including PRC residual).

---

## Project facts counsel should rely on

1. **Ledger:** Python `btq-node/l1_mainnet.py`; Dilithium3 spends; client PoW (~20 bit).  
2. **Public surface:** `https://www.brahmnetwork.com` — home, claim, RPC.  
3. **Launch posture:** Live origin may have `BRAH_PUBLIC_LAUNCH=1` (`public_launch` true). Marketing must match live `brah_getStats.public_launch` — see `AUDIT_READINESS` §C.  
4. **Supply (economics v2):** 100M — mining 50M, adoption 30M, 50×200k dist, empower 3M+2M, strategic 3M, founder 2M, donation 0.  
5. **Adoption:** join tiers + referrer half; memo `dev:<phoneId>[:referrer]`; one claim per phone; **1,000 of each genesis L2** on join.  
6. **Fees:** `payment_fee_bps=0`, `refund_fee_bps=0`; sealing is PoW, not gas.  
7. **Wallet:** Android sideload APK; self-custody; not a Play Store listing in-repo.  
8. **Security packet:** `docs/INTERNAL_REVIEW.md` — not a named-firm audit.  
9. **Privacy:** draft `docs/PRIVACY_POLICY.md` + `/static/privacy.html` (controller contact still ops work).  
10. **L2:** nine genesis tokens; launches closed; 750M shared halving airdrop; mining-empty L2 release.  
11. **PRC-sensitive facts:** public mine UX; open HTTPS/APK/RPC with no Mainland China geo-block documented in-repo.

---

## Artifact checklist

| Artifact | Status | Gap |
| --- | --- | --- |
| MiCA / offering white paper | **Draft** | Still needs counsel adapt + NCA notification if EU public offer |
| US PPM / Reg D / Reg S pack | Missing | If US offer/sale |
| Privacy policy (GDPR/POPIA/CCPA) | **Draft** | Controller, DPIA, retention ops |
| AML / KYC / OFAC / Travel Rule programme | Scaffold | If CASP / US nexus / SA accountable institution |
| FSCA FSP / FIC registration evidence | Missing | If SA advice/intermediary/CASP |
| PRC geo-exclusion / ToS block | **Missing** | Required if any risk of 境内 users |
| Play / App Store listing + disclosures | N/A (sideload) | Sideload ≠ compliance free pass |
| Internal security review | Present | Engineering only |
| Site disclaimers (testnet, no USD, not advice) | Present | Helpful; incomplete alone |

---

## Gap matrix (abbreviated)

### European Union (2026 refresh)

MiCA CASP rules apply; national transitional/grandfathering windows were time-limited after Dec 2024 full application — do **not** assume “still under national crypto law” without checking the home Member State.

| Angle | Framework cue | Posture | Priority |
| --- | --- | --- | --- |
| Offering | MiCA public offer + white paper **notification** | Partial — draft WP, **no** NCA notification pack | P0 |
| Marketing | MiCA fair, clear, not misleading | Partial — disclaimers + reward/L2 CTAs | P0 |
| CASP | Art. 59+ authorisation (EU entity, capital, AML, complaints…) | Counsel call — self-custody vs hosted RPC/APK/rail | P0 |
| Privacy | GDPR | Partial — draft notice; controller/DPIA open | P1 |
| AML | Travel Rule / CASP AML | Open if CASP | P1 |

### United States (2026 refresh)

| Angle | Framework cue | Posture | Priority |
| --- | --- | --- | --- |
| Securities | Howey / Securities Act | Counsel call — join/referral/L2 airdrop/dist facts | P0 |
| Money transmission | FinCEN MSB / state MTL | Counsel call — pay rail + hosted infra | P0 |
| Sanctions | OFAC (digital currency = same duties) | Open — no screening | P0 |
| Ads | FTC / anti-fraud | Partial — restrained claims + reward promo; launch-flag consistency | P1 |
| Privacy | State privacy acts | Partial — draft notice | P1 |
| Stablecoins | GENIUS Act / PPSI AML (proposed 2026) | N/A — no USD/RMB payment stablecoin issued here | — |

### South Africa

| Angle | Framework cue | Posture | Priority |
| --- | --- | --- | --- |
| Financial services | FAIS crypto-as-financial-product (FSCA) | Counsel call | P0 |
| AML | FIC Act CASP; Directive 9 Travel Rule | Open if accountable institution | P0 |
| Marketing | FAIS General Code | Partial | P1 |
| Privacy | POPIA | Partial — draft themes; programme docs open | P1 |

### China (PRC) — prohibition regime

**Cue documents (orientation only):** 2013 Bitcoin risk notice; 2021 trading/speculation notice; 2021 mining rectification; **Feb 2026** PBOC + seven other departments *Notice on Further Preventing and Resolving Risks Related to Virtual Currencies* (and official Q&A): onshore VC business = illegal financial activity; offshore parties must not illegally serve 境内 subjects; mining continues to be shut down; unapproved offshore VC issuance by PRC / PRC-controlled entities restricted; unapproved RMB-pegged stablecoins banned.

| Angle | Framework cue | Posture | Priority |
| --- | --- | --- | --- |
| Onshore VC business | Illegal financial activity — no CASP licence path | **Out of market** — cannot “comply into” a PRC crypto exchange/CASP | P0 |
| Mining | NDRC / industrial-policy mining ban | Open if any onshore mine / miner sales / data-centre mining disguise | P0 |
| Offshore → 境内 service | Ban on illegally providing VC services to Mainland subjects | Open — public site/APK/RPC/join with **no** geo-exclusion in-repo | P0 |
| Offshore issuance | PRC person / controlled offshore issuer without approval | Counsel call if founders/funding/control have PRC nexus | P0 |
| Marketing / ads | Platform + ad controls; risk education push | Open if Chinese-language or PRC-targeted promo | P1 |
| RWA tokenization | 2026 notice restricts unapproved RWA / mid-office IT for 境内 | N/A today (no PRC-asset tokenization) — watch if product pivots | P2 |
| Privacy | PIPL | Secondary if 境内 personal data processed despite ban | P2 |

**Operator control (docs-level, not a licence):** treat Mainland China as excluded; add ToS + edge geo/IP blocks; do not market mine/join to 境内 users; counsel if any team, server, or funding is PRC-linked.

### Cross-cutting

| Issue | Posture | Priority |
| --- | --- | --- |
| `public_launch` vs marketing consistency | Policy closed in docs; **ops** must align live VPS | P0 |
| No named-firm audit | Partial | P2 |
| No Mainland China geo-exclusion | Open | P0 |

---

## Recommended engagement order

1. **China (PRC):** confirm out-of-market + geo/ToS exclusion; counsel on any PRC nexus (people, capital, servers). Do not plan a PRC CASP application.  
2. **South Africa** (if founders, servers, or primary users are SA-linked): FSCA / FIC / POPIA.  
3. **EU:** MiCA offer vs exemption; CASP perimeter; GDPR ops.  
4. **US:** Howey + MSB facts; OFAC programme for any US person nexus.

## What to hand counsel

1. This file  
2. `docs/AUDIT_READINESS.md`  
3. Root `README.md`  
4. `docs/INTERNAL_REVIEW.md`  
5. `docs/WHITEPAPER.md` + `docs/PRIVACY_POLICY.md`  
6. `btq-node/static/index.html` + `site.js`  
7. `btq-node/genesis.json` economics section  
8. `btq-node/brahm_rail.py` (`FEE_SCHEDULE`)  
9. Wallet share/APK URLs and version  
10. Who operates `www.brahmnetwork.com`, live `BRAH_PUBLIC_LAUNCH`, and any PRC/US/EU/SA nexus statement  

---

## Explicit non-claims

- This document does **not** conclude that Brahma is or is not a security, CASP, MSB, or FAIS product.  
- This document does **not** replace registration, licensing, or a formal legal opinion.  
- For the PRC, “gap” means **exclusion / nexus risk**, not a missing application form.  
- “AI audit of every government” remains impossible; this is the scoped substitute.
