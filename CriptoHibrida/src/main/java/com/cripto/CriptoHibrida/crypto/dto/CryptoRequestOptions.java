package com.cripto.CriptoHibrida.crypto.dto;

import com.cripto.CriptoHibrida.crypto.model.DhParameters;
import com.cripto.CriptoHibrida.crypto.model.ProcessOptions;
import java.math.BigInteger;

/**
 * Multipart JSON options. All DH integers are decimal strings to preserve precision.
 * Emission: encrypt/sign, remitente, g/n/publicK/publicIv when encrypting.
 * Reception: decrypt/verify, g/n/privateK/privateIv when decrypting.
 */
public record CryptoRequestOptions(Boolean encrypt, Boolean sign, Boolean decrypt, Boolean verify,
                                   String remitente, String g, String n, String publicK,
                                   String publicIv, String privateK, String privateIv) {
    public ProcessOptions processOptions() {
        return new ProcessOptions(Boolean.TRUE.equals(encrypt), Boolean.TRUE.equals(sign),
                Boolean.TRUE.equals(decrypt), Boolean.TRUE.equals(verify));
    }

    public DhParameters parameters() {
        BigInteger modulus = integer(n, "n");
        if (modulus.bitLength() > 8192) {
            throw new IllegalArgumentException("DH modulus must not exceed 8192 bits");
        }
        return new DhParameters(integer(g, "g"), modulus);
    }

    public static BigInteger integer(String value, String field) {
        if (value == null || value.length() > 2500 || !value.matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException(field + " must be a positive decimal string");
        }
        return new BigInteger(value);
    }

    @Override
    public String toString() {
        return "CryptoRequestOptions[encrypt=" + encrypt + ", sign=" + sign
                + ", decrypt=" + decrypt + ", verify=" + verify + ", key material omitted]";
    }
}
