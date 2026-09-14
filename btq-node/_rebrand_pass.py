"""One-shot BRAH rebrand helper. Run from repo; delete after."""
from __future__ import annotations

from pathlib import Path
import shutil

REPO = Path(__file__).resolve().parent.parent
NODE = REPO / "btq-node"
WALLET_SRC = REPO / "wallet-android" / "app" / "src"


def patch_l1() -> None:
    p = NODE / "l1_mainnet.py"
    t = p.read_text(encoding="utf-8")
    t = t.replace(
        "from urllib.parse import parse_qs, urlparse\n",
        "from urllib.parse import parse_qs, urlparse\n\nfrom envutil import env\n",
    )
    t = t.replace(
        """from ai_council import (
    FIRST_MINE_BONUS_BTQ,
    L2_BOOTSTRAP_IDS,
    L2_HALVING_AIRDROP,
    L2_OPERATOR_GRANT,
    L2_OPERATOR_GRANT_ADDR,
    block_reward_btq,
    donations_unlocked,
    mine_halving_band,
    reward_rates,
    whale_tax_units,
)
from crypto_mldsa import (
    ADDR_PREFIXES,
    addresses_equal,
    canonical_message,
    is_btq_address,
    load_or_create_identity,
    normalize_address,
    sibling_address,
    verify_invite,
    verify_spend,
)
from generate_genesis_keys import accounts_path
from p2p import PeerNet
from units import UNITS_PER_BTQ, from_units, from_units_float, to_units
""",
        """from ai_council import (
    FIRST_MINE_BONUS,
    L2_BOOTSTRAP_IDS,
    L2_HALVING_AIRDROP,
    L2_OPERATOR_GRANT,
    L2_OPERATOR_GRANT_ADDR,
    block_reward,
    donations_unlocked,
    mine_halving_band,
    reward_rates,
    whale_tax_units,
)
from crypto_mldsa import (
    ADDR_PREFIXES,
    addresses_equal,
    canonical_message,
    checksum_address,
    is_brah_address,
    load_or_create_identity,
    matching_canonical_message,
    normalize_address,
    sibling_address,
    verify_invite,
    verify_spend,
)
from generate_genesis_keys import accounts_path
from p2p import PeerNet
from units import UNITS_PER_BRAH, from_units, from_units_float, to_units
""",
    )
    t = t.replace(
        'DATA = Path(os.environ.get("BTQ_DATA_DIR", str(ROOT)))\n'
        "STATE_PATH = DATA / \"l1_state.json\"\n"
        "CHAIN_PATH = DATA / \"l1_chain.jsonl\"\n\n\n"
        "def refresh_paths() -> None:\n"
        "    global DATA, STATE_PATH, CHAIN_PATH\n"
        '    DATA = Path(os.environ.get("BTQ_DATA_DIR", str(ROOT)))\n'
        '    if os.environ.get("BTQ_DATA_DIR"):\n'
        "        DATA.mkdir(parents=True, exist_ok=True)\n",
        'DATA = Path(env("DATA_DIR") or str(ROOT))\n'
        "STATE_PATH = DATA / \"l1_state.json\"\n"
        "CHAIN_PATH = DATA / \"l1_chain.jsonl\"\n\n\n"
        "def refresh_paths() -> None:\n"
        "    global DATA, STATE_PATH, CHAIN_PATH\n"
        '    DATA = Path(env("DATA_DIR") or str(ROOT))\n'
        '    if env("DATA_DIR"):\n'
        "        DATA.mkdir(parents=True, exist_ok=True)\n",
    )
    swaps = [
        ('os.environ.get("BTQ_RPC_PORT", "8545")', 'env("RPC_PORT", "8545")'),
        ('raise RuntimeError("BTQ_RPC_PORT must be an integer")', 'raise RuntimeError("BRAH_RPC_PORT must be an integer")'),
        ('raise RuntimeError("BTQ_RPC_PORT must be in 1..65535")', 'raise RuntimeError("BRAH_RPC_PORT must be in 1..65535")'),
        ('"""Write BTQ_PUBLIC_RPC to data/public_rpc.txt when set. Else read the file."""',
         '"""Write BRAH_PUBLIC_RPC (or legacy BTQ_PUBLIC_RPC) to data/public_rpc.txt when set."""'),
        ('url = os.environ.get("BTQ_PUBLIC_RPC", "").strip()', 'url = (env("PUBLIC_RPC") or "").strip()'),
        ('"BTQ_PUBLIC_RPC must be an https hostname that forwards to this node, "',
         '"BRAH_PUBLIC_RPC must be an https hostname that forwards to this node, "'),
        ('url = os.environ.get("BTQ_PUBLIC_RPC", "").strip().rstrip("/")',
         'url = (env("PUBLIC_RPC") or "").strip().rstrip("/")'),
        ('extra = os.environ.get("BTQ_CORS_ORIGIN", "").strip().rstrip("/")',
         'extra = (env("CORS_ORIGIN") or "").strip().rstrip("/")'),
        ('return os.environ.get("BTQ_EXPLORER") == "1"', 'return env("EXPLORER") == "1"'),
        ('return os.environ.get("BTQ_SERVE_APK") == "1"', 'return env("SERVE_APK") == "1"'),
        ("is_btq_address", "is_brah_address"),
        ("UNITS_PER_BTQ", "UNITS_PER_BRAH"),
        ("btq_outstanding_units", "brah_outstanding_units"),
        ("block_reward_btq", "block_reward"),
        ("FIRST_MINE_BONUS_BTQ", "FIRST_MINE_BONUS"),
        ('launched = os.environ.get("BTQ_PUBLIC_LAUNCH") == "1" and ready',
         'launched = env("PUBLIC_LAUNCH") == "1" and ready'),
        ('return max(1, int(os.environ.get("BTQ_JOIN_MAX_PER_HOUR", "20")))',
         'return max(1, int(env("JOIN_MAX_PER_HOUR", "20") or "20"))'),
        ('bind = os.environ.get("BTQ_BIND", "127.0.0.1")', 'bind = env("BIND", "127.0.0.1") or "127.0.0.1"'),
        ('if not local and os.environ.get("BTQ_ALLOW_LAN") != "1":',
         'if not local and env("ALLOW_LAN") != "1":'),
        ('"Set BTQ_ALLOW_LAN=1 and BTQ_BIND=0.0.0.0 for phone LAN tests."',
         '"Set BRAH_ALLOW_LAN=1 and BRAH_BIND=0.0.0.0 for phone LAN tests."'),
        ('if os.environ.get("BTQ_REQUIRE_TLS") == "1":', 'if env("REQUIRE_TLS") == "1":'),
        ('if not (os.environ.get("BTQ_TLS_CERT") and os.environ.get("BTQ_TLS_KEY")):',
         'if not (env("TLS_CERT") and env("TLS_KEY")):'),
        ('raise RuntimeError("BTQ_REQUIRE_TLS=1 requires BTQ_TLS_CERT and BTQ_TLS_KEY")',
         'raise RuntimeError("BRAH_REQUIRE_TLS=1 requires BRAH_TLS_CERT and BRAH_TLS_KEY")'),
        ('if secrets.exists() and os.environ.get("BTQ_ALLOW_SECRETS_ON_DISK") != "1":',
         'if secrets.exists() and env("ALLOW_SECRETS_ON_DISK") != "1":'),
        ('"Move operator secrets offline. Local-only exception: BTQ_ALLOW_SECRETS_ON_DISK=1."',
         '"Move operator secrets offline. Local-only exception: BRAH_ALLOW_SECRETS_ON_DISK=1."'),
        ('p2p_port = int(os.environ.get("BTQ_P2P_PORT", "18545"))',
         'p2p_port = int(env("P2P_PORT", "18545") or "18545")'),
        ('seeds = [s for s in os.environ.get("BTQ_PEERS", "").split(",") if s.strip()]',
         'seeds = [s for s in (env("PEERS") or "").split(",") if s.strip()]'),
        ('udp_port = int(os.environ.get("BTQ_UDP_PORT", str(p2p_port + 2)))',
         'udp_port = int(env("UDP_PORT") or str(p2p_port + 2))'),
        ('f"btq-l1-{p2p_port}"', 'f"brah-l1-{p2p_port}"'),
        ('cert = os.environ.get("BTQ_TLS_CERT", "")', 'cert = env("TLS_CERT") or ""'),
        ('key = os.environ.get("BTQ_TLS_KEY", "")', 'key = env("TLS_KEY") or ""'),
        ('filename="btq-wallet.apk"', 'filename="brahma-wallet.apk"'),
        ('"APK download disabled (set BTQ_SERVE_APK=1)"',
         '"APK download disabled (set BRAH_SERVE_APK=1)"'),
        ('"explorer is localhost-only (set BTQ_EXPLORER=1)"',
         '"explorer is localhost-only (set BRAH_EXPLORER=1)"'),
        ('"<code>BTQ_SERVE_APK=1</code>."', '"<code>BRAH_SERVE_APK=1</code>."'),
        ('sha256_hex("BTQ-L1-GENESIS")', 'sha256_hex("BRAHMNETWORK-L1-GENESIS")'),
        ('if os.environ.get("BTQ_ALLOW_LAN") == "1" and parsed is not None:',
         'if env("ALLOW_LAN") == "1" and parsed is not None:'),
        ('"from must be a checksummed BRM1G or BTQ1G address"',
         '"from must be a checksummed BRM1G address"'),
        ('"to must be a checksummed BRM1G or BTQ1G address"',
         '"to must be a checksummed BRM1G address"'),
        ('"Verified referrer (checksummed BRM1G or BTQ1G): "',
         '"Verified referrer (checksummed BRM1G): "'),
        ('f\'<a href="btq://claim?ref={html.escape(addr)}">btq:// claim</a></p>\'',
         "\"</p>\""),
        ('if asset.lower() in ("latest", "btq", "brahma", "brahma coin"):',
         'if asset.lower() in ("latest", "brah", "brahma", "brahma coin", "0"):'),
        ('if asset.lower() in ("latest", "btq", "brahma", "brahma coin", "0"):',
         'if asset.lower() in ("latest", "brah", "brahma", "brahma coin", "0"):'),
        ('"Legacy state refused (units_per_btq != 1e8). "',
         '"Legacy state refused (units scale != 1e8). "'),
        ('"Set BTQ_DATA_DIR to a new directory and generate genesis keys."',
         '"Set BRAH_DATA_DIR to a new directory and generate genesis keys."'),
        ('join_btq, ref_btq = reward_rates(int(state.get("joined_count", 0)))',
         'join_amt_coins, ref_amt_coins = reward_rates(int(state.get("joined_count", 0)))'),
        ('join_amt = to_units(join_btq)', 'join_amt = to_units(join_amt_coins)'),
        ('ref_amt = to_units(ref_btq)', 'ref_amt = to_units(ref_amt_coins)'),
        ('for name, amount_btq in genesis["treasury"].items():',
         'for name, amount_coins in genesis["treasury"].items():'),
        ('acct["0"] = to_units(amount_btq)', 'acct["0"] = to_units(amount_coins)'),
        ('l2_launch_burn_btq', 'l2_launch_burn'),
        ('"burn_btq"', '"burn"'),
        ('"l2_launch_burn_btq"', '"l2_launch_burn"'),
        ('"units_per_btq"', '"units_per_brah"'),
        ('"burned_btq"', '"burned_brah"'),
        ('int(state.get("burned_btq", 0) or 0)', 'int(state.get("burned_brah") or state.get("burned_btq") or 0)'),
        ('int(state.get("burned_brah", 0))', 'int(state.get("burned_brah") or state.get("burned_btq") or 0)'),
    ]
    for a, b in swaps:
        t = t.replace(a, b)

    t = t.replace(
        'SUBMIT_METHODS = frozenset({\n'
        '    "btq_submitTx",\n'
        '    "btq_sendTransaction",\n'
        '    "eth_sendRawTransaction",\n'
        '    "btq_requestFaucet",\n'
        '    "btq_faucet",\n'
        '    "btq_joinRewards",\n'
        '    "btq_mine",\n'
        '    "btq_councilDonate",\n'
        '    "btq_launchL2",\n'
        '    "btq_launchMemecoin",\n'
        '})\n',
        '''def canonical_rpc_method(method) -> str:
    name = str(method or "")
    if name.startswith("btq_"):
        return "brah_" + name[4:]
    return name


SUBMIT_METHODS = frozenset({
    "brah_submitTx",
    "brah_sendTransaction",
    "eth_sendRawTransaction",
    "brah_requestFaucet",
    "brah_faucet",
    "brah_joinRewards",
    "brah_mine",
    "brah_councilDonate",
    "brah_launchL2",
    "brah_launchMemecoin",
})
''',
    )
    t = t.replace('"btq_', '"brah_')
    t = t.replace(
        '"methods": [\n'
        '                    "brah_submitTx",\n'
        '                    "brah_getAccount",\n'
        '                    "brah_getStats",\n'
        '                    "brah_getBlocks",\n'
        '                    "eth_getBalance",\n'
        '                ]',
        '"methods": [\n'
        '                    "brah_submitTx",\n'
        '                    "brah_getAccount",\n'
        '                    "brah_getStats",\n'
        '                    "brah_getBlocks",\n'
        '                    "eth_getBalance",\n'
        '                ]',
    )
    t = t.replace(
        '                "BTQ": from_units_float(int(acct.get("0", 0))),\n'
        '                "Brahma": from_units_float(int(acct.get("0", 0))),\n'
        '                "BRAHMA": from_units_float(int(acct.get("0", 0))),\n'
        '                "Brahma Coin": from_units_float(int(acct.get("0", 0))),',
        '                "BRAH": from_units_float(int(acct.get("0", 0))),\n'
        '                "Brahma": from_units_float(int(acct.get("0", 0))),\n'
        '                "BRAHMA": from_units_float(int(acct.get("0", 0))),\n'
        '                "Brahma Coin": from_units_float(int(acct.get("0", 0))),',
    )
    t = t.replace(
        '                    "BTQ": from_units_float(int(acct.get("0", 0))),\n'
        '                    "Brahma": from_units_float(int(acct.get("0", 0))),',
        '                    "BRAH": from_units_float(int(acct.get("0", 0))),\n'
        '                    "Brahma": from_units_float(int(acct.get("0", 0))),',
    )
    t = t.replace(
        'method = request.get("method")\n'
        '        self._rpc_method = str(method or "")\n',
        'method = canonical_rpc_method(request.get("method"))\n'
        '        self._rpc_method = str(method or "")\n',
    )
    p.write_text(t, encoding="utf-8")
    print("patched l1_mainnet.py")


def relocate_wallet() -> None:
    mapping = [
        WALLET_SRC / "main/java/com/btq/wallet",
        WALLET_SRC / "test/java/com/btq/wallet",
        WALLET_SRC / "androidTest/java/com/btq/wallet",
    ]
    repl = [
        ("package com.btq.wallet", "package com.brahmnetwork.wallet"),
        ("import com.btq.wallet", "import com.brahmnetwork.wallet"),
        ("com.btq.wallet", "com.brahmnetwork.wallet"),
        ("class BTQRpc", "class BrahRpc"),
        ("BTQRpc", "BrahRpc"),
        ("private const val TAG = \"BTQRpc\"", "private const val TAG = \"BrahRpc\""),
        ('"BTQ1|"', '"BRAH1|"'),
        ("BTQ1|", "BRAH1|"),
        ("BTQINVITE", "BRAHINVITE"),
        ("BTQHELLO", "BRAHHELLO"),
        ('"BTQF|"', '"BRAHF|"'),
        ("startsWith(\"BTQF|\")", "startsWith(\"BRAHF|\") || text.startsWith(\"BTQF|\")"),
        ("BTQSentinel", "BrahSentinel"),
        ("UNITS_PER_BTQ", "UNITS_PER_BRAH"),
        ("L2_LAUNCH_BURN_BTQ", "L2_LAUNCH_BURN"),
        ("fun isBtqAddress", "fun isBrahAddress"),
        ("isBtqAddress", "isBrahAddress"),
        ('"btq_submitTx"', '"brah_submitTx"'),
        ('"btq_getAccount"', '"brah_getAccount"'),
        ('"btq_getStats"', '"brah_getStats"'),
        ("btq.publicRpc", "brah.publicRpc"),
        ('System.getenv("BTQ_PUBLIC_RPC")',
         '(System.getenv("BRAH_PUBLIC_RPC") ?: System.getenv("BTQ_PUBLIC_RPC"))'),
        ('thread(name = "btq-mesh-tcp"', 'thread(name = "brah-mesh-tcp"'),
        ('thread(name = "btq-mesh-udp"', 'thread(name = "brah-mesh-udp"'),
        ('thread(name = "btq-mesh-hello"', 'thread(name = "brah-mesh-hello"'),
        ('thread(name = "btq-bt-listen"', 'thread(name = "brah-bt-listen"'),
        ('thread(name = "btq-bt-dial"', 'thread(name = "brah-bt-dial"'),
        ('thread(name = "btq-bt-read"', 'thread(name = "brah-bt-read"'),
        ('"btq-wallet"', '"brah-wallet"'),
        ('"btq_referrer_', '"brah_referrer_'),
        ("fun toUnits(btq: Double)", "fun toUnits(amount: Double)"),
        ("return BigDecimal.valueOf(btq)", "return BigDecimal.valueOf(amount)"),
        ('doubleOr("BRAHMA", balance.doubleOr("BTQ", balance.doubleOr("0")))',
         'doubleOr("BRAH", balance.doubleOr("BRAHMA", balance.doubleOr("Brahma Coin", balance.doubleOr("0"))))'),
        ('"Recipient must be a checksummed BRM1G or BTQ1G address"',
         '"Recipient must be a checksummed BRM1G address"'),
        ("BRAHMNETWORK v1.0.33", "BRAHMNETWORK v1.0.34"),
    ]
    for src in mapping:
        if not src.exists():
            print("skip missing", src)
            continue
        dest = Path(str(src).replace(r"\com\btq\wallet", r"\com\brahmnetwork\wallet"))
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.exists():
            shutil.rmtree(dest)
        shutil.copytree(src, dest)
        for f in dest.rglob("*.kt"):
            text = f.read_text(encoding="utf-8")
            for a, b in repl:
                text = text.replace(a, b)
            if f.name == "BTQRpc.kt":
                nf = f.with_name("BrahRpc.kt")
                nf.write_text(text, encoding="utf-8")
                f.unlink()
            else:
                f.write_text(text, encoding="utf-8")
        shutil.rmtree(src)
        # prune empty com/btq
        btq = src.parent
        if btq.exists() and not any(btq.iterdir()):
            btq.rmdir()
            com = btq.parent
            if com.name == "com" and com.exists() and not any(com.iterdir()):
                pass
        print("moved", src, "->", dest)


if __name__ == "__main__":
    patch_l1()
    relocate_wallet()
    print("rebrand pass done")
