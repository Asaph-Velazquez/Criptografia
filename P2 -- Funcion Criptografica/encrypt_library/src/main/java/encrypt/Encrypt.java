package encrypt;

import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.Cipher;

public class Encrypt {

    private static final int RSA_KEY_SIZE = 2048;
    private static final int ENCRYPT_BLOCK_SIZE = (RSA_KEY_SIZE / 8) - 11; // 245 bytes
    private static final int DECRYPT_BLOCK_SIZE = RSA_KEY_SIZE / 8;        // 256 bytes
    
    private PublicKey publicKey;
    private PrivateKey privateKey;

    public Encrypt() {
    }

    public void generateKeys() throws Exception {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");

        keyGen.initialize(RSA_KEY_SIZE);

        KeyPair pair = keyGen.generateKeyPair();

        publicKey = pair.getPublic();
        privateKey = pair.getPrivate();
        
    }
    
    public byte[] encrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, this.getPublicKey());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int offset = 0; offset < data.length; offset += ENCRYPT_BLOCK_SIZE) {
            int len = Math.min(ENCRYPT_BLOCK_SIZE, data.length - offset);
            out.write(cipher.doFinal(data, offset, len));
        }
        return out.toByteArray();
    }

    public byte[] decrypt(byte[] data) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, this.getPrivateKey());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int offset = 0; offset < data.length; offset += DECRYPT_BLOCK_SIZE) {
            int len = Math.min(DECRYPT_BLOCK_SIZE, data.length - offset);
            out.write(cipher.doFinal(data, offset, len));
        }
        return out.toByteArray();
    }
    
    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPublicKey(PublicKey key) {
        this.publicKey = key;
    }

    public void setPrivateKey(PrivateKey key) {
        this.privateKey = key;
    }
    
}