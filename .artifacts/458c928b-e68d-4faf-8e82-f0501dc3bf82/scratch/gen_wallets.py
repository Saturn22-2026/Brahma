from eth_account import Account
import secrets
import json

names = ["Homie", "SlumDog", "Crazy God", "BoujieClique", "QuarterMile", "SoldiersOfFortune", "5thAvenue", "Pookie"]
registry = {}

for name in names:
    registry[name] = {
        "wealth": Account.from_key("0x" + secrets.token_hex(32)).address,
        "empower": Account.from_key("0x" + secrets.token_hex(32)).address,
        "stability": Account.from_key("0x" + secrets.token_hex(32)).address,
        "donation": Account.from_key("0x" + secrets.token_hex(32)).address,
        "airdrop": Account.from_key("0x" + secrets.token_hex(32)).address,
        "float": Account.from_key("0x" + secrets.token_hex(32)).address,
    }

print(json.dumps(registry, indent=2))
