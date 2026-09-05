package com.cripto.CriptoHibrida.crypto;

import com.cripto.CriptoHibrida.crypto.model.EncryptedPackage;
import com.cripto.CriptoHibrida.crypto.service.PackageManagerService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PackageManagerServiceTest {
    private final PackageManagerService service = new PackageManagerService();
    private static final String VALID = """
            {"remitente":"Ana","nombre_archivo":"archivo.mp4","dh_public_k":"123",
            "dh_public_iv":"456","ciphertext":"AAH/","firma":"AQ=="}
            """;

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 256, 1048576})
    void preservesBinaryContentMetadataAndLargeDhValues(int size) {
        byte[] payload = new byte[size];
        new Random(42).nextBytes(payload);
        byte[] signature = new byte[256];
        for (int i = 0; i < signature.length; i++) {
            signature[i] = (byte) i;
        }
        BigInteger dh = BigInteger.ONE.shiftLeft(2047).add(BigInteger.valueOf(123));
        EncryptedPackage original = new EncryptedPackage("José", "música 🎵.mp4", dh,
                dh.add(BigInteger.TWO), payload, signature);
        EncryptedPackage restored = service.deserialize(service.serialize(original));
        assertEquals(original.remitente(), restored.remitente());
        assertEquals(original.nombreArchivo(), restored.nombreArchivo());
        assertEquals(original.dhPublicK(), restored.dhPublicK());
        assertEquals(original.dhPublicIv(), restored.dhPublicIv());
        assertArrayEquals(payload, restored.ciphertext());
        assertArrayEquals(signature, restored.firma());
    }

    @Test
    void readsKnownJsonContract() {
        EncryptedPackage result = service.deserialize(VALID.getBytes(StandardCharsets.UTF_8));
        assertEquals("Ana", result.remitente());
        assertEquals("archivo.mp4", result.nombreArchivo());
        assertEquals(BigInteger.valueOf(123), result.dhPublicK());
        assertArrayEquals(new byte[]{0, 1, -1}, result.ciphertext());
        assertArrayEquals(new byte[]{1}, result.firma());
        String json = new String(service.serialize(result), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"ciphertext\":\"AAH/\""));
        assertTrue(json.contains("\"dh_public_k\":\"123\""));
    }

    @Test
    void writesAndReadsJsonFiles(@TempDir Path directory) throws Exception {
        EncryptedPackage original = service.deserialize(VALID.getBytes(StandardCharsets.UTF_8));
        Path file = directory.resolve("package.json");
        try (var output = Files.newOutputStream(file)) {
            service.write(original, output);
        }
        try (var input = Files.newInputStream(file)) {
            EncryptedPackage restored = service.read(input, (int) Files.size(file));
            assertArrayEquals(original.ciphertext(), restored.ciphertext());
            assertArrayEquals(original.firma(), restored.firma());
        }
    }

    @Test
    void supportsIndependentSigningAndEncryption() {
        EncryptedPackage unsigned = new EncryptedPackage("Ana", "a.bin", BigInteger.TWO,
                BigInteger.TEN, new byte[]{1}, null);
        assertNull(service.deserialize(service.serialize(unsigned)).firma());
        EncryptedPackage signedOnly = new EncryptedPackage("Ana", "a.bin", null, null,
                new byte[]{1}, new byte[]{2});
        EncryptedPackage restored = service.deserialize(service.serialize(signedOnly));
        assertNull(restored.dhPublicK());
        assertNull(restored.dhPublicIv());
        assertArrayEquals(new byte[]{2}, restored.firma());
    }

    @Test
    void protectsBinaryArraysFromMutation() {
        byte[] payload = {1};
        byte[] signature = {2};
        EncryptedPackage value = new EncryptedPackage("Ana", "a.bin", null, null, payload, signature);
        payload[0] = 3;
        signature[0] = 4;
        value.ciphertext()[0] = 5;
        value.firma()[0] = 6;
        assertArrayEquals(new byte[]{1}, value.ciphertext());
        assertArrayEquals(new byte[]{2}, value.firma());
    }

    @Test
    void rejectsInvalidJsonSchemaAndEncoding() {
        for (String invalid : new String[]{"", "null", "[]", "{}", "{", VALID + "{}",
                VALID.replace("\"Ana\"", "42"), VALID.replace("\"Ana\"", "\" \""),
                VALID.replace("\"remitente\":\"Ana\",", ""),
                VALID.replace("\"remitente\":\"Ana\"", "\"remitente\":\"Ana\",\"remitente\":\"Bob\""),
                VALID.replace("\"remitente\":\"Ana\"", "\"remitente\":\"Ana\",\"extra\":1"),
                VALID.replace("\"123\"", "123"), VALID.replace("\"123\"", "\"-1\""),
                VALID.replace("\"123\"", "\"1\""), VALID.replace("\"123\"", "null"),
                VALID.replace("\"AAH/\"", "null"), VALID.replace("AAH/", "!!!!"),
                VALID.replace("AQ==", "AQ"), VALID.replace("AQ==", "AR=="),
                VALID.replace("AQ==", "A Q==")}) {
            assertThrows(RuntimeException.class,
                    () -> service.deserialize(invalid.getBytes(StandardCharsets.UTF_8)), invalid);
        }
    }

    @Test
    void enforcesUploadLimitAndKeepsCallerStreamsOpen() throws Exception {
        byte[] json = VALID.getBytes(StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class,
                () -> service.read(new ByteArrayInputStream(json), json.length - 1));
        assertThrows(IllegalArgumentException.class,
                () -> service.read(new ByteArrayInputStream(json), 0));
        var input = new ByteArrayInputStream(json) {
            @Override public void close() { fail("Caller owns input"); }
        };
        var output = new ByteArrayOutputStream() {
            @Override public void close() { fail("Caller owns output"); }
        };
        service.write(service.read(input, json.length), output);
        assertArrayEquals(new byte[]{0, 1, -1}, service.deserialize(output.toByteArray()).ciphertext());
    }
}
