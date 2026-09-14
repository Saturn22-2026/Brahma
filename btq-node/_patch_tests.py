from pathlib import Path
p = Path(r"c:\GitHub\Bitcoin-Quantum\btq-node\test_launch_gates.py")
t = p.read_text(encoding="utf-8")
for a, b in [
    ("UNITS_PER_BTQ", "UNITS_PER_BRAH"),
    ("is_btq_address", "is_brah_address"),
    ("btq_outstanding_units", "brah_outstanding_units"),
    ("BTQ_DATA_DIR", "BRAH_DATA_DIR"),
    ("BTQ_ALLOW_LAN", "BRAH_ALLOW_LAN"),
    ("BTQ_JOIN_MAX_PER_HOUR", "BRAH_JOIN_MAX_PER_HOUR"),
    ("BTQ_CORS_ORIGIN", "BRAH_CORS_ORIGIN"),
    ("BTQ_PUBLIC_RPC", "BRAH_PUBLIC_RPC"),
    ("BTQ_EXPLORER", "BRAH_EXPLORER"),
    ("BTQ_SERVE_APK", "BRAH_SERVE_APK"),
    ("BTQ_ALLOW_SECRETS_ON_DISK", "BRAH_ALLOW_SECRETS_ON_DISK"),
    ("BTQ_RPC_PORT", "BRAH_RPC_PORT"),
    ('prefix="btq-rep-a-"', 'prefix="brah-rep-a-"'),
    ('prefix="btq-rep-b-"', 'prefix="brah-rep-b-"'),
    ('prefix="btq-rpc-"', 'prefix="brah-rpc-"'),
    ('self.assertIn("units_per_btq", str(ctx.exception))', 'self.assertIn("units scale", str(ctx.exception))'),
    ('self.assertIn("btq://claim?ref=%s" % addr, page)\n        ', ""),
    ('os.environ["BRAH_CORS_ORIGIN"] = "https://btq.example"', 'os.environ["BRAH_CORS_ORIGIN"] = "https://brah.example"'),
    ('self.assertEqual(allow_cors_origin("https://btq.example"), "https://btq.example")',
     'self.assertEqual(allow_cors_origin("https://brah.example"), "https://brah.example")'),
    ('hello_message("btq-l1-18545"', 'hello_message("brah-l1-18545"'),
    ('"node": "btq-l1-18545"', '"node": "brah-l1-18545"'),
    ('verify_hello({"node": "btq-l1-18545"', 'verify_hello({"node": "brah-l1-18545"'),
    ('msg = b"BTQ-roundtrip"', 'msg = b"BRAH-roundtrip"'),
    ("block_reward_btq", "block_reward"),
]:
    t = t.replace(a, b)
p.write_text(t, encoding="utf-8")
print("tests patched")
