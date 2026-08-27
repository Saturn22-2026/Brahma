from eth_account import Account
import secrets
import json

names = ["Homie", "SlumDog", "Crazy God", "BoujieClique", "QuarterMile", "SoldiersOfFortune", "5thAvenue", "Pookie"]
roles = ["wealth", "empower", "stability", "donation", "airdrop"]

final_data = {}

for name in names:
    final_data[name] = {}
    for role in roles:
        priv = "0x" + secrets.token_hex(32)
        addr = Account.from_key(priv).address
        final_data[name][role] = {
            "address": addr,
            "private_key": priv
        }

print(json.dumps(final_data, indent=2))
