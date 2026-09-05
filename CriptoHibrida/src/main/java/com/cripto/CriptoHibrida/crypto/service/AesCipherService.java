package com.cripto.CriptoHibrida.crypto.service;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.util.Objects;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

/**
 * Binary AES-256-CBC. Use a fresh DH-derived IV for each encryption.
 * CBC does not authenticate data: a wrong key or IV may produce corrupted
 * plaintext without throwing. Integrity must be verified by the caller.
 */
@Service
public class AesCipherService {
    public byte[] encrypt(byte[] plaintext, byte[] key, byte[] iv) throws GeneralSecurityException {
        return transform(Cipher.ENCRYPT_MODE, plaintext, key, iv);
    }

    public byte[] decrypt(byte[] ciphertext, byte[] key, byte[] iv) throws GeneralSecurityException {
        Objects.requireNonNull(ciphertext, "ciphertext");
        if (ciphertext.length == 0 || ciphertext.length % 16 != 0) {
            throw new javax.crypto.IllegalBlockSizeException("Ciphertext must contain complete AES blocks");
        }
        return transform(Cipher.DECRYPT_MODE, ciphertext, key, iv);
    }

    private byte[] transform(int mode, byte[] input, byte[] key, byte[] iv)
            throws GeneralSecurityException {
        Objects.requireNonNull(input, "input");
        if (key == null || key.length != 32) {
            throw new InvalidKeyException("AES-256 requires exactly 32 key bytes");
        }
        if (iv == null || iv.length != 16) {
            throw new InvalidAlgorithmParameterException("AES-CBC requires exactly 16 IV bytes");
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(mode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        return cipher.doFinal(input);
    }
}
