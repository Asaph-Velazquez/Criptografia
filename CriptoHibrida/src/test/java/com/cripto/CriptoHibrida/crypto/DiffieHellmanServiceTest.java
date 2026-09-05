package com.cripto.CriptoHibrida.crypto;

import com.cripto.CriptoHibrida.crypto.model.DhParameters;
import com.cripto.CriptoHibrida.crypto.service.DiffieHellmanService;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DiffieHellmanServiceTest {
    // Public 2048-bit MODP group parameters (RFC 3526 group 14).
    private static final BigInteger PRIME = new BigInteger(
            "FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD1"
            + "29024E088A67CC74020BBEA63B139B22514A08798E3404DDEF"
            + "9519B3CD3A431B302B0A6DF25F14374FE1356D6D51C245"
            + "E485B576625E7EC6F44C42E9A637ED6B0BFF5CB6F406B7ED"
            + "EE386BFB5A899FA5AE9F24117C4B1FE649286651ECE45B3DC"
            + "2007CB8A163BF0598DA48361C55D39A69163FA8FD24CF5F"
            + "83655D23DCA3AD961C62F356208552BB9ED529077096966D"
            + "670C354E4ABC9804F1746C08CA18217C32905E462E36CE3BE"
            + "39E772C180E86039B2783A2EC07A28FB5C55DF06F4C52C9"
            + "DE2BCBF6955817183995497CEA956AE515D2261898FA051015"
            + "728E5A8AACAA68FFFFFFFFFFFFFFFF", 16);
    private static final DhParameters PARAMETERS = new DhParameters(BigInteger.TWO, PRIME);
    private final DiffieHellmanService service = new DiffieHellmanService();

    @Test
    void independentAgreementsProduceIdenticalSecretsAndDerivedBytes() throws Exception {
        BigInteger a = service.generatePrivateKey(PARAMETERS);
        BigInteger b = service.generatePrivateKey(PARAMETERS);
        BigInteger c = service.generatePrivateKey(PARAMETERS);
        BigInteger d = service.generatePrivateKey(PARAMETERS);
        BigInteger keySecret = agree(a, b);
        BigInteger ivSecret = agree(c, d);
        assertEquals(32, service.deriveAesKey(keySecret).length);
        assertEquals(16, service.deriveIv(ivSecret).length);
        assertArrayEquals(MessageDigest.getInstance("SHA-256").digest(keySecret.toByteArray()),
                service.deriveAesKey(keySecret));
        assertArrayEquals(Arrays.copyOf(MessageDigest.getInstance("SHA-256")
                .digest(ivSecret.toByteArray()), 16), service.deriveIv(ivSecret));
    }

    private BigInteger agree(BigInteger local, BigInteger peer) {
        assertTrue(local.compareTo(BigInteger.TWO) >= 0);
        assertTrue(local.compareTo(PRIME.subtract(BigInteger.TWO)) <= 0);
        BigInteger localPublic = service.computePublicKey(PARAMETERS, local);
        assertEquals(BigInteger.TWO.modPow(local, PRIME), localPublic);
        BigInteger first = service.computeSharedSecret(PARAMETERS, local,
                service.computePublicKey(PARAMETERS, peer));
        BigInteger second = service.computeSharedSecret(PARAMETERS, peer, localPublic);
        assertEquals(first, second);
        assertArrayEquals(service.deriveAesKey(first), service.deriveAesKey(second));
        assertArrayEquals(service.deriveIv(first), service.deriveIv(second));
        return first;
    }

    @Test
    void hashingPreservesBigIntegerSignByte() throws Exception {
        BigInteger secret = BigInteger.valueOf(128);
        assertArrayEquals(new byte[]{0, (byte) 128}, secret.toByteArray());
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray());
        assertArrayEquals(hash, service.deriveAesKey(secret));
        assertArrayEquals(Arrays.copyOf(hash, 16), service.deriveIv(secret));
    }

    @Test
    void rejectsInvalidParametersAndKeys() {
        assertThrows(IllegalArgumentException.class,
                () -> new DhParameters(BigInteger.TWO, BigInteger.valueOf(23)));
        assertThrows(IllegalArgumentException.class,
                () -> new DhParameters(BigInteger.TWO, PRIME.multiply(BigInteger.TWO)));
        assertThrows(IllegalArgumentException.class,
                () -> new DhParameters(BigInteger.ONE, PRIME));
        for (BigInteger invalid : new BigInteger[]{BigInteger.ZERO, BigInteger.ONE,
                PRIME.subtract(BigInteger.ONE), PRIME}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.computePublicKey(PARAMETERS, invalid));
            assertThrows(IllegalArgumentException.class,
                    () -> service.computeSharedSecret(PARAMETERS, BigInteger.TWO, invalid));
        }
        assertThrows(IllegalArgumentException.class, () -> service.deriveIv(BigInteger.ZERO));
    }
}
