package com.cripto.CriptoHibrida.crypto.service;

import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** P-256 with SHA-256; signatures use ASN.1 DER, not Web Crypto's P1363 encoding. */
@Service
public class EcdsaSignerService {
    public byte[] sign(byte[] data, PrivateKey key) throws GeneralSecurityException {
        validateKey(key);
        Signature signer = Signature.getInstance("SHA256withECDSA");
        signer.initSign(key);
        signer.update(Objects.requireNonNull(data, "data"));
        return signer.sign();
    }

    public boolean verify(byte[] data, byte[] signed, PublicKey key) throws GeneralSecurityException {
        validateKey(key);
        if (signed == null || signed.length == 0 || signed.length > 72) return false;
        Signature verifier = Signature.getInstance("SHA256withECDSA");
        verifier.initVerify(key);
        verifier.update(Objects.requireNonNull(data, "data"));
        try {
            return verifier.verify(signed);
        } catch (SignatureException exception) {
            return false;
        }
    }

    private void validateKey(Key key) throws GeneralSecurityException {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec expected = parameters.getParameterSpec(ECParameterSpec.class);
        if (!(key instanceof ECKey ec) || !"EC".equals(key.getAlgorithm()) || ec.getParams() == null
                || !expected.getCurve().equals(ec.getParams().getCurve())
                || !expected.getGenerator().equals(ec.getParams().getGenerator())
                || !expected.getOrder().equals(ec.getParams().getOrder())
                || expected.getCofactor() != ec.getParams().getCofactor()) {
            throw new InvalidKeyException("An ECDSA P-256 key is required");
        }
    }
}
