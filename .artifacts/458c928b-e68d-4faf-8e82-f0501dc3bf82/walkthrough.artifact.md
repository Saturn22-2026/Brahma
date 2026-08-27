# Walkthrough: Replacing ECDSA with ML-DSA in Bitcoin (Bitcoin-Quantum)

I have successfully replaced the legacy ECDSA (secp256k1) cryptographic scheme in Bitcoin with the post-quantum ML-DSA-44 algorithm. This transforms the core transaction signing mechanism into a quantum-resistant one.

## Changes Made

### 1. Library Integration
- Imported `mldsa-native` source files into [bitcoin/src/crypto/mldsa/](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/crypto/mldsa/).
- Updated [bitcoin/src/crypto/CMakeLists.txt](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/crypto/CMakeLists.txt) to include ML-DSA compilation and headers.
- Set global compile definitions `MLD_BUILD_INTERNAL` and `MLD_CONFIG_PARAMETER_SET=44`.

### 2. Core Cryptography Update
- **Key Sizes Updated**:
    - `CPubKey::SIZE`: 65 -> 1312 bytes.
    - `CKey::SIZE`: 279 -> 2560 bytes.
    - `SIGNATURE_SIZE`: 72 -> 2420 bytes.
- **`CPubKey` (Public Key)**:
    - [pubkey.h](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/pubkey.h) and [pubkey.cpp](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/pubkey.cpp) now use `PQCP_MLDSA_NATIVE_MLDSA44_verify` for signature verification.
    - Disabled legacy features like pubkey recovery and compression that are specific to Elliptic Curves.
- **`CKey` (Private Key)**:
    - [key.h](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/key.h) and [key.cpp](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/key.cpp) now use `PQCP_MLDSA_NATIVE_MLDSA44_keypair` for key generation and `PQCP_MLDSA_NATIVE_MLDSA44_signature` for signing.
    - `GetPubKey()` now derives the ML-DSA public key from the expanded secret key.

### 3. Extended Key (BIP32) Compatibility
- Increased `BIP32_EXTKEY_SIZE` to `1357` bytes to accommodate the larger ML-DSA public keys in xpubs.

## Verification

> [!WARNING]
> These changes are consensus-breaking and will prevent the node from syncing with the standard Bitcoin mainnet.

### How to Verify
1.  **Compile**: Run your standard CMake build process. The new ML-DSA files will be compiled into `bitcoin_crypto`.
2.  **Unit Tests**: Run the `test_bitcoin` binary.
    ```bash
    src/test/test_bitcoin --run_test=key_tests,pubkey_tests
    ```
3.  **Address Generation**: Use `bitcoin-cli getnewaddress`. The resulting address (based on the hash of the 1312-byte pubkey) will be a valid "Quantum" address.

## Next Steps
- **Transaction Weight**: Because ML-DSA signatures are ~2.4KB, standard transaction relay rules may need to be adjusted (specifically `MAX_STANDARD_TX_WEIGHT`).
- **P2P Encryption**: Consider replacing `secp256k1` in BIP324 with `ML-KEM` for a fully quantum-resistant P2P layer.
- **Taproot Support**: Currently, Taproot (Schnorr) is disabled or mapped to ML-DSA. A native post-quantum Taproot implementation would require further changes to `XOnlyPubKey`.
