# Dilithium3 is called directly. Keep that package so R8 cannot rename
# the parameter tables the signer reads by field.
-keep class org.bouncycastle.pqc.crypto.crystals.dilithium.** { *; }
-keep class org.bouncycastle.crypto.digests.SHAKEDigest { *; }
-keep class org.bouncycastle.crypto.prng.FixedSecureRandom { *; }
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
