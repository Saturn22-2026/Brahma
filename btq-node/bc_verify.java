import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumSigner;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Verify a wallet BouncyCastle Dilithium3 signature.
 * Run this class with bcprov 1.72 or 1.78.1, the jars that match phones already in use.
 * Args: pk.hex sig.hex msg.hex. Exit 0 if valid.
 */
public class bc_verify {
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

    public static void main(String[] args) {
        if (args.length != 3) System.exit(2);
        try {
            byte[] pk = hexFile(args[0]);
            byte[] sig = hexFile(args[1]);
            byte[] msg = hexFile(args[2]);
            if (pk.length > 1952) {
                byte[] raw = new byte[1952];
                System.arraycopy(pk, pk.length - 1952, raw, 0, 1952);
                pk = raw;
            }
            DilithiumSigner v = new DilithiumSigner();
            v.init(false, new DilithiumPublicKeyParameters(DilithiumParameters.dilithium3, pk));
            System.exit(v.verifySignature(msg, sig) ? 0 : 1);
        } catch (Exception e) {
            System.exit(1);
        }
    }
}
