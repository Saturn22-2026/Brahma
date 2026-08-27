# Implementation Plan: Replace ECDSA with ML-DSA in Bitcoin

This plan outlines the steps to replace the Elliptic Curve Digital Signature Algorithm (ECDSA) in the Bitcoin codebase with the Module Lattice-Based Digital Signature Algorithm (ML-DSA) using the `mldsa-native` implementation. This is a significant architectural change that turns the codebase into a "Quantum-Resistant" variant of Bitcoin.

## User Review Required

> [!IMPORTANT]
> **Consensus Breaking Change**: This modification completely breaks compatibility with the existing Bitcoin network. It is intended for experimental/research purposes (Bitcoin-Quantum).
>
> **Key and Signature Sizes**: ML-DSA keys and signatures are significantly larger than ECDSA.
> - **Public Key**: ~33 bytes -> 1312 bytes (ML-DSA-44)
> - **Private Key**: 32 bytes -> 2560 bytes (ML-DSA-44)
> - **Signature**: ~72 bytes -> 2420 bytes (ML-DSA-44)
>
> **Performance**: ML-DSA verification is generally faster than ECDSA, but the increased data size will impact block size and transaction throughput.

## Proposed Changes

### 1. Build System Integration

#### [NEW] [mldsa.cmake](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/cmake/mldsa.cmake)
- Create a CMake helper to compile the `mldsa-native` source files.
- Include the `mldsa/src` and `mldsa/src/fips202` directories.

#### [MODIFY] [CMakeLists.txt](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/crypto/CMakeLists.txt)
- Add `mldsa-native` sources to the `bitcoin_crypto` library or create a standalone `mldsa` library.

---

### 2. Cryptographic Abstraction Update

#### [MODIFY] [pubkey.h](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/pubkey.h)
- Update `CPubKey::SIZE` to `1312` (MLDSA44_PUBLICKEYBYTES).
- Update `CPubKey::SIGNATURE_SIZE` to `2420` (MLDSA44_BYTES).
- Update `vch` internal buffer size.
- Update `GetLen` and `IsValid` to handle ML-DSA keys.

#### [MODIFY] [key.h](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/key.h)
- Update `CKey::SIZE` and `KeyType` array size to `2560` (MLDSA44_SECRETKEYBYTES).
- Ensure `secure_unique_ptr` handles the larger size.

---

### 3. Implementation of ML-DSA Operations

#### [MODIFY] [key.cpp](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/key.cpp)
- Replace `secp256k1` calls in `MakeNewKey`, `GetPubKey`, and `Sign` with `mldsa-native` equivalents.
- Implement `ML-DSA-44` key generation and signing logic.

#### [MODIFY] [pubkey.cpp](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/pubkey.cpp)
- Replace `secp256k1` calls in `Verify` with `mldsa-native` verification.
- Update `IsFullyValid` for ML-DSA.

---

### 4. Address and Script Impacts

#### [MODIFY] [script/interpreter.cpp](file:///C:/Users/Navesh/Documents/GitHub/Bitcoin-Quantum/bitcoin/src/script/interpreter.cpp)
- Ensure `OP_CHECKSIG` and related opcodes correctly pass the larger signatures and public keys to the verification functions.
- (Optional) Adjust `MAX_STANDARD_P2WSH_STACK_ITEM_SIZE` if necessary, though 10KB `MAX_SCRIPT_SIZE` should suffice.

## Verification Plan

### Automated Tests
- Run `test_bitcoin` unit tests, specifically focusing on `key_tests.cpp` and `pubkey_tests.cpp`.
- Execute `src/test/test_bitcoin --run_test=key_tests` and `pubkey_tests`.

### Manual Verification
- Verify that a newly generated "Quantum" address can sign a message and be verified.
- Inspect serialized transaction sizes to confirm ML-DSA signature presence.
