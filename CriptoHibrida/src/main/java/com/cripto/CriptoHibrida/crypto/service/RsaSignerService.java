package com.cripto.CriptoHibrida.crypto.service;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAKey;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class RsaSignerService {
    public byte[] sha256(byte[] data) throws GeneralSecurityException {
        return MessageDigest.getInstance("SHA-256").digest(Objects.requireNonNull(data, "data"));
    }

    /** SHA256withRSA hashes the original bytes internally; do not pass a precomputed hash. */
    public byte[] sign(byte[] data, PrivateKey privateKey) throws GeneralSecurityException {
        Objects.requireNonNull(data, "data");
        validateKey(privateKey);
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /** Malformed or non-matching signatures return false; invalid keys raise an exception. */
    public boolean verify(byte[] data, byte[] signed, PublicKey publicKey) throws GeneralSecurityException {
        Objects.requireNonNull(data, "data");
        validateKey(publicKey);
        if (signed == null || signed.length != (((RSAKey) publicKey).getModulus().bitLength() + 7) / 8) {
            return false;
        }
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data);
        try {
            return signature.verify(signed);
        } catch (SignatureException exception) {
            return false;
        }
    }

    private void validateKey(Key key) throws InvalidKeyException {
        if (!(key instanceof RSAKey rsa) || !"RSA".equals(key.getAlgorithm())
                || rsa.getModulus().bitLength() < 2048) {
            throw new InvalidKeyException("An RSA key of at least 2048 bits is required");
        }
    }
}
