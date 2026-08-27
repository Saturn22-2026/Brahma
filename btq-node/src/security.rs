use aes_gcm::{Aes256Gcm, Key, Nonce, aead::{Aead, KeyInit}};
use winreg::RegKey;
use winreg::enums::HKEY_LOCAL_MACHINE;
use sha2::{Sha256, Digest};
use zeroize::Zeroize;

/**
 * @title BTQSecurity
 * @dev Implements Defensive Key Encapsulation (Phase 52).
 * Pins secrets to the local MachineGuid and a Master Sovereign Key.
 */
pub struct BTQSecurity;

impl BTQSecurity {
    /**
     * @dev Retrieves the local hardware ID (Windows MachineGuid).
     */
    fn get_machine_id() -> String {
        let hklm = RegKey::predef(HKEY_LOCAL_MACHINE);
        let crypto = hklm.open_subkey("SOFTWARE\\Microsoft\\Cryptography").expect("Registry access failed");
        crypto.get_value("MachineGuid").expect("MachineGuid not found")
    }

    /**
     * @dev Derives a 256-bit AES key from hardware ID + Master Passphrase.
     */
    fn derive_aes_key(master_key: &str) -> Key<Aes256Gcm> {
        let machine_id \u003d Self::get_machine_id();
        let mut hasher = Sha256::new();
        hasher.update(machine_id.as_bytes());
        hasher.update(master_key.as_bytes());
        hasher.update(b"Sovereign-Salt-2026");
        let result = hasher.finalize();
        *Key::\u003cAes256Gcm\u003e::from_slice(&result)
    }

    /**
     * @dev Decrypts a hardcoded blob into a zeroizable byte vector.
     */
    pub fn decrypt_secret(encrypted_hex: &str, master_key: &str) -> Result\u003cVec\u003cu8\u003e, String\u003e {
        let cipher_data = hex::decode(encrypted_hex).map_err(|e| e.to_string())?;

        // Split Nonce (12 bytes) and Ciphertext
        if cipher_data.len() < 12 { return Err("Invalid blob".to_string()); }
        let (nonce_bytes, ciphertext) = cipher_data.split_at(12);
        let nonce = Nonce::from_slice(nonce_bytes);

        let aes_key = Self::derive_aes_key(master_key);
        let cipher = Aes256Gcm::new(&aes_key);

        let decrypted = cipher.decrypt(nonce, ciphertext)
            .map_err(|_| "Hardware Authorization Failed: Incorrect Master Key or Hardware Mismatch".to_string())?;

        Ok(decrypted)
    }
}

/**
 * @dev Wrapper for sensitive keys that implements Zeroize on Drop.
 */
pub struct ZeroizedKey(pub Vec\u003cu8\u003e);

impl Drop for ZeroizedKey {
    fn drop(&mut self) {
        self.0.zeroize();
    }
}
