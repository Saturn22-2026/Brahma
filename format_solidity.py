with open('wallets.txt', 'r') as f:
    lines = f.readlines()
addresses = [line.split('|')[0] for line in lines[1:]]

solidity_array = "        return [\n            "
for i, addr in enumerate(addresses):
    solidity_array += f'address(uint160(bytes20(hex"{addr[2:]}")))'
    if i < len(addresses) - 1:
        solidity_array += ", "
    if (i + 1) % 2 == 0:
        solidity_array += "\n            "
solidity_array += "\n        ];"

with open('solidity_output.txt', 'w') as f:
    f.write(solidity_array)
