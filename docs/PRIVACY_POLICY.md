# Privacy Policy — Brahma Coin / BRAHMNETWORK

**Effective date:** 2026-10-22  
**Public HTML:** `/static/privacy.html` on the node host  
**Status:** Operator draft. Update the controller identity before relying on this in a regulated filing.

## 1. Who we are

The **operator** of a BRAHMNETWORK node and of `www.brahmnetwork.com` (when that host is live) processes limited technical data to run the testnet site, RPC, and optional wallet download.

This policy does **not** mean a licensed financial institution is offering regulated services.

## 2. What we process

Depending on how you use the network, we may process:

| Data | Purpose | Legal basis (EU/SA framing — counsel to confirm) |
| --- | --- | --- |
| IP address, User-Agent, TLS metadata | Security, rate limits, abuse prevention, Cloudflare edge | Legitimate interests / security |
| RPC request bodies you send | Execute ledger methods you request | Contract / requested service |
| Join memo `dev:<phoneId>[:referrer]` | One-claim-per-phone anti-farm; optional referrer bind | Legitimate interests / fraud prevention |
| Wallet addresses you publish or use | Ledger state (public by design on a blockchain) | Blockchain publicity / requested tx |
| APK download logs | Serve and debug distribution | Legitimate interests |

We do **not** ask for government ID in the open-source wallet. There is **no KYC product** in this repository.

## 3. Blockchain publicity

On-chain data (addresses, amounts, memos, blocks) is **public and immutable** once sealed. Do not put secrets, names, or personal identifiers in memos.

## 4. Sharing

- **Cloudflare** (or similar CDN/tunnel) may process traffic for `www.brahmnetwork.com`.  
- **No sale** of personal information for advertising is intended by this project’s default configuration.  
- Authorities may compel disclosure where law requires.

## 5. Retention

Edge and server logs: retained only as needed for security and operations (operator should set a concrete window, e.g. 30–90 days).  
Ledger data: retained for the life of the chain.

## 6. Your choices

- Do not join / do not use the public RPC.  
- Run your own node.  
- Avoid putting personal data in memos.  
- Uninstall the sideloaded APK to remove local wallet data (on-chain history remains).

## 7. International transfers

If the operator or CDN is outside your country, traffic and logs may cross borders. Operators targeting the EU or South Africa should complete transfer assessments (GDPR / POPIA) with counsel.

## 8. Children

Not directed at children under 18 (or higher age required locally).

## 9. Contact

Replace with a real operator contact email before production reliance: `privacy@[operator-domain]`.

## 10. Changes

Material changes will be reflected in this file and `/static/privacy.html` with an updated effective date.
