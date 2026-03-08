package encrypt;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Arrays;


public class Encrypt {

    private static final int RSA_KEY_SIZE = 2048;
    private static final int ENCRYPT_BLOCK_SIZE = (RSA_KEY_SIZE / 8) - 1; // 245 bytes
    private static final int DECRYPT_BLOCK_SIZE = RSA_KEY_SIZE / 8;        // 256 bytes
    
    private BigInteger n;
    private BigInteger e;
    private BigInteger d;

    public Encrypt() {
    }

    public void generateKeys() throws Exception {

        SecureRandom random = new SecureRandom();

        BigInteger p = BigInteger.probablePrime(RSA_KEY_SIZE / 2, random);
        BigInteger q = BigInteger.probablePrime(RSA_KEY_SIZE / 2, random);

        n = p.multiply(q);

        BigInteger phi = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));

        e = BigInteger.valueOf(65537);

        while(!phi.gcd(e).equals(BigInteger.ONE)){
            e = e.add(BigInteger.TWO);
        }

        d = e.modInverse(phi);
        
    }
    
    public byte[] encrypt(byte[] data) throws Exception {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        for (int offset = 0; offset < data.length; offset += ENCRYPT_BLOCK_SIZE) {
            
            int len = Math.min(ENCRYPT_BLOCK_SIZE, data.length - offset);
            byte[] block = Arrays.copyOfRange(data, offset, offset + len);

            BigInteger m = new BigInteger(1, block);
            BigInteger c = m.modPow(e, n);

            byte[] encryptedBlock = toFixedSize(c.toByteArray(), DECRYPT_BLOCK_SIZE);

            out.write(encryptedBlock);
            
        }
        
        return out.toByteArray();
        
    }

    public byte[] decrypt(byte[] data) throws Exception {
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        for (int offset = 0; offset < data.length; offset += DECRYPT_BLOCK_SIZE) {
            
            int len = Math.min(DECRYPT_BLOCK_SIZE, data.length - offset);
            byte[] block = Arrays.copyOfRange(data, offset, offset + len);

            BigInteger c = new BigInteger(1, block);

            BigInteger m = c.modPow(d, n);

            byte[] decryptedBlock = toFixedSize(m.toByteArray(), ENCRYPT_BLOCK_SIZE);

            out.write(decryptedBlock);
            
        }
        
        return out.toByteArray();
        
    }
    
    private byte[] toFixedSize(byte[] data, int size){

        if(data.length == size)
            return data;

        byte[] result = new byte[size];

        if(data.length > size){
            System.arraycopy(data, data.length - size, result, 0, size);
        }else{
            System.arraycopy(data, 0, result, size - data.length, data.length);
        }

        return result;
        
    }
    
    public BigInteger getN(){ 
        return n; 
    }
    
    public BigInteger getE(){ 
        return e; 
    }
    
    public BigInteger getD(){ 
        return d; 
    }

    public void setPublicKey(BigInteger n, BigInteger e){
        this.n = n;
        this.e = e;
    }

    public void setPrivateKey(BigInteger n, BigInteger d){
        this.n = n;
        this.d = d;
    }
    
}