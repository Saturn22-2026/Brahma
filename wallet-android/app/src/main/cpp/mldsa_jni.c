#include <jni.h>
#include <string.h>
#include <stdlib.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdio.h>
#include "mldsa-native/mldsa/mldsa_native.h"

// Provide randombytes for mldsa-native
int randombytes(uint8_t *out, size_t outlen) {
    int fd = open("/dev/urandom", O_RDONLY);
    if (fd < 0) return -1;
    ssize_t r = read(fd, out, outlen);
    close(fd);
    return (r == (ssize_t)outlen) ? 0 : -1;
}

JNIEXPORT jobject JNICALL
Java_com_btq_wallet_CryptoManager_generateKeyPair(JNIEnv *env, jobject thiz) {
    uint8_t pk[MLDSA44_PUBLICKEYBYTES];
    uint8_t sk[MLDSA44_SECRETKEYBYTES];

    if (PQCP_MLDSA_NATIVE_MLDSA44_keypair(pk, sk) != 0) {
        return NULL;
    }

    jbyteArray jpk = (*env)->NewByteArray(env, MLDSA44_PUBLICKEYBYTES);
    (*env)->SetByteArrayRegion(env, jpk, 0, MLDSA44_PUBLICKEYBYTES, (jbyte *)pk);

    jbyteArray jsk = (*env)->NewByteArray(env, MLDSA44_SECRETKEYBYTES);
    (*env)->SetByteArrayRegion(env, jsk, 0, MLDSA44_SECRETKEYBYTES, (jbyte *)sk);

    jclass keypairClass = (*env)->FindClass(env, "com/btq/wallet/KeyPair");
    if (keypairClass == NULL) return NULL;
    jmethodID constructor = (*env)->GetMethodID(env, keypairClass, "<init>", "([B[B)V");
    jobject keypairObj = (*env)->NewObject(env, keypairClass, constructor, jpk, jsk);

    return keypairObj;
}

JNIEXPORT jbyteArray JNICALL
Java_com_btq_wallet_CryptoManager_signMessage(JNIEnv *env, jobject thiz, jbyteArray secret_key, jbyteArray message) {
    jbyte *sk = (*env)->GetByteArrayElements(env, secret_key, NULL);
    jbyte *msg = (*env)->GetByteArrayElements(env, message, NULL);
    jsize mlen = (*env)->GetArrayLength(env, message);

    uint8_t sig[MLDSA44_BYTES];
    if (PQCP_MLDSA_NATIVE_MLDSA44_signature(sig, (uint8_t *)msg, mlen, NULL, 0, (uint8_t *)sk) != 0) {
        (*env)->ReleaseByteArrayElements(env, secret_key, sk, JNI_ABORT);
        (*env)->ReleaseByteArrayElements(env, message, msg, JNI_ABORT);
        return NULL;
    }

    jbyteArray jsig = (*env)->NewByteArray(env, MLDSA44_BYTES);
    (*env)->SetByteArrayRegion(env, jsig, 0, MLDSA44_BYTES, (jbyte *)sig);

    (*env)->ReleaseByteArrayElements(env, secret_key, sk, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, message, msg, JNI_ABORT);

    return jsig;
}

JNIEXPORT jstring JNICALL
Java_com_btq_wallet_CryptoManager_deriveAddress(JNIEnv *env, jobject thiz, jbyteArray public_key) {
    // For the demo, we use a simple hex representation of the first 20 bytes of the PK
    // In production this should be a proper hash (SHAKE256 or SHA256)
    jbyte *pk = (*env)->GetByteArrayElements(env, public_key, NULL);
    char address[64];
    sprintf(address, "BTQ1");
    for(int i=0; i<12; i++) {
        sprintf(address + 4 + i*2, "%02x", (unsigned char)pk[i]);
    }
    (*env)->ReleaseByteArrayElements(env, public_key, pk, JNI_ABORT);
    return (*env)->NewStringUTF(env, address);
}
