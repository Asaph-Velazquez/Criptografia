package encrypt;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
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

    public Encrypt(KeyPair keys) {
        this.keys = keys;
    }

    /** Guarda el par de llaves en un archivo binario .key */
    public void saveKeys(String path) throws Exception {
        byte[] privateBytes = keys.getPrivate().getEncoded(); // PKCS8
        byte[] publicBytes  = keys.getPublic().getEncoded();  // X.509
        try (DataOutputStream dos = new DataOutputStream(new FileOutputStream(path))) {
            dos.writeInt(privateBytes.length);
            dos.write(privateBytes);
            dos.writeInt(publicBytes.length);
            dos.write(publicBytes);
        }
    }

    /** Carga un Encrypt desde un archivo .key generado por saveKeys() */
    public static Encrypt loadFromFile(String path) throws Exception {
        try (DataInputStream dis = new DataInputStream(new FileInputStream(path))) {
            int privLen = dis.readInt();
            byte[] privBytes = new byte[privLen];
            dis.readFully(privBytes);
            int pubLen = dis.readInt();
            byte[] pubBytes = new byte[pubLen];
            dis.readFully(pubBytes);

            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
            PublicKey  publicKey  = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
            return new Encrypt(new KeyPair(publicKey, privateKey));
        }
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