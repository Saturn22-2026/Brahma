# Sovereign Identity Map: Bitcoin-Quantum (BTQ) v5

This artifact contains the definitive list of strategic addresses for the Bitcoin-Quantum ecosystem.

## 🛡️ L1 Sovereign Core Wallets
These wallets manage the root reserves of the protocol.

| Wallet Role | Public Address | Status |
| :--- | :--- | :--- |
| **Wealth Reserve** | `0x98a1961A67a42F80735E2D075677b7F68748366c` | Sovereign (External) |
| **Empowerment AI** | `0xA31c890737920A939E4366C34E3Eee75Af79A744` | AI Council Managed |
| **Strategic Stability** | `0x5DF596fc9aBA769926C1eE3f98181BBf3b7A54C4` | Sovereign (External) |
| **Donation Reserve** | `0x0b93082D9b3C7C97fAcd250082899BAcf3af3885` | Time-Locked (2 Years) |
| **Airdrop Source** | `0x19a00be544D1e065F257A9230172779A74101605` | AI Council Managed |
| **Founder Wallet** | `0x7FA9385bE102ac3EAc297483Dd6233D62b3e1496` | Sovereign (External) |

---

## 🏛️ AI Council Managed Wallets (Encrypted)
The following addresses are managed by the **8-Member AI Council**. Their private keys are stored in the AES-256-GCM encrypted `ai_wallets.json` vault.

- **Council Treasury**: Derived at runtime via the **Master Sovereign Key**.
- **Specialized Agent Keys**: Dynamically expanded from the master entropy.

---

## 📈 L2 Multi-Asset Wallets
Strategic wallets for the 8 parallel memecoin economies.

| Asset | Treasury/Wealth Address | Airdrop Contract |
| :--- | :--- | :--- |
| **HOMIE** | `0x5FfD7Fc9C3231c8E525F9d57e6220a4FCBc650F3` | `0xf7f37745d4Cf1675d500322ba6Fa41E61D533266` |
| **POOKIE** | `0xD772fc9aE6E08b47BC42ec54888a54B03fBE542a` | `0x84F2292cCF9032F874e74a96107e3147C04BD695` |
| **SLUMDOG** | `0xc64732E4dD8Ec498a73920f1dCC3B0262B022a13` | `0xBAbA5B86fbA810E245e14Bbd00BC14482f323894` |
| **CRAZY** | `0x7F200C0E3b4E55372BeC04C2076c2cA60BcC1f7D` | `0x5edbC9dBC4DbbF4ed402669554186E87514e7fe9` |
| **BOUJIE** | `0xccd1c7E19f55432bf7B3F0591EB40c900De0a828` | `0x272070d3FbdF5Beaf027648E4d33241fED0c45b8` |
| **QMILE** | `0xe5247fDDe0891A4BC6a4Ea5b872Eb616Bfd85775` | `0xA72c4aA719C1F80478C9A7B5f104FE2Bc804b597` |
| **SOF** | `0xE8C91088A77335E39E7913E070Ed97C5397f692D` | `0x0Eaa759Db820c20873051Ed79D8a38D9E5B26A5b` |
| **5AVE** | `0x3D2B19bEBa4EAd441C58226B3C4E57132E8c43f5` | `0x3e62fAFDC69535a3E7B88fc442C9fbc47b2b3caa` |

---

## 👥 Initial 50 Citizen Wallets (Placeholders)
The first 10,000,000 BTQ are distributed across 50 placeholder addresses for initial network stability.

- **Range**: `0xdEad000000000000000000000000000000000000` to `0xdEad000000000000000000000000000000000031`.
- **Amounts**: Fixed as per v5 distribution table (95 BTQ to 2.58M BTQ).

---

## 🔑 Accessing Pass Keys (Private Keys)

> [!CAUTION]
> **Sovereign Principle**: As an AI assistant, I do not store or generate private keys for the **L1 Foundation Wallets** (Wealth, Strategic, Founder). These must be managed via your hardware wallet or native PQC generator using your **Master Sovereign Key**.

> [!IMPORTANT]
> **AI Managed Keys**: The private keys for the AI Agent and Council distributions are locked in the **AES-256 encrypted vault** (`ai_wallets.json`). To access them, you must provide the **Master Sovereign Key** to the `EncryptedStorage` utility.

**The future is quantum. The keys are yours.**
