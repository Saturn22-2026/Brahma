import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumKeyPairGenerator;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumSigner;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyGenerationParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAKeyPairGenerator;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPrivateKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSAPublicKeyParameters;
import org.bouncycastle.pqc.crypto.mldsa.MLDSASigner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;

/** Write one Dilithium3 sample and one ML-DSA-87 sample for the Python checker. */
public class mldsa_roundtrip {
    static String hex(byte[] raw) {
        StringBuilder out = new StringBuilder(raw.length * 2);
        for (byte value : raw) out.append(String.format("%02x", value));
        return out.toString();
    }

    static void write(Path dir, String name, byte[] pk, byte[] sig, byte[] msg) throws Exception {
        Path folder = dir.resolve(name);
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("pk.hex"), hex(pk));
        Files.writeString(folder.resolve("sig.hex"), hex(sig));
        Files.writeString(folder.resolve("msg.hex"), hex(msg));
    }
    static boolean checkDilithium3() throws Exception {
        DilithiumKeyPairGenerator gen = new DilithiumKeyPairGenerator();
        gen.init(new DilithiumKeyGenerationParameters(new SecureRandom(), DilithiumParameters.dilithium3));
        AsymmetricCipherKeyPair pair = gen.generateKeyPair();
        DilithiumPublicKeyParameters pub = (DilithiumPublicKeyParameters) pair.getPublic();
        DilithiumPrivateKeyParameters priv = (DilithiumPrivateKeyParameters) pair.getPrivate();
        byte[] msg = "BRAH1|roundtrip".getBytes("UTF-8");
        DilithiumSigner signer = new DilithiumSigner();
        signer.init(true, priv);
        byte[] sig = signer.generateSignature(msg);
        write(Path.of("sig-samples"), "dilithium3", pub.getEncoded(), sig, msg);
        System.out.println("dilithium3 pk=" + pub.getEncoded().length + " sig=" + sig.length);
        return pub.getEncoded().length == 1952;
    }

    static boolean checkMlDsa87() throws Exception {
        MLDSAParameters params = MLDSAParameters.ml_dsa_87;
        MLDSAKeyPairGenerator gen = new MLDSAKeyPairGenerator();
        gen.init(new MLDSAKeyGenerationParameters(new SecureRandom(), params));
        AsymmetricCipherKeyPair pair = gen.generateKeyPair();
        MLDSAPublicKeyParameters pub = (MLDSAPublicKeyParameters) pair.getPublic();
        MLDSAPrivateKeyParameters priv = (MLDSAPrivateKeyParameters) pair.getPrivate();
        byte[] msg = "BRAH1|roundtrip".getBytes("UTF-8");
        MLDSASigner signer = new MLDSASigner();
        signer.init(true, priv);
        signer.update(msg, 0, msg.length);
        byte[] sig = signer.generateSignature();
        write(Path.of("sig-samples"), "mldsa87", pub.getEncoded(), sig, msg);
        System.out.println("mldsa87 pk=" + pub.getEncoded().length + " sig=" + sig.length);
        return pub.getEncoded().length == 2592;
    }

    public static void main(String[] args) throws Exception {
        if (!checkDilithium3() || !checkMlDsa87()) System.exit(1);
    }
}
