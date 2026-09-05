package com.cripto.CriptoHibrida.crypto.model;

import java.util.Objects;

/** FAILED never carries recovered bytes; NOT_REQUESTED is not a claim of integrity. */
public record VerificationResult(String remitente, String nombreArchivo,
                                 IntegrityStatus integrity, byte[] file) {
    public enum IntegrityStatus { VERIFIED, NOT_REQUESTED, FAILED }

    public VerificationResult {
        Objects.requireNonNull(integrity, "integrity");
        if (integrity == IntegrityStatus.FAILED) {
            file = null;
        } else {
            file = Objects.requireNonNull(file, "file").clone();
        }
    }

    @Override
    public byte[] file() {
        return file == null ? null : file.clone();
    }
}
