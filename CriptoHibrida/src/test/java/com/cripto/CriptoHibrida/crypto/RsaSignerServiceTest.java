package com.cripto.CriptoHibrida.crypto;

import com.cripto.CriptoHibrida.crypto.service.RsaSignerService;
import com.cripto.CriptoHibrida.crypto.util.PemUtils;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class RsaSignerServiceTest {
    private static KeyPair keys;
    private static KeyPair otherKeys;
    private final RsaSignerService service = new RsaSignerService();

    @BeforeAll
    static void createKeys() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
        otherKeys = generator.generateKeyPair();
    }

    private static byte[] pem(String label, byte[] der) {
        return ("-----BEGIN " + label + "-----\r\n"
                + Base64.getMimeEncoder(64, new byte[]{13, 10}).encodeToString(der)
                + "\r\n-----END " + label + "-----\r\n").getBytes(StandardCharsets.US_ASCII);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 256, 1048576})
    void signsOriginalBinaryBytesWithLoadedKeys(int size) throws Exception {
        byte[] data = new byte[size];
        new Random(42).nextBytes(data);
        byte[] copy = data.clone();
        var privateKey = PemUtils.parsePrivateKey(pem("PRIVATE KEY", keys.getPrivate().getEncoded()));
        var publicKey = PemUtils.parsePublicKey(pem("PUBLIC KEY", keys.getPublic().getEncoded()));
        byte[] signed = service.sign(data, privateKey);
        assertTrue(service.verify(data, signed, publicKey));
        Signature independent = Signature.getInstance("SHA256withRSA");
        independent.initVerify(keys.getPublic());
        independent.update(data);
        assertTrue(independent.verify(signed));
        independent.initSign(keys.getPrivate());
        independent.update(data);
        assertTrue(service.verify(data, independent.sign(), publicKey));
        assertArrayEquals(copy, data);
    }

    @Test
    void rejectsTamperedDataSignaturesAndDifferentPublicKey() throws Exception {
        byte[] data = {0, -1, 4, 5};
        byte[] signed = service.sign(data, keys.getPrivate());
        assertFalse(service.verify(data, signed, otherKeys.getPublic()));
        data[0] ^= 1;
        assertFalse(service.verify(data, signed, keys.getPublic()));
        data[0] ^= 1;
        signed[10] ^= 1;
        assertFalse(service.verify(data, signed, keys.getPublic()));
        assertFalse(service.verify(data, Arrays.copyOf(signed, 10), keys.getPublic()));
        assertFalse(service.verify(data, new byte[256], keys.getPublic()));
        assertFalse(service.verify(data, null, keys.getPublic()));
    }

    @Test
    void hashesOriginalBytesUsingKnownSha256Vector() throws Exception {
        assertArrayEquals(HexFormat.of().parseHex(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"),
                service.sha256(new byte[]{97, 98, 99}));
    }

    @Test
    void rejectsMalformedUnsupportedAndNonRsaPem() throws Exception {
        for (byte[] invalid : new byte[][]{null, new byte[0], new byte[]{-1}, new byte[65537],
                pem("RSA PRIVATE KEY", keys.getPrivate().getEncoded()),
                pem("ENCRYPTED PRIVATE KEY", keys.getPrivate().getEncoded()),
                pem("PRIVATE KEY", new byte[]{1, 2, 3}),
                "-----BEGIN PRIVATE KEY-----\n!\n-----END PRIVATE KEY-----".getBytes(StandardCharsets.US_ASCII)}) {
            assertThrows(InvalidKeySpecException.class, () -> PemUtils.parsePrivateKey(invalid));
        }
        byte[] publicPem = pem("PUBLIC KEY", keys.getPublic().getEncoded());
        byte[] twoBlocks = new byte[publicPem.length * 2];
        System.arraycopy(publicPem, 0, twoBlocks, 0, publicPem.length);
        System.arraycopy(publicPem, 0, twoBlocks, publicPem.length, publicPem.length);
        assertThrows(InvalidKeySpecException.class, () -> PemUtils.parsePublicKey(twoBlocks));
        assertThrows(InvalidKeySpecException.class, () -> PemUtils.parsePrivateKey(publicPem));
        KeyPair ec = KeyPairGenerator.getInstance("EC").generateKeyPair();
        assertThrows(InvalidKeySpecException.class,
                () -> PemUtils.parsePublicKey(pem("PUBLIC KEY", ec.getPublic().getEncoded())));
        assertThrows(InvalidKeyException.class, () -> service.sign(new byte[0], ec.getPrivate()));
        assertThrows(InvalidKeyException.class, () -> service.verify(new byte[0], new byte[0], ec.getPublic()));
    }
}
