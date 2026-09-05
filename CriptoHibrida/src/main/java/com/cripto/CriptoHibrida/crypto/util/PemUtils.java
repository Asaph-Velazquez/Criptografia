package com.cripto.CriptoHibrida.crypto.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Reads one unencrypted PKCS#8 private key or X.509 SubjectPublicKeyInfo public key. */
public final class PemUtils {
    private PemUtils() { }

    public static PrivateKey parsePrivateKey(byte[] pem) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePrivate(
                new PKCS8EncodedKeySpec(decode(pem, "PRIVATE KEY")));
    }

    public static PublicKey parsePublicKey(byte[] pem) throws GeneralSecurityException {
        return KeyFactory.getInstance("RSA").generatePublic(
                new X509EncodedKeySpec(decode(pem, "PUBLIC KEY")));
    }

    private static byte[] decode(byte[] pem, String label) throws InvalidKeySpecException {
        if (pem == null || pem.length == 0 || pem.length > 65536) {
            throw new InvalidKeySpecException("PEM must contain between 1 and 65536 bytes");
        }
        for (byte value : pem) {
            if (value < 0) {
                throw new InvalidKeySpecException("PEM must be ASCII");
            }
        }
        String text = new String(pem, StandardCharsets.US_ASCII).strip();
        String begin = "-----BEGIN " + label + "-----";
        String end = "-----END " + label + "-----";
        if (!text.startsWith(begin) || !text.endsWith(end)) {
            throw new InvalidKeySpecException("Expected a single " + label + " PEM block");
        }
        String body = text.substring(begin.length(), text.length() - end.length())
                .replaceAll("[\\r\\n\\t ]", "");
        if (body.isEmpty()) {
            throw new InvalidKeySpecException("Empty PEM body");
        }
        try {
            return Base64.getDecoder().decode(body);
        } catch (IllegalArgumentException exception) {
            throw new InvalidKeySpecException("Invalid PEM Base64", exception);
        }
    }
}
