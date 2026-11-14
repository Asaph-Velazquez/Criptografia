package com.cripto.CriptoHibrida.crypto;

import com.cripto.CriptoHibrida.crypto.service.EcdsaSignerService;
import com.cripto.CriptoHibrida.crypto.util.PemUtils;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EcdsaSignerServiceTest {
    private final EcdsaSignerService service = new EcdsaSignerService();

    private KeyPair keys(String curve) throws Exception {
        var generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec(curve));
        return generator.generateKeyPair();
    }

    private byte[] pem(String label, byte[] value) {
        return ("-----BEGIN " + label + "-----\n" + Base64.getEncoder().encodeToString(value)
                + "\n-----END " + label + "-----").getBytes(StandardCharsets.US_ASCII);
    }

    @Test
    void readsPkcs8AndSpkiAndSignsOriginalBinaryBytes() throws Exception {
        var keys = keys("secp256r1");
        var privateKey = PemUtils.parsePrivateKey(pem("PRIVATE KEY", keys.getPrivate().getEncoded()), "EC");
        var publicKey = PemUtils.parsePublicKey(pem("PUBLIC KEY", keys.getPublic().getEncoded()), "EC");
        for (int size : new int[]{0, 1, 256, 1048576}) {
            byte[] bytes = new byte[size];
            new java.util.Random(42).nextBytes(bytes);
            byte[] signed = service.sign(bytes, privateKey);
            assertTrue(service.verify(bytes, signed, publicKey));
            assertFalse(service.verify(bytes, signed, keys("secp256r1").getPublic()));
            if (size > 0) {
                bytes[0] ^= 1;
                assertFalse(service.verify(bytes, signed, publicKey));
            }
        }
    }

    @Test
    void rejectsMalformedSignaturesAndUnsupportedKeys() throws Exception {
        var keys = keys("secp256r1");
        for (byte[] invalid : new byte[][]{null, {}, {0}, new byte[64], new byte[73]}) {
            assertFalse(service.verify(new byte[]{1}, invalid, keys.getPublic()));
        }
        var wrongCurve = keys("secp384r1");
        assertThrows(InvalidKeyException.class, () -> service.sign(new byte[0], wrongCurve.getPrivate()));
        assertThrows(InvalidKeyException.class, () -> service.verify(new byte[0], new byte[0], wrongCurve.getPublic()));
        var rsa = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        assertThrows(InvalidKeyException.class, () -> service.sign(new byte[0], rsa.getPrivate()));
    }
}
