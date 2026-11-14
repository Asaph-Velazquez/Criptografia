package com.cripto.CriptoHibrida.crypto.model;

/** Explicit allowlist; legacy packages and requests use RSA. */
public enum SignatureAlgorithm {
    RSA, ECDSA;

    public static SignatureAlgorithm parse(String value) {
        return value == null ? RSA : valueOf(value);
    }

    public String keyAlgorithm() {
        return this == ECDSA ? "EC" : "RSA";
    }
}
