package com.cripto.CriptoHibrida.crypto;

import com.cripto.CriptoHibrida.crypto.service.AesCipherService;
import com.cripto.CriptoHibrida.crypto.service.DiffieHellmanService;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class AesCipherServiceTest {
    private final AesCipherService sender = new AesCipherService();
    private final AesCipherService receiver = new AesCipherService();
    private final DiffieHellmanService dh = new DiffieHellmanService();
    private final byte[] key = dh.deriveAesKey(BigInteger.valueOf(123456));
    private final byte[] iv = dh.deriveIv(BigInteger.valueOf(987654));

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 15, 16, 17, 31, 32, 255, 256, 4097, 1048576})
    void roundTripsBinaryFilesInBothDirections(int size) throws Exception {
        byte[] original = new byte[size];
        new Random(42L + size).nextBytes(original);
        byte[] snapshot = original.clone();
        byte[] keySnapshot = key.clone();
        byte[] ivSnapshot = iv.clone();
        byte[] encrypted = sender.encrypt(original, key, iv);
        assertEquals((size / 16 + 1) * 16, encrypted.length);
        assertArrayEquals(original, receiver.decrypt(encrypted, key, iv));
        assertArrayEquals(original, sender.decrypt(receiver.encrypt(original, key, iv), key, iv));
        assertArrayEquals(snapshot, original);
        assertArrayEquals(keySnapshot, key);
        assertArrayEquals(ivSnapshot, iv);
    }

    @Test
    void preservesEveryPossibleByteValue() throws Exception {
        byte[] original = new byte[256];
        for (int i = 0; i < original.length; i++) {
            original[i] = (byte) i;
        }
        assertArrayEquals(original, receiver.decrypt(sender.encrypt(original, key, iv), key, iv));
    }

    @Test
    void matchesAes256CbcKnownFirstBlock() throws Exception {
        HexFormat hex = HexFormat.of();
        byte[] testKey = hex.parseHex("603deb1015ca71be2b73aef0857d77811f352c073b6108d72d9810a30914dff4");
        byte[] testIv = hex.parseHex("000102030405060708090a0b0c0d0e0f");
        byte[] plaintext = hex.parseHex("6bc1bee22e409f96e93d7e117393172a");
        byte[] ciphertext = sender.encrypt(plaintext, testKey, testIv);
        assertArrayEquals(hex.parseHex("f58c4c04d6e5f1ba779eabfb5f7bfbd6"), Arrays.copyOf(ciphertext, 16));
        assertEquals(32, ciphertext.length);
    }

    @Test
    void rejectsIncorrectKeyAndIvLengths() throws Exception {
        byte[] encrypted = sender.encrypt(new byte[5], key, iv);
        for (int size : new int[]{0, 16, 24, 31, 33}) {
            assertThrows(InvalidKeyException.class, () -> sender.encrypt(new byte[1], new byte[size], iv));
            assertThrows(InvalidKeyException.class, () -> receiver.decrypt(encrypted, new byte[size], iv));
        }
        for (int size : new int[]{0, 15, 17, 32}) {
            assertThrows(InvalidAlgorithmParameterException.class,
                    () -> sender.encrypt(new byte[1], key, new byte[size]));
            assertThrows(InvalidAlgorithmParameterException.class,
                    () -> receiver.decrypt(encrypted, key, new byte[size]));
        }
    }

    @Test
    void wrongIvThatChangesPaddingFailsDeterministically() throws Exception {
        byte[] encrypted = sender.encrypt(new byte[0], key, iv);
        byte[] wrongIv = iv.clone();
        wrongIv[15] ^= 16; // Changes the final padding byte from 16 to 0.
        assertThrows(BadPaddingException.class, () -> receiver.decrypt(encrypted, key, wrongIv));
    }

    @Test
    void wrongKeyFailsForFixedFixture() throws Exception {
        byte[] encrypted = sender.encrypt(new byte[0], key, iv);
        byte[] wrongKey = key.clone();
        wrongKey[0] ^= 1;
        assertThrows(BadPaddingException.class, () -> receiver.decrypt(encrypted, wrongKey, iv));
    }

    @Test
    void wrongIvCanCorruptFirstBlockWithoutPaddingFailure() throws Exception {
        byte[] original = new byte[32];
        byte[] encrypted = sender.encrypt(original, key, iv);
        byte[] wrongIv = iv.clone();
        wrongIv[0] ^= 1;
        byte[] corrupted = receiver.decrypt(encrypted, key, wrongIv);
        assertFalse(Arrays.equals(original, corrupted));
        assertEquals(1, corrupted[0]);
        assertArrayEquals(Arrays.copyOfRange(original, 16, 32), Arrays.copyOfRange(corrupted, 16, 32));
    }

    @Test
    void rejectsTruncatedAndEmptyCiphertext() {
        for (int size : new int[]{0, 1, 15, 17, 31}) {
            assertThrows(IllegalBlockSizeException.class,
                    () -> receiver.decrypt(new byte[size], key, iv));
        }
    }
}
