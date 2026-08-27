use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString};
use jni::sys::{jbyteArray, jstring};
use pqc_dilithium::*;
use sha2::{Sha256, Digest};

#[no_mangle]
pub extern "system" fn Java_com_btq_wallet_CryptoManager_generateKeyPair<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> JObject<'local> {
    let keys = Keypair::generate();

    let pub_key = env.byte_array_from_slice(&keys.public).unwrap();
    let sec_key = env.byte_array_from_slice(&keys.secret).unwrap();

    let keypair_class = env.find_class("com/btq/wallet/KeyPair").unwrap();
    let keypair_obj = env.new_object(
        keypair_class,
        "([B[B)V",
        &[(&pub_key).into(), (&sec_key).into()],
    ).unwrap();

    keypair_obj
}

#[no_mangle]
pub extern "system" fn Java_com_btq_wallet_CryptoManager_signMessage<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    secret_key: jbyteArray,
    message: jbyteArray,
) -> jbyteArray {
    let sk_bytes = env.convert_byte_array(secret_key).unwrap();
    let msg_bytes = env.convert_byte_array(message).unwrap();

    let sig = sign(&msg_bytes, &sk_bytes).expect("Signing failed");

    env.byte_array_from_slice(&sig).unwrap()
}

#[no_mangle]
pub extern "system" fn Java_com_btq_wallet_CryptoManager_deriveAddress<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    public_key: jbyteArray,
) -> jstring {
    let pub_bytes = env.convert_byte_array(public_key).unwrap();

    let mut hasher = Sha256::new();
    hasher.update(pub_bytes);
    let result = hasher.finalize();
    let address = format!("0x{}", hex::encode(&result[0..20]));

    env.new_string(address).unwrap().into_raw()
}
