package com.cripto.CriptoHibrida.crypto.model;

import java.math.BigInteger;
import java.util.Objects;

/** Immutable global parameters shared by both participants and both agreements. */
public record DhParameters(BigInteger g, BigInteger n) {
    public DhParameters {
        Objects.requireNonNull(g, "g");
        Objects.requireNonNull(n, "n");
        if (n.bitLength() < 2048 || !n.isProbablePrime(100)) {
            throw new IllegalArgumentException("n must be a prime of at least 2048 bits");
        }
        if (g.compareTo(BigInteger.TWO) < 0 || g.compareTo(n.subtract(BigInteger.TWO)) > 0) {
            throw new IllegalArgumentException("g must be in [2, n - 2]");
        }
    }
}
