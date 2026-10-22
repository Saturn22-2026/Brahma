# BTQ → BRM rename inventory (2026-10-22)

**Policy:** Prefer **BRM** / **BRAH** / **Brahma**. Keep **BTQ** acceptance so the live ledger and old phones do not fork. Do **not** rename `btq-node/` or `btq-data/` on disk until a coordinated cutover.

## Classification

| Class | Action | Examples |
| --- | --- | --- |
| **Leave path** | Keep folder names | `btq-node/`, `btq-data/`, `/opt/btq`, `/var/lib/btq`, Linux user `btq` |
| **Alias (done)** | Accept `brm` + keep `btq`/`brah` | RPC `brm_*`→`brah_*`; env `BRM_*`; mesh `BRMHELLO`/`BRMINVITE`/`BRM1|`/`BRMF` |
| **Emit canonical** | Unchanged | Addresses `BRM1G`; messages `BRAH1|`; HELLO `BRAHHELLO`; frames `BRAHF` |
| **Legacy accept** | Keep forever (or until hard fork) | `BTQ1G`, `BTQ1|`, `BTQHELLO`, `BTQINVITE`, `BTQF`, RPC `btq_*`, env `BTQ_*` |
| **Cosmetic / docs** | Prefer Brahma/BRM wording | README, white paper, site — already mostly Brahma |
| **Do not mass-edit** | Live JSON ledgers | `btq-data*/l1_state.json`, chain jsonl, genesis_accounts with `BTQ1G` keys |
| **Deferred** | Optional later cutover | Rename dir to `brm-node`, systemd User=`brm`, package `io.btq` genesis wallet tree |

## Code changes in this pass

- `envutil.py` — `BRAH_` → `BRM_` → `BTQ_`  
- `l1_mainnet.canonical_rpc_method` — `brm_*` and `btq_*` → `brah_*`  
- `crypto_mldsa.py` — verify `BRM1|`, `BRMHELLO`, `BRMINVITE`  
- Wallet mesh — accept `BRMF|`, `BRMHELLO`, `BRMINVITE`  
- `brm-l1.service` — systemd alias unit (same `/opt/btq` paths)

## Still named btq (intentional)

- Directory `btq-node/` (this tree)  
- Service file `btq-l1.service` (keep; `brm-l1.service` is alias)  
- Install scripts referencing `/opt/btq`  
- Historical `burned_btq` state field if present  
- Genesis-wallet Android package `io.btq.wallet` (separate tree; deferred)
