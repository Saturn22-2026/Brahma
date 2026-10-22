# Audit readiness — close-out of eight open conditions

**Date:** 2026-10-22  
**Purpose:** Track what was closed in-repo vs what still requires counsel, licences, or product work to move toward a regulated-grade crypto financial tool.  
**Not** a pass certificate.

Companion: `docs/WHITEPAPER.md`, `docs/PRIVACY_POLICY.md`, `docs/COMPLIANCE_GAP_PACK.md`, canvas `compliance-gap-pack`.

---

## Scoreboard (the eight)

| # | Condition | Was | Now (in-repo) | Residual (outside git) |
| --- | --- | --- | --- | --- |
| 1 | No offering / MiCA-style white paper | Open | **Draft closed** — `docs/WHITEPAPER.md` + `/static/whitepaper.html` | Counsel must adapt + notify for any EU public offer |
| 2 | No GDPR / privacy notice | Open | **Draft closed** — `docs/PRIVACY_POLICY.md` + `/static/privacy.html` + site footer | Controller identity, DPIA, retention ops |
| 3 | No POPIA programme docs | Open | **Partial closed** — privacy pack covers POPIA themes | SA operator registration / PAIA manuals as needed |
| 4 | No AML / KYC / Travel Rule | Open | **Scaffold only** — §A below + non-goals | Licensed programme, vendor, monitoring |
| 5 | No OFAC / sanctions screening | Open | **Scaffold only** — §B below | List feeds, blocking policy, US nexus ops |
| 6 | `public_launch` vs “private testnet” inconsistency | Open | **Policy closed** — §C single source of truth | Operator must align live VPS env with policy |
| 7 | Marketing / reward CTAs vs disclaimers | Partial | **Tightened** — footer links + white paper risk language | Counsel review of reward UX before regulated markets |
| 8 | CASP / MSB / FAIS perimeter unclear | Counsel call | **Memo closed** — §D activity classification | Licences if classification says “yes” |
| 9 | Mainland China (PRC) market access | Unscoped | **Policy closed** — §H out-of-market | Geo/ToS/edge blocks + counsel on PRC nexus; **no** PRC CASP path |

---

## A. AML / KYC / Travel Rule — what “ultimate” still needs

To operate as a **client-facing CASP / money transmitter** (not merely publish self-custody software), typical requirements include:

1. Board-approved AML/CFT policy and risk assessment  
2. Customer due diligence (KYC) with verified identity  
3. Ongoing monitoring and suspicious activity reporting  
4. Travel Rule information exchange on crypto transfers (e.g. FATF R.16 / SA FIC Directive 9 when applicable)  
5. Independent AML audit cycle  

**This repository does not implement (1)–(5).** Self-custody + public ledger ≠ a licensed remittance product.

**In-repo stance:** default product is **self-custody testnet software**. Partner pay-rail remains API-key gated and fee_bps=0. No KYC UI.

---

## B. Sanctions (OFAC and peers) — scaffold

**Policy stub for operators with a US nexus:**

- Do not onboard or facilitate known sanctioned persons/wallets once a screening stack exists.  
- Document who runs the public RPC and APK host.  
- Until a screening vendor is integrated, treat public open RPC as **high residual risk** for any US person operator.

**Code hook (future):** optional deny-list check before `brah_submitTx` / join — not shipped in this pass.

---

## C. Launch truth — single source of truth

| Layer | Rule |
| --- | --- |
| Genesis / docs marketing | Default story: **private testnet** until gates + explicit flip |
| Live truth | `brah_getStats.public_launch` on the **public origin** |
| Operator duty | If marketing says private testnet, **do not** leave `BRAH_PUBLIC_LAUNCH=1` on the public VPS unless gates are truly met **and** counsel agrees the marketing may say launched |
| Site | Gate chip already binds to live stats — do not hardcode “FALSE” in operator runbooks while the flag is true |

**Close-out action for operators:** SSH/public stats check; unset `BRAH_PUBLIC_LAUNCH` or update all public copy to match. Memory bank must record the chosen truth after ops confirm.

---

## D. Activity classification memo (perimeter)

| Activity | In this repo? | Typical question for counsel |
| --- | --- | --- |
| Publish open-source node + wallet | Yes | Software publication vs regulated service |
| Host public RPC / site / APK | Operator choice | Hosting / distribution / possible CASP facts |
| Pay users join/referral rewards | Yes (protocol) | Public offer / promotion / FAIS intermediary |
| Hold customer keys | No (self-custody) | Usually not custody CASP if true |
| Exchange crypto↔fiat | No | N/A unless added |
| Partner invoice rail | Optional API | MSB / payment institution facts |

**Non-goal until licensed:** “Ultimate all-in-one regulated exchange + bank + CASP.” That is a **company and licence** programme, not a markdown close-out.

---

## E. Path to a regulated-grade financial tool (ordered)

1. **Align launch flag + marketing** (ops, days)  
2. **Exclude Mainland China** — ToS + geo/IP (and app distribution) blocks; counsel on any PRC nexus (§H)  
3. **Counsel opinions** — SA / EU / US as needed (weeks–months)  
4. **Privacy ops** — real controller contact, retention, CDN DPA  
5. **If offering in EU** — finalize white paper Annex content + notification  
6. **If CASP/MSB/FSP** — licence applications, capital, audits  
7. **If transferring for clients** — KYC + Travel Rule vendor  
8. **Named security firm** — beyond `INTERNAL_REVIEW.md`  
9. **Store distribution** — Play/App Store policies if leaving sideload  

---

## H. China (PRC) — out of market

**Regime:** prohibition, not MiCA-style authorisation. Feb 2026 eight-department notice restates: onshore virtual-currency business is illegal financial activity; mining continues to be rectified; offshore parties must not illegally serve 境内 subjects; unapproved offshore issuance by PRC / PRC-controlled entities is restricted.

**In-repo stance:** Brahma does **not** target Mainland China. There is **no** plan to apply for a PRC crypto licence (none exists for this activity class).

**Residuals (ops / counsel, outside git):**

| Control | Status |
| --- | --- |
| Site / ToS “not available in Mainland China” | Not shipped |
| Edge geo/IP block for CN | Not shipped |
| Avoid Chinese-language mine/join marketing aimed at 境内 | Operator duty |
| Counsel memo if any founder, employee, server, or funding has PRC nexus | Open |

**Do not** treat draft white paper / privacy pages as PRC compliance.

---

## F. Engineering audit checklist (runnable now)

```bash
cd btq-node
python test_launch_gates.py
python check_public_tls.py   # if public host live
```

Also review: Dilithium BC 1.72, `docs/INTERNAL_REVIEW.md` gates, fee schedule 0 bps, L2 launches closed, economics v2 pools.

---

## G. Document index

| Doc | Role |
| --- | --- |
| `docs/WHITEPAPER.md` | Technical white paper draft |
| `docs/PRIVACY_POLICY.md` | Privacy draft |
| `docs/COMPLIANCE_GAP_PACK.md` | Regional gap matrix (EU · US · SA · **PRC**) |
| `docs/INTERNAL_REVIEW.md` | Security war-game (not firm audit) |
| `/static/whitepaper.html` | Public summary |
| `/static/privacy.html` | Public privacy page |
