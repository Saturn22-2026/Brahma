package com.brahmnetwork.wallet;

import java.util.Arrays;
import org.bouncycastle.crypto.digests.SHAKEDigest;

/**
 * Deterministic Dilithium3 signing in the round 3.1 encoding (4000-byte secret key,
 * 3293-byte signature). Genesis wallets use this encoding; the bundled BouncyCastle
 * reads only the newer one.
 */
public final class LegacyDilithium3 {
    private static final int N = 256;
    private static final int Q = 8380417;
    private static final int QINV = 58728449;
    private static final int D = 13;
    private static final int K = 6;
    private static final int L = 5;
    private static final int ETA = 4;
    private static final int TAU = 49;
    private static final int BETA = 196;
    private static final int GAMMA1 = 1 << 19;
    private static final int GAMMA2 = (Q - 1) / 32;
    private static final int OMEGA = 55;
    private static final int SEEDBYTES = 32;
    private static final int CRHBYTES = 64;
    private static final int POLYETA_PACKEDBYTES = 128;
    private static final int POLYT0_PACKEDBYTES = 416;
    private static final int POLYZ_PACKEDBYTES = 640;
    private static final int POLYW1_PACKEDBYTES = 128;
    public static final int PUBLIC_KEY_BYTES = SEEDBYTES + K * 320;
    public static final int SECRET_KEY_BYTES =
        3 * SEEDBYTES + (L + K) * POLYETA_PACKEDBYTES + K * POLYT0_PACKEDBYTES;
    public static final int SIGNATURE_BYTES = SEEDBYTES + L * POLYZ_PACKEDBYTES + OMEGA + K;

    private static final int[] ZETAS = buildZetas();

    private LegacyDilithium3() {
    }

    /** True when [secretKey] is a round 3.1 key for [publicKey]: tr = SHAKE256(pk) sits at bytes 64..96. */
    public static boolean matches(byte[] secretKey, byte[] publicKey) {
        if (secretKey == null || publicKey == null) return false;
        if (secretKey.length != SECRET_KEY_BYTES || publicKey.length != PUBLIC_KEY_BYTES) return false;
        byte[] tr = shake256(SEEDBYTES, publicKey);
        return Arrays.equals(tr, Arrays.copyOfRange(secretKey, 2 * SEEDBYTES, 3 * SEEDBYTES))
            && Arrays.equals(Arrays.copyOfRange(secretKey, 0, SEEDBYTES), Arrays.copyOfRange(publicKey, 0, SEEDBYTES));
    }

    public static byte[] sign(byte[] sk, byte[] message) {
        if (sk.length != SECRET_KEY_BYTES) throw new IllegalArgumentException("secret key size");
        byte[] rho = Arrays.copyOfRange(sk, 0, SEEDBYTES);
        byte[] key = Arrays.copyOfRange(sk, SEEDBYTES, 2 * SEEDBYTES);
        byte[] tr = Arrays.copyOfRange(sk, 2 * SEEDBYTES, 3 * SEEDBYTES);
        int off = 3 * SEEDBYTES;
        int[][] s1 = new int[L][];
        for (int i = 0; i < L; i++, off += POLYETA_PACKEDBYTES) s1[i] = unpackEta(sk, off);
        int[][] s2 = new int[K][];
        for (int i = 0; i < K; i++, off += POLYETA_PACKEDBYTES) s2[i] = unpackEta(sk, off);
        int[][] t0 = new int[K][];
        for (int i = 0; i < K; i++, off += POLYT0_PACKEDBYTES) t0[i] = unpackT0(sk, off);

        byte[] mu = shake256(CRHBYTES, tr, message);
        byte[] rhoprime = shake256(CRHBYTES, key, mu);

        int[][][] mat = new int[K][L][];
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < L; j++) mat[i][j] = uniform(rho, (i << 8) + j);
        }
        for (int[] p : s1) ntt(p);
        for (int[] p : s2) ntt(p);
        for (int[] p : t0) ntt(p);

        byte[] sig = new byte[SIGNATURE_BYTES];
        int nonce = 0;
        while (true) {
            int[][] y = new int[L][];
            for (int i = 0; i < L; i++) y[i] = uniformGamma1(rhoprime, L * nonce + i);
            nonce++;

            int[][] z = new int[L][];
            for (int i = 0; i < L; i++) {
                z[i] = y[i].clone();
                ntt(z[i]);
            }
            int[][] w1 = new int[K][];
            int[][] w0 = new int[K][];
            for (int i = 0; i < K; i++) {
                int[] acc = pointwise(mat[i][0], z[0]);
                for (int j = 1; j < L; j++) {
                    int[] t = pointwise(mat[i][j], z[j]);
                    for (int c = 0; c < N; c++) acc[c] += t[c];
                }
                reduce(acc);
                invnttTomont(acc);
                caddq(acc);
                w0[i] = new int[N];
                w1[i] = new int[N];
                for (int c = 0; c < N; c++) {
                    int[] parts = decompose(acc[c]);
                    w1[i][c] = parts[0];
                    w0[i][c] = parts[1];
                }
            }
            byte[] w1packed = new byte[K * POLYW1_PACKEDBYTES];
            for (int i = 0; i < K; i++) {
                for (int c = 0; c < N / 2; c++) {
                    w1packed[i * POLYW1_PACKEDBYTES + c] = (byte) (w1[i][2 * c] | (w1[i][2 * c + 1] << 4));
                }
            }
            byte[] ctilde = shake256(SEEDBYTES, mu, w1packed);
            int[] cp = challenge(ctilde);
            ntt(cp);

            boolean reject = false;
            for (int i = 0; i < L && !reject; i++) {
                int[] t = pointwise(cp, s1[i]);
                invnttTomont(t);
                for (int c = 0; c < N; c++) t[c] += y[i][c];
                reduce(t);
                if (chknorm(t, GAMMA1 - BETA)) reject = true;
                z[i] = t;
            }
            if (reject) continue;

            for (int i = 0; i < K && !reject; i++) {
                int[] h = pointwise(cp, s2[i]);
                invnttTomont(h);
                for (int c = 0; c < N; c++) w0[i][c] -= h[c];
                reduce(w0[i]);
                if (chknorm(w0[i], GAMMA2 - BETA)) reject = true;
            }
            if (reject) continue;

            int[][] hint = new int[K][];
            for (int i = 0; i < K && !reject; i++) {
                int[] h = pointwise(cp, t0[i]);
                invnttTomont(h);
                reduce(h);
                if (chknorm(h, GAMMA2)) reject = true;
                hint[i] = h;
            }
            if (reject) continue;

            int ones = 0;
            for (int i = 0; i < K; i++) {
                for (int c = 0; c < N; c++) {
                    w0[i][c] += hint[i][c];
                    int a0 = w0[i][c];
                    int a1 = w1[i][c];
                    int bit = (a0 > GAMMA2 || a0 < -GAMMA2 || (a0 == -GAMMA2 && a1 != 0)) ? 1 : 0;
                    hint[i][c] = bit;
                    ones += bit;
                }
            }
            if (ones > OMEGA) continue;

            System.arraycopy(ctilde, 0, sig, 0, SEEDBYTES);
            int pos = SEEDBYTES;
            for (int i = 0; i < L; i++, pos += POLYZ_PACKEDBYTES) packZ(z[i], sig, pos);
            int k = 0;
            for (int i = 0; i < K; i++) {
                for (int c = 0; c < N; c++) {
                    if (hint[i][c] != 0) sig[pos + k++] = (byte) c;
                }
                sig[pos + OMEGA + i] = (byte) k;
            }
            return sig;
        }
    }

    public static boolean verify(byte[] publicKey, byte[] message, byte[] signature) {
        if (publicKey == null || message == null || signature == null) return false;
        if (publicKey.length != PUBLIC_KEY_BYTES || signature.length != SIGNATURE_BYTES) return false;
        byte[] rho = Arrays.copyOfRange(publicKey, 0, SEEDBYTES);
        int[][] t1 = new int[K][];
        for (int i = 0; i < K; i++) t1[i] = unpackT1(publicKey, SEEDBYTES + i * 320);
        byte[] ctilde = Arrays.copyOfRange(signature, 0, SEEDBYTES);
        int[][] z = new int[L][];
        int pos = SEEDBYTES;
        for (int i = 0; i < L; i++, pos += POLYZ_PACKEDBYTES) z[i] = unpackZ(signature, pos);
        int[][] hint = new int[K][];
        for (int i = 0; i < K; i++) hint[i] = new int[N];
        int k = 0;
        for (int i = 0; i < K; i++) {
            int next = signature[pos + OMEGA + i] & 0xFF;
            if (next < k || next > OMEGA) return false;
            for (int j = k; j < next; j++) {
                int idx = signature[pos + j] & 0xFF;
                if (idx >= N || (j > k && idx <= (signature[pos + j - 1] & 0xFF))) return false;
                hint[i][idx] = 1;
            }
            k = next;
        }
        for (int j = k; j < OMEGA; j++) {
            if (signature[pos + j] != 0) return false;
        }
        for (int i = 0; i < L; i++) {
            if (chknorm(z[i], GAMMA1 - BETA)) return false;
        }

        byte[] tr = shake256(SEEDBYTES, publicKey);
        byte[] mu = shake256(CRHBYTES, tr, message);
        int[][][] mat = new int[K][L][];
        for (int i = 0; i < K; i++) {
            for (int j = 0; j < L; j++) mat[i][j] = uniform(rho, (i << 8) + j);
        }
        int[] cp = challenge(ctilde);
        ntt(cp);
        for (int i = 0; i < L; i++) ntt(z[i]);
        for (int i = 0; i < K; i++) {
            for (int c = 0; c < N; c++) t1[i][c] <<= D;
            ntt(t1[i]);
        }
        int[][] w1 = new int[K][];
        for (int i = 0; i < K; i++) {
            int[] acc = pointwise(mat[i][0], z[0]);
            for (int j = 1; j < L; j++) {
                int[] t = pointwise(mat[i][j], z[j]);
                for (int c = 0; c < N; c++) acc[c] += t[c];
            }
            int[] ct = pointwise(cp, t1[i]);
            for (int c = 0; c < N; c++) acc[c] -= ct[c];
            reduce(acc);
            invnttTomont(acc);
            caddq(acc);
            w1[i] = new int[N];
            for (int c = 0; c < N; c++) w1[i][c] = useHint(acc[c], hint[i][c]);
        }
        byte[] w1packed = new byte[K * POLYW1_PACKEDBYTES];
        for (int i = 0; i < K; i++) {
            for (int c = 0; c < N / 2; c++) {
                w1packed[i * POLYW1_PACKEDBYTES + c] = (byte) (w1[i][2 * c] | (w1[i][2 * c + 1] << 4));
            }
        }
        byte[] again = shake256(SEEDBYTES, mu, w1packed);
        return Arrays.equals(ctilde, again);
    }

    private static int useHint(int a, int hint) {
        int[] parts = decompose(a);
        if (hint == 0) return parts[0];
        if (parts[1] > 0) return (parts[0] + 1) & 15;
        return (parts[0] - 1) & 15;
    }

    private static int[] unpackT1(byte[] src, int off) {
        int[] r = new int[N];
        for (int i = 0; i < N / 4; i++) {
            int b0 = src[off + 5 * i] & 0xFF;
            int b1 = src[off + 5 * i + 1] & 0xFF;
            int b2 = src[off + 5 * i + 2] & 0xFF;
            int b3 = src[off + 5 * i + 3] & 0xFF;
            int b4 = src[off + 5 * i + 4] & 0xFF;
            r[4 * i] = (b0 | (b1 << 8)) & 0x3FF;
            r[4 * i + 1] = ((b1 >> 2) | (b2 << 6)) & 0x3FF;
            r[4 * i + 2] = ((b2 >> 4) | (b3 << 4)) & 0x3FF;
            r[4 * i + 3] = ((b3 >> 6) | (b4 << 2)) & 0x3FF;
        }
        return r;
    }

    private static int[] unpackZ(byte[] src, int off) {
        int[] r = new int[N];
        for (int i = 0; i < N / 2; i++) {
            int b0 = src[off + 5 * i] & 0xFF;
            int b1 = src[off + 5 * i + 1] & 0xFF;
            int b2 = src[off + 5 * i + 2] & 0xFF;
            int b3 = src[off + 5 * i + 3] & 0xFF;
            int b4 = src[off + 5 * i + 4] & 0xFF;
            r[2 * i] = GAMMA1 - ((b0 | (b1 << 8) | (b2 << 16)) & 0xFFFFF);
            r[2 * i + 1] = GAMMA1 - (((b2 >> 4) | (b3 << 4) | (b4 << 12)) & 0xFFFFF);
        }
        return r;
    }

    private static int[] buildZetas() {
        int[] z = new int[N];
        long mont = (1L << 32) % Q;
        for (int k = 0; k < N; k++) {
            int rev = Integer.reverse(k) >>> 24;
            long v = mont * modPow(1753, rev) % Q;
            if (v > Q / 2) v -= Q;
            z[k] = (int) v;
        }
        z[0] = 0;
        return z;
    }

    private static long modPow(long base, int exp) {
        long r = 1;
        long b = base % Q;
        int e = exp;
        while (e > 0) {
            if ((e & 1) != 0) r = r * b % Q;
            b = b * b % Q;
            e >>= 1;
        }
        return r;
    }

    private static int montgomeryReduce(long a) {
        int t = (int) a * QINV;
        return (int) ((a - (long) t * Q) >> 32);
    }

    private static int reduce32(int a) {
        int t = (a + (1 << 22)) >> 23;
        return a - t * Q;
    }

    private static void reduce(int[] a) {
        for (int i = 0; i < N; i++) a[i] = reduce32(a[i]);
    }

    private static void caddq(int[] a) {
        for (int i = 0; i < N; i++) a[i] += (a[i] >> 31) & Q;
    }

    private static void ntt(int[] a) {
        int k = 0;
        for (int len = 128; len > 0; len >>= 1) {
            for (int start = 0; start < N; start += 2 * len) {
                int zeta = ZETAS[++k];
                for (int j = start; j < start + len; j++) {
                    int t = montgomeryReduce((long) zeta * a[j + len]);
                    a[j + len] = a[j] - t;
                    a[j] = a[j] + t;
                }
            }
        }
    }

    private static void invnttTomont(int[] a) {
        final int f = 41978;
        int k = 256;
        for (int len = 1; len < N; len <<= 1) {
            for (int start = 0; start < N; start += 2 * len) {
                int zeta = -ZETAS[--k];
                for (int j = start; j < start + len; j++) {
                    int t = a[j];
                    a[j] = t + a[j + len];
                    a[j + len] = t - a[j + len];
                    a[j + len] = montgomeryReduce((long) zeta * a[j + len]);
                }
            }
        }
        for (int j = 0; j < N; j++) a[j] = montgomeryReduce((long) f * a[j]);
    }

    private static int[] pointwise(int[] a, int[] b) {
        int[] c = new int[N];
        for (int i = 0; i < N; i++) c[i] = montgomeryReduce((long) a[i] * b[i]);
        return c;
    }

    private static boolean chknorm(int[] a, int bound) {
        if (bound > (Q - 1) / 8) return true;
        for (int i = 0; i < N; i++) {
            int t = a[i] >> 31;
            t = a[i] - (t & 2 * a[i]);
            if (t >= bound) return true;
        }
        return false;
    }

    private static int[] decompose(int a) {
        int a1 = (a + 127) >> 7;
        a1 = (a1 * 1025 + (1 << 21)) >> 22;
        a1 &= 15;
        int a0 = a - a1 * 2 * GAMMA2;
        a0 -= (((Q - 1) / 2 - a0) >> 31) & Q;
        return new int[] {a1, a0};
    }

    private static int[] uniform(byte[] rho, int nonce) {
        SHAKEDigest xof = new SHAKEDigest(128);
        xof.update(rho, 0, rho.length);
        xof.update((byte) nonce);
        xof.update((byte) (nonce >> 8));
        int[] a = new int[N];
        byte[] buf = new byte[3];
        int ctr = 0;
        while (ctr < N) {
            xof.doOutput(buf, 0, 3);
            int t = (buf[0] & 0xFF) | ((buf[1] & 0xFF) << 8) | ((buf[2] & 0xFF) << 16);
            t &= 0x7FFFFF;
            if (t < Q) a[ctr++] = t;
        }
        return a;
    }

    private static int[] uniformGamma1(byte[] seed, int nonce) {
        SHAKEDigest xof = new SHAKEDigest(256);
        xof.update(seed, 0, seed.length);
        xof.update((byte) nonce);
        xof.update((byte) (nonce >> 8));
        byte[] buf = new byte[POLYZ_PACKEDBYTES];
        xof.doOutput(buf, 0, buf.length);
        int[] r = new int[N];
        for (int i = 0; i < N / 2; i++) {
            int b0 = buf[5 * i] & 0xFF, b1 = buf[5 * i + 1] & 0xFF, b2 = buf[5 * i + 2] & 0xFF;
            int b3 = buf[5 * i + 3] & 0xFF, b4 = buf[5 * i + 4] & 0xFF;
            r[2 * i] = GAMMA1 - ((b0 | (b1 << 8) | (b2 << 16)) & 0xFFFFF);
            r[2 * i + 1] = GAMMA1 - (((b2 >> 4) | (b3 << 4) | (b4 << 12)) & 0xFFFFF);
        }
        return r;
    }

    private static int[] challenge(byte[] seed) {
        SHAKEDigest xof = new SHAKEDigest(256);
        xof.update(seed, 0, seed.length);
        byte[] buf = new byte[8];
        xof.doOutput(buf, 0, 8);
        long signs = 0;
        for (int i = 0; i < 8; i++) signs |= (long) (buf[i] & 0xFF) << (8 * i);
        int[] c = new int[N];
        byte[] one = new byte[1];
        for (int i = N - TAU; i < N; i++) {
            int b;
            do {
                xof.doOutput(one, 0, 1);
                b = one[0] & 0xFF;
            } while (b > i);
            c[i] = c[b];
            c[b] = (int) (1 - 2 * (signs & 1));
            signs >>>= 1;
        }
        return c;
    }

    private static int[] unpackEta(byte[] a, int off) {
        int[] r = new int[N];
        for (int i = 0; i < N / 2; i++) {
            int b = a[off + i] & 0xFF;
            r[2 * i] = ETA - (b & 0x0F);
            r[2 * i + 1] = ETA - (b >> 4);
        }
        return r;
    }

    private static int[] unpackT0(byte[] src, int off) {
        int[] r = new int[N];
        for (int i = 0; i < N / 8; i++) {
            int[] a = new int[13];
            for (int j = 0; j < 13; j++) a[j] = src[off + 13 * i + j] & 0xFF;
            r[8 * i] = (a[0] | (a[1] << 8)) & 0x1FFF;
            r[8 * i + 1] = ((a[1] >> 5) | (a[2] << 3) | (a[3] << 11)) & 0x1FFF;
            r[8 * i + 2] = ((a[3] >> 2) | (a[4] << 6)) & 0x1FFF;
            r[8 * i + 3] = ((a[4] >> 7) | (a[5] << 1) | (a[6] << 9)) & 0x1FFF;
            r[8 * i + 4] = ((a[6] >> 4) | (a[7] << 4) | (a[8] << 12)) & 0x1FFF;
            r[8 * i + 5] = ((a[8] >> 1) | (a[9] << 7)) & 0x1FFF;
            r[8 * i + 6] = ((a[9] >> 6) | (a[10] << 2) | (a[11] << 10)) & 0x1FFF;
            r[8 * i + 7] = ((a[11] >> 3) | (a[12] << 5)) & 0x1FFF;
            for (int j = 0; j < 8; j++) r[8 * i + j] = (1 << (D - 1)) - r[8 * i + j];
        }
        return r;
    }

    private static void packZ(int[] a, byte[] r, int off) {
        for (int i = 0; i < N / 2; i++) {
            int t0 = GAMMA1 - a[2 * i];
            int t1 = GAMMA1 - a[2 * i + 1];
            r[off + 5 * i] = (byte) t0;
            r[off + 5 * i + 1] = (byte) (t0 >> 8);
            r[off + 5 * i + 2] = (byte) ((t0 >> 16) | (t1 << 4));
            r[off + 5 * i + 3] = (byte) (t1 >> 4);
            r[off + 5 * i + 4] = (byte) (t1 >> 12);
        }
    }

    private static byte[] shake256(int outLen, byte[]... parts) {
        SHAKEDigest xof = new SHAKEDigest(256);
        for (byte[] p : parts) xof.update(p, 0, p.length);
        byte[] out = new byte[outLen];
        xof.doFinal(out, 0, outLen);
        return out;
    }
}
