package encrypt;

import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class KeyManager {

    public static void saveKeys(PublicKey publicKey, PrivateKey privateKey) throws Exception {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String publicFile = "public_key_" + timestamp + ".key";
        String privateFile = "private_key_" + timestamp + ".key";

        writeKey(publicFile, publicKey.getEncoded());
        writeKey(privateFile, privateKey.getEncoded());
    }

    private static void writeKey(String fileName, byte[] keyBytes) throws Exception {

        String base64 = Base64.getEncoder().encodeToString(keyBytes);
        FileWriter writer = new FileWriter(fileName);

        writer.write(base64);
        writer.close();
        
    }

    public static PublicKey loadPublicKey(String path) throws Exception {

        String base64 = Files.readString(Path.of(path));
        byte[] keyBytes = Base64.getDecoder().decode(base64);

        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        
        return factory.generatePublic(spec);
        
    }

    public static PrivateKey loadPrivateKey(String path) throws Exception {

        String base64 = Files.readString(Path.of(path));
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        
        return factory.generatePrivate(spec);
        
    }
    
}