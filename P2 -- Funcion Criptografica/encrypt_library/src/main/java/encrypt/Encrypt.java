package encrypt;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.crypto.Cipher;

public class Encrypt {

    private static final int RSA_KEY_SIZE = 2048;
    private static final int ENCRYPT_BLOCK_SIZE = (RSA_KEY_SIZE / 8) - 11; // 245 bytes
    private static final int DECRYPT_BLOCK_SIZE = RSA_KEY_SIZE / 8;        // 256 bytes
    private KeyPair keys;

    public Encrypt() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(RSA_KEY_SIZE);
        keys = keyGen.generateKeyPair();
    }

    public byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, keys.getPublic());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int offset = 0; offset < data.length; offset += ENCRYPT_BLOCK_SIZE) {
            int len = Math.min(ENCRYPT_BLOCK_SIZE, data.length - offset);
            out.write(cipher.doFinal(data, offset, len));
        }
        return out.toByteArray();
    }

    public byte[] decrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int offset = 0; offset < data.length; offset += DECRYPT_BLOCK_SIZE) {
            int len = Math.min(DECRYPT_BLOCK_SIZE, data.length - offset);
            out.write(cipher.doFinal(data, offset, len));
        }
        return out.toByteArray();
    }
}
