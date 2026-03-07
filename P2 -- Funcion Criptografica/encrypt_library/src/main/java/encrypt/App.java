package encrypt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import javax.crypto.Cipher;

public final class App {
    
    private static final int RSA_KEY_SIZE = 2048;
    
    private App() {
    }
    public static void main(String[] args) throws Exception {
        //Archive txt file
        Path input = Path.of("src/main/resources/BaseTXT/p.txt");

        Path encryptedFile = Path.of("src/main/resources/EncryptTXT/p-e.txt");
        Path decryptedFile = Path.of("src/main/resources/DecryptTXT/p-e-d.txt");

        // Read file data
        byte[] data = Files.readAllBytes(input);

        // generate RSA key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(RSA_KEY_SIZE);
        KeyPair keys = keyGen.generateKeyPair();

        // Encrypt data
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.ENCRYPT_MODE, keys.getPublic());
        byte[] encrypted = cipher.doFinal(data);

        Files.write(encryptedFile, encrypted);

        // Decrypt data
        cipher.init(Cipher.DECRYPT_MODE, keys.getPrivate());
        byte[] decrypted = cipher.doFinal(encrypted);

        Files.write(decryptedFile, decrypted);

        System.out.println("Proceso terminado");
    }
}
