# Walkthrough: C++ Build Tools & Shell Compatibility

I have successfully initiated the installation of the **C++ Build Tools** and verified the availability of **Git Bash** for Unix-style shell support on your Windows machine.

## Changes Made

### 1. Visual Studio Build Tools 2022
- **Action**: Initiated the installation of `Microsoft.VisualStudio.2022.BuildTools` via `winget`.
- **Workload**: Configured to include the **Desktop development with C++** components (specifically `link.exe` and `Windows 11 SDK`).
- **Status**: Installation is currently being processed by the Windows Installer in the background.

### 2. Git for Windows (Git Bash)
- **Action**: Installed `Git.Git` via `winget`.
- **Status**: [PASSED]
- **Capability**: You can now open a **Git Bash** terminal from your Start Menu to run `curl | sh` and other Linux-native commands.

### 3. Verification & Troubleshooting

| Feature | Status | Requirement |
| :--- | :--- | :--- |
| **Git Bash** | [READY] | Open "Git Bash" from Start Menu. |
| **MSVC Linker** | [PENDING]* | Requires background installation to complete. |

> [!CAUTION]
> **RESTART REQUIRED**: For the `link.exe` (MSVC Linker) to be recognized by the Rust compiler, you **must restart your computer** (or at least your IDE/Terminal) once the Visual Studio Installer has finished its work in the background.

## Next Steps

1.  **Monitor Installer**: Check your Windows notification tray or open the "Visual Studio Installer" application to monitor the completion of the Build Tools.
2.  **Restart**: Once complete, restart your machine.
3.  **Compile BTQ Node**: Run the following in a fresh terminal:
    ```powershell
    cd btq-node
    cargo check
    ```
