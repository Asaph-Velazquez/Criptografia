package com.cripto.CriptoHibrida.crypto.model;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Binary package contents. DH values may both be null for signing-only operations;
 * firma may be null for encryption-only operations. The filename is metadata, not a path.
 */
public record EncryptedPackage(String remitente, String nombreArchivo,
                               BigInteger dhPublicK, BigInteger dhPublicIv,
                               byte[] ciphertext, byte[] firma) {
    public EncryptedPackage {
        if (remitente == null || remitente.isBlank() || nombreArchivo == null || nombreArchivo.isBlank()) {
            throw new IllegalArgumentException("Sender and original filename are required");
        }
        if ((dhPublicK == null) != (dhPublicIv == null)) {
            throw new IllegalArgumentException("Both DH public values must be provided together");
        }
        if (dhPublicK != null && (dhPublicK.compareTo(BigInteger.TWO) < 0
                || dhPublicIv.compareTo(BigInteger.TWO) < 0)) {
            throw new IllegalArgumentException("DH public values must be greater than one");
        }
        ciphertext = Objects.requireNonNull(ciphertext, "ciphertext").clone();
        firma = firma == null ? null : firma.clone();
    }

    @Override
    public byte[] ciphertext() {
        return ciphertext.clone();
    }

    @Override
    public byte[] firma() {
        return firma == null ? null : firma.clone();
    }
}
