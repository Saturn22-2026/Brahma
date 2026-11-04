import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumSigner;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verify with BouncyCastle 1.85.
 * 2592-byte keys are FIPS 204 ML-DSA-87 in pure mode (CNSA 2.0).
 * Other keys are Dilithium3. HashML-DSA is not accepted.
 * Args: pk.hex sig.hex msg.hex. Exit 0 if valid.
 */
public class bc_mldsa {
    static final int DILITHIUM3_PK = 1952;
    static final int MLDSA87_PK = 2592;

    static byte[] hexFile(String path) throws Exception {
        String s = Files.readString(Path.of(path)).trim().replace("\r", "").replace("\n", "");
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        int n = s.length();
        byte[] out = new byte[n / 2];
        for (int i = 0; i < n; i += 2) {
            out[i / 2] = (byte) Integer.parseInt(s.substring(i, i + 2), 16);
        }
        return out;
    }

    static boolean dilithium3(byte[] pk, byte[] msg, byte[] sig) {
        try {
            DilithiumSigner v = new DilithiumSigner();
            v.init(false, new DilithiumPublicKeyParameters(DilithiumParameters.dilithium3, pk));
            return v.verifySignature(msg, sig);
        } catch (Exception e) {
            return false;
        }
    }

    static boolean mlDsa87(byte[] pk, byte[] msg, byte[] sig) {
        try {
            MLDSASigner v = new MLDSASigner();
            v.init(false, new MLDSAPublicKeyParameters(MLDSAParameters.ml_dsa_87, pk));
            v.update(msg, 0, msg.length);
            return v.verifySignature(sig);
        } catch (Exception e) {
            return false;
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) System.exit(2);
        try {
            byte[] pk = hexFile(args[0]);
            byte[] sig = hexFile(args[1]);
            byte[] msg = hexFile(args[2]);
            boolean ok;
            if (pk.length == MLDSA87_PK) {
                ok = mlDsa87(pk, msg, sig);
            } else if (pk.length == DILITHIUM3_PK) {
                ok = dilithium3(pk, msg, sig);
            } else if (pk.length > DILITHIUM3_PK) {
                byte[] raw = new byte[DILITHIUM3_PK];
                System.arraycopy(pk, pk.length - DILITHIUM3_PK, raw, 0, DILITHIUM3_PK);
                ok = dilithium3(raw, msg, sig);
            } else {
                ok = false;
            }
            System.exit(ok ? 0 : 1);
        } catch (Exception e) {
            System.exit(1);
        }
    }
}
