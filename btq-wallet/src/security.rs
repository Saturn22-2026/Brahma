use aes_gcm::{Aes256Gcm, Key, Nonce, aead::{Aead, KeyInit}};
use winreg::RegKey;
use winreg::enums::HKEY_LOCAL_MACHINE;
use argon2::{
    password_hash::{PasswordHasher, SaltString},
    Argon2,
};
use rand::{RngCore, thread_rng};
use zeroize::Zeroize;

pub struct BTQSecurity;

impl BTQSecurity {
    /// Retrieves the local hardware ID (Windows MachineGuid).
    fn get_machine_id() -> String {
        let hklm = RegKey::predef(HKEY_LOCAL_MACHINE);
        let crypto = hklm.open_subkey("SOFTWARE\\Microsoft\\Cryptography")
            .expect("Registry access failed: Run as Admin if needed");
        crypto.get_value("MachineGuid").expect("MachineGuid not found")
    }

    /// Derives a 256-bit AES key from hardware ID + User Passphrase using Argon2id.
    fn derive_aes_key(password: &str) -> Key<Aes256Gcm> {
        let machine_id = Self::get_machine_id();

        // "Sovereign-Salt-2026" base64
        let salt = SaltString::from_b64("U292ZXJlaWduLVNhbHQtMjAyNg").unwrap();
        let argon2 = Argon2::default();

        let mut output = [0u8; 32];
        let combined_secret = format!("{}:{}", machine_id, password);

        argon2.hash_password_into(
            combined_secret.as_bytes(),
            salt.as_str().as_bytes(),
            &mut output
        ).expect("Argon2 derivation failed");

        *Key::<Aes256Gcm>::from_slice(&output)
    }

    /// Encrypts data and returns a hex-encoded blob containing [Nonce(12b)][Ciphertext].
    pub fn encrypt_data(data: &[u8], password: &str) -> String {
        let aes_key = Self::derive_aes_key(password);
        let cipher = Aes256Gcm::new(&aes_key);

        let mut nonce_bytes = [0u8; 12];
        thread_rng().fill_bytes(&mut nonce_bytes);
        let nonce = Nonce::from_slice(&nonce_bytes);

        let ciphertext = cipher.encrypt(nonce, data)
            .expect("Encryption failure");

        let mut combined = nonce_bytes.to_vec();
        combined.extend(ciphertext);

        hex::encode(combined)
    }

    /// Decrypts a hex-encoded blob using the user password and hardware ID.
    pub fn decrypt_data(encrypted_hex: &str, password: &str) -> Result<Vec<u8>, String> {
        let combined = hex::decode(encrypted_hex).map_err(|e| e.to_string())?;

        if combined.len() < 12 {
            return Err("Invalid encrypted data format".to_string());
        }

        let (nonce_bytes, ciphertext) = combined.split_at(12);
        let nonce = Nonce::from_slice(nonce_bytes);

        let aes_key = Self::derive_aes_key(password);
        let cipher = Aes256Gcm::new(&aes_key);

        cipher.decrypt(nonce, ciphertext)
            .map_err(|_| "Hardware Authorization Failed: Incorrect password or device mismatch.".to_string())
    }
}

pub struct ZeroizedData(pub Vec<u8>);

impl Drop for ZeroizedData {
    fn drop(&mut self) {
        self.0.zeroize();
    }
}
