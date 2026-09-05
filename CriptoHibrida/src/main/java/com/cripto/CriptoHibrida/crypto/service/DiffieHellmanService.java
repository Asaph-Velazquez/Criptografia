package com.cripto.CriptoHibrida.crypto.service;

import com.cripto.CriptoHibrida.crypto.model.DhParameters;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Stateless exchanges: keep each private exponent local to its participant. */
@Service
public class DiffieHellmanService {
    private final SecureRandom random = new SecureRandom();

    public BigInteger generatePrivateKey(DhParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        BigInteger secret;
        do {
            secret = new BigInteger(parameters.n().bitLength(), random);
        } while (secret.compareTo(BigInteger.TWO) < 0
                || secret.compareTo(parameters.n().subtract(BigInteger.TWO)) > 0);
        return secret;
    }

    public BigInteger computePublicKey(DhParameters parameters, BigInteger privateKey) {
        validateElement(parameters, privateKey, "privateKey");
        return parameters.g().modPow(privateKey, parameters.n());
    }

    public BigInteger computeSharedSecret(DhParameters parameters, BigInteger privateKey,
                                          BigInteger peerPublicKey) {
        validateElement(parameters, privateKey, "privateKey");
        validateElement(parameters, peerPublicKey, "peerPublicKey");
        BigInteger secret = peerPublicKey.modPow(privateKey, parameters.n());
        if (secret.compareTo(BigInteger.ONE) <= 0) {
            throw new IllegalArgumentException("Degenerate shared secret");
        }
        return secret;
    }

    public byte[] deriveAesKey(BigInteger sharedSecret) {
        return sha256(sharedSecret);
    }

    public byte[] deriveIv(BigInteger ivSecret) {
        return Arrays.copyOf(sha256(ivSecret), 16);
    }

    private byte[] sha256(BigInteger secret) {
        Objects.requireNonNull(secret, "secret");
        if (secret.compareTo(BigInteger.ONE) <= 0) {
            throw new IllegalArgumentException("Secret must be greater than one");
        }
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.toByteArray());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void validateElement(DhParameters parameters, BigInteger value, String name) {
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(value, name);
        if (value.compareTo(BigInteger.TWO) < 0
                || value.compareTo(parameters.n().subtract(BigInteger.TWO)) > 0) {
            throw new IllegalArgumentException(name + " must be in [2, n - 2]");
        }
    }
}
