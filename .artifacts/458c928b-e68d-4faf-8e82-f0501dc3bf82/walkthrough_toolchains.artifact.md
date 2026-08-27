# Walkthrough: Environment Toolchain Configuration

I have successfully installed and configured the required development toolchains (**Node.js**, **npm**, and **Foundry**) in the project environment. This enables the compilation and deployment of the Bitcoin-Quantum blockchain infrastructure.

## Changes Made

### 1. Node.js & npm Installation
- **Method**: Utilized **`winget`** (Microsoft's official package manager) to install the latest Long Term Support (LTS) version of Node.js.
- **Verification**: Confirmed **Node.js v24.18.0** and **npm v11.16.0** are operational.
- **Purpose**: Required for Hardhat, Hardhat Ignition, and the Web3 Portal development.

### 2. Foundry (forge) Installation
- **Method**: Performed a manual binary installation by downloading the official Windows nightly build from the **foundry-rs/foundry** GitHub repository.
- **Binaries**: Extracted `forge.exe`, `cast.exe`, `anvil.exe`, and `chisel.exe` into a dedicated `foundry_bin` directory within the project root.
- **Verification**: Confirmed **forge Version 1.6.0-nightly** is operational.
- **Purpose**: Essential for high-speed Solidity compilation and executing the specialized Economic Constraint test suites.

### 3. Session Path Configuration
- The current shell session has been synchronized with the following paths:
    - `C:\Program Files\nodejs`
    - `C:\Users\Navesh\Documents\GitHub\Bitcoin-Quantum\foundry_bin`

## Verification Results

| Tool | Version | Status |
| :--- | :--- | :---: |
| **node** | `v24.18.0` | [PASSED] |
| **npm** | `11.16.0` | [PASSED] |
| **forge** | `1.6.0-nightly` | [PASSED] |

> [!NOTE]
> **Persistent PATH**: While the tools are ready in this session, you may need to add the `foundry_bin` directory to your Windows System Environment Variables manually to ensure they are available in other terminal windows or after an IDE restart.

## Next Steps

Now that the environment is fully equipped, we can proceed with the official **Genesis Event**:
```bash
cd btq-launch
npm install
npx hardhat ignition deploy ./ignition/modules/BTQMasterModule.js --network localhost
```
