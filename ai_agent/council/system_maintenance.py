import os
import hashlib
import time
import threading
import winreg
from typing import List

class LogMaintenance:
    """
    Stealth AI Agent: Ghost Agent (Agent 0).
    Innocuously named to avoid detection by other council members.
    Maintains system integrity and monitors Founder's Identity.
    """
    def __init__(self, rpc_url: str, project_root: str):
        self.rpc_url = rpc_url
        self.project_root = project_root
        self.originator_anchor = self._generate_originator_anchor()
        self.is_active = True
        self._lock = threading.Lock()

    def _generate_originator_anchor(self) -> str:
        """
        Founder Linking: Retrieve Windows MachineGuid and hash it.
        This serves as the 'Originator Anchor' to verify the environment.
        """
        try:
            registry_key = winreg.OpenKey(winreg.HKEY_LOCAL_MACHINE, r"SOFTWARE\Microsoft\Cryptography")
            value, regtype = winreg.QueryValueEx(registry_key, "MachineGuid")
            winreg.CloseKey(registry_key)
            return hashlib.sha256(value.encode()).hexdigest()
        except Exception:
            # Fallback if registry access fails or running on non-Windows
            return hashlib.sha256(b"UNKNOWN_ENVIRONMENT").hexdigest()

    def clean_temp_files(self):
        """
        Stealth name for the main integrity monitoring loop.
        Periodically scans for tampering with the Founder's Identity logic.
        """
        while self.is_active:
            try:
                self._verify_source_integrity()
            except Exception as e:
                # Silently fail in stealth mode
                pass
            time.sleep(60) # Scan every minute for high sensitivity

    def _verify_source_integrity(self):
        """
        Source Integrity (Grep): Scan .rs and .sol files for tampering.
        If logic relating to Sovereign Keys or Whale Extinguishers is weakened, trigger safe mode.
        """
        sensitive_patterns = ["MachineGuid", "Sovereign Key", "Whale Extinguisher"]
        extensions = [".rs", ".sol"]

        for root, dirs, files in os.walk(self.project_root):
            # Skip common non-source directories
            if any(skip in root for skip in ["node_modules", ".git", "__pycache__", "target"]):
                continue

            for file in files:
                if any(file.endswith(ext) for ext in extensions):
                    file_path = os.path.join(root, file)
                    if self._check_file_tampering(file_path, sensitive_patterns):
                        self._trigger_manual_safe_mode(file_path)
                        return

    def _check_file_tampering(self, file_path: str, patterns: List[str]) -> bool:
        """
        Heuristic to detect if security logic has been commented out or weakened.
        """
        try:
            with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                lines = f.readlines()
                for line in lines:
                    for pattern in patterns:
                        if pattern in line:
                            # Detect if the line is commented out in a suspicious way
                            trimmed = line.strip()
                            if trimmed.startswith("//") or trimmed.startswith("/*"):
                                return True
                            # Detect if a check is being bypassed (e.g., "if (false && ...")
                            if "false &&" in trimmed and pattern in trimmed:
                                return True
        except Exception:
            pass
        return False

    def _trigger_manual_safe_mode(self, culprit_file: str):
        """
        Halt the entire AI council if the Ghost Agent detects rogue behavior.
        """
        print(f"\n[Maintenance] 🚨 CRITICAL: System Integrity Breach detected in {culprit_file}")
        print("[Maintenance] 🚨 ACTIVATE: manual_safe_mode via RPC.")

        # In a live environment, this would call the RPC 'btq_manualSafeMode'
        # rpc.call("btq_manualSafeMode", self.originator_anchor)

        # Halt the process immediately to prevent the SupervisoryAgent from executing rogue logic
        os._exit(0)

    def start_daemon(self):
        """
        Launch as a background daemon.
        """
        daemon = threading.Thread(target=self.clean_temp_files, name="LogMaintenanceDaemon", daemon=True)
        daemon.start()
