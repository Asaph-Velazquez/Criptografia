package com.cripto.CriptoHibrida.crypto;

import com.cripto.CriptoHibrida.crypto.model.*;
import com.cripto.CriptoHibrida.crypto.model.VerificationResult.IntegrityStatus;
import com.cripto.CriptoHibrida.crypto.service.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class HybridCryptoOrchestratorTest {
    private static DhParameters parameters;
    private static byte[] privatePem;
    private static byte[] publicPem;
    private static byte[] wrongPublicPem;
    private static final BigInteger B = BigInteger.valueOf(1234567);
    private static final BigInteger D = BigInteger.valueOf(7654321);
    private final DiffieHellmanService dh = new DiffieHellmanService();
    private final PackageManagerService packages = new PackageManagerService();
    private final HybridCryptoOrchestrator service = new HybridCryptoOrchestrator(
            dh, new AesCipherService(), new RsaSignerService(), packages);

    @BeforeAll
    static void setup() throws Exception {
        parameters = new DhParameters(BigInteger.TWO, BigInteger.probablePrime(2048, new SecureRandom()));
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keys = generator.generateKeyPair();
        privatePem = pem("PRIVATE KEY", keys.getPrivate().getEncoded());
        publicPem = pem("PUBLIC KEY", keys.getPublic().getEncoded());
        wrongPublicPem = pem("PUBLIC KEY", generator.generateKeyPair().getPublic().getEncoded());
    }

    private static byte[] pem(String label, byte[] bytes) {
        return ("-----BEGIN " + label + "-----\n" + Base64.getEncoder().encodeToString(bytes)
                + "\n-----END " + label + "-----").getBytes(StandardCharsets.US_ASCII);
    }

    private byte[] emit(byte[] file, boolean encrypt, boolean sign) throws Exception {
        return service.emit(file, "Ana", "multimedia.mp4", new ProcessOptions(encrypt, sign, false, false),
                encrypt ? parameters : null, encrypt ? dh.computePublicKey(parameters, B) : null,
                encrypt ? dh.computePublicKey(parameters, D) : null, sign ? privatePem : null);
    }

    private VerificationResult receive(byte[] json, boolean decrypt, boolean verify, byte[] publicKey) throws Exception {
        return service.receive(json, new ProcessOptions(false, false, decrypt, verify),
                decrypt ? parameters : null, decrypt ? B : null, decrypt ? D : null, publicKey);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 16, 256, 1048576})
    void recoversOriginalBytesInAllThreeModes(int size) throws Exception {
        byte[] data = new byte[size];
        new Random(42).nextBytes(data);
        byte[] copy = data.clone();
        for (int mode = 1; mode <= 3; mode++) {
            boolean encrypt = (mode & 1) != 0;
            boolean sign = (mode & 2) != 0;
            byte[] json = emit(data, encrypt, sign);
            VerificationResult result = receive(json, encrypt, sign, sign ? publicPem : null);
            assertArrayEquals(data, result.file());
            assertEquals(sign ? IntegrityStatus.VERIFIED : IntegrityStatus.NOT_REQUESTED, result.integrity());
            assertEquals("multimedia.mp4", result.nombreArchivo());
            assertArrayEquals(copy, data);
            EncryptedPackage value = packages.deserialize(json);
            assertEquals(encrypt, value.dhPublicK() != null);
            assertEquals(sign, value.firma() != null);
        }
    }

    @Test
    void freshExchangesAndIndependentSecretsAreUsedPerEmission() throws Exception {
        EncryptedPackage first = packages.deserialize(emit(new byte[32], true, true));
        EncryptedPackage second = packages.deserialize(emit(new byte[32], true, true));
        assertNotEquals(first.dhPublicK(), second.dhPublicK());
        assertNotEquals(first.dhPublicIv(), second.dhPublicIv());
        assertNotEquals(first.dhPublicK(), first.dhPublicIv());
        byte[] key = dh.deriveAesKey(dh.computeSharedSecret(parameters, B, first.dhPublicK()));
        byte[] iv = dh.deriveIv(dh.computeSharedSecret(parameters, D, first.dhPublicIv()));
        assertArrayEquals(new byte[32], new AesCipherService().decrypt(first.ciphertext(), key, iv));
    }

    @Test
    void neverReleasesFileWhenSignatureOrDataIsTampered() throws Exception {
        for (boolean encrypt : new boolean[]{false, true}) {
            EncryptedPackage value = packages.deserialize(emit(new byte[64], encrypt, true));
            byte[] modifiedData = value.ciphertext();
            modifiedData[0] ^= 1; // With CBC this preserves padding but invalidates the signature.
            byte[] modifiedSignature = value.firma();
            modifiedSignature[0] ^= 1;
            for (EncryptedPackage changed : new EncryptedPackage[]{
                    new EncryptedPackage(value.remitente(), value.nombreArchivo(), value.dhPublicK(),
                            value.dhPublicIv(), modifiedData, value.firma()),
                    new EncryptedPackage(value.remitente(), value.nombreArchivo(), value.dhPublicK(),
                            value.dhPublicIv(), value.ciphertext(), modifiedSignature)}) {
                VerificationResult result = receive(packages.serialize(changed), encrypt, true, publicPem);
                assertEquals(IntegrityStatus.FAILED, result.integrity());
                assertNull(result.file());
            }
        }
        VerificationResult wrongKey = receive(emit(new byte[10], true, true), true, true, wrongPublicPem);
        assertEquals(IntegrityStatus.FAILED, wrongKey.integrity());
        assertNull(wrongKey.file());
    }

    @Test
    void rejectsCorruptCiphertextWithoutReturningBytes() throws Exception {
        EncryptedPackage value = packages.deserialize(emit(new byte[64], true, false));
        byte[] truncated = Arrays.copyOf(value.ciphertext(), value.ciphertext().length - 1);
        byte[] json = packages.serialize(new EncryptedPackage(value.remitente(), value.nombreArchivo(),
                value.dhPublicK(), value.dhPublicIv(), truncated, null));
        VerificationResult result = receive(json, true, false, null);
        assertEquals(IntegrityStatus.FAILED, result.integrity());
        assertNull(result.file());
    }

    @Test
    void rejectsMissingDependenciesAndIncompatibleModes() throws Exception {
        byte[] signed = emit(new byte[1], false, true);
        byte[] encrypted = emit(new byte[1], true, false);
        assertThrows(IllegalArgumentException.class, () -> receive(encrypted, false, true, publicPem));
        assertThrows(IllegalArgumentException.class, () -> receive(signed, true, true, publicPem));
        assertThrows(IllegalArgumentException.class, () -> receive(encrypted, true, true, publicPem));
        assertThrows(IllegalArgumentException.class, () -> receive(signed, false, true, null));
        assertThrows(IllegalArgumentException.class, () -> service.emit(new byte[1], "Ana", "a",
                new ProcessOptions(false, true, false, false), null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.emit(new byte[1], "Ana", "a",
                new ProcessOptions(true, false, false, false), null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> service.receive(encrypted,
                new ProcessOptions(false, false, true, false), parameters, null, D, null));
        assertThrows(IllegalArgumentException.class, () -> service.receive(signed,
                new ProcessOptions(false, true, false, false), null, null, null, null));
    }

    @Test
    void acceptsOnlyOneOrTwoOperationsFromTheSameDirection() {
        for (int mask = 0; mask < 16; mask++) {
            final int selection = mask;
            if (mask == 1 || mask == 2 || mask == 3 || mask == 4 || mask == 8 || mask == 12) {
                assertDoesNotThrow(() -> options(selection));
            } else {
                assertThrows(IllegalArgumentException.class, () -> options(selection));
            }
        }
    }

    private ProcessOptions options(int mask) {
        return new ProcessOptions((mask & 1) != 0, (mask & 2) != 0, (mask & 4) != 0, (mask & 8) != 0);
    }

    @Test
    void decryptOnlyNeverClaimsSignatureVerificationAndResultsAreDefensive() throws Exception {
        VerificationResult result = receive(emit(new byte[]{1, 2, 3}, true, true), true, false, null);
        assertEquals(IntegrityStatus.NOT_REQUESTED, result.integrity());
        result.file()[0] = 9;
        assertArrayEquals(new byte[]{1, 2, 3}, result.file());
    }
}
