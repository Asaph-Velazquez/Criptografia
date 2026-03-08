package encrypt;

import java.io.FileWriter;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class KeyManager {

    public static void saveKeys(BigInteger n, BigInteger e, BigInteger d) throws Exception {

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String publicFile = "public_key_" + timestamp + ".key";
        String privateFile = "private_key_" + timestamp + ".key";

        writeKey(publicFile, n, e);
        writeKey(privateFile, n, d);
    }

    private static void writeKey(String fileName, BigInteger n, BigInteger exp) throws Exception {

        FileWriter writer = new FileWriter(fileName);

        writer.write(n.toString(16) + "\n");
        writer.write(exp.toString(16));
        writer.close();
        
    }

    public static BigInteger[] loadPublicKey(String path) throws Exception {

        String[] lines = Files.readString(Path.of(path)).split("\n");

        BigInteger n = new BigInteger(lines[0],16);
        BigInteger e = new BigInteger(lines[1],16);
        
        return new BigInteger[]{n,e};
        
    }

    public static BigInteger[] loadPrivateKey(String path) throws Exception {

        String[] lines = Files.readString(Path.of(path)).split("\n");

        BigInteger n = new BigInteger(lines[0],16);
        BigInteger d = new BigInteger(lines[1],16);
        
        return new BigInteger[]{n,d};
        
    }
    
}