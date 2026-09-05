package com.cripto.CriptoHibrida.crypto.model;

/** Select one or both operations of a single direction. */
public record ProcessOptions(boolean encrypt, boolean sign, boolean decrypt, boolean verify) {
    public ProcessOptions {
        if (!(encrypt || sign || decrypt || verify) || ((encrypt || sign) && (decrypt || verify))) {
            throw new IllegalArgumentException("Select emission or reception operations, not both directions");
        }
    }

    public void requireEmission() {
        if (!(encrypt || sign)) {
            throw new IllegalArgumentException("Emission requires encrypt or sign");
        }
    }

    public void requireReception() {
        if (!(decrypt || verify)) {
            throw new IllegalArgumentException("Reception requires decrypt or verify");
        }
    }
}
