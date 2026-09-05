package com.cripto.CriptoHibrida.crypto.service;

import com.cripto.CriptoHibrida.crypto.model.DhParameters;
import com.cripto.CriptoHibrida.crypto.model.EncryptedPackage;
import com.cripto.CriptoHibrida.crypto.model.ProcessOptions;
import com.cripto.CriptoHibrida.crypto.model.VerificationResult;
import com.cripto.CriptoHibrida.crypto.model.VerificationResult.IntegrityStatus;
import com.cripto.CriptoHibrida.crypto.util.PemUtils;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import org.springframework.stereotype.Service;

/** Per-call state only: no private keys or recovered files are retained by the service. */
@Service
public class HybridCryptoOrchestrator {
    private final DiffieHellmanService dh;
    private final AesCipherService aes;
    private final RsaSignerService rsa;
    private final PackageManagerService packages;

    public HybridCryptoOrchestrator(DiffieHellmanService dh, AesCipherService aes,
                                    RsaSignerService rsa, PackageManagerService packages) {
        this.dh = dh;
        this.aes = aes;
        this.rsa = rsa;
        this.packages = packages;
    }

    /** Recipient supplies Kb and Kd; fresh independent a and c are generated for each emission. */
    public byte[] emit(byte[] file, String sender, String filename, ProcessOptions options,
                       DhParameters parameters, BigInteger recipientPublicK, BigInteger recipientPublicIv,
                       byte[] privateKeyPem) throws GeneralSecurityException {
        require(options, "options");
        options.requireEmission();
        require(file, "file");
        // Validate metadata before performing cryptographic work.
        new EncryptedPackage(sender, filename, null, null, file, null);
        byte[] signature = null;
        if (options.sign()) {
            require(privateKeyPem, "privateKeyPem");
            signature = rsa.sign(file, PemUtils.parsePrivateKey(privateKeyPem));
        }
        BigInteger publicK = null;
        BigInteger publicIv = null;
        byte[] payload = file;
        if (options.encrypt()) {
            require(parameters, "parameters");
            require(recipientPublicK, "recipientPublicK");
            require(recipientPublicIv, "recipientPublicIv");
            BigInteger a = dh.generatePrivateKey(parameters);
            BigInteger c = dh.generatePrivateKey(parameters);
            publicK = dh.computePublicKey(parameters, a);
            publicIv = dh.computePublicKey(parameters, c);
            byte[] key = dh.deriveAesKey(dh.computeSharedSecret(parameters, a, recipientPublicK));
            byte[] iv = dh.deriveIv(dh.computeSharedSecret(parameters, c, recipientPublicIv));
            try {
                payload = aes.encrypt(file, key, iv);
            } finally {
                Arrays.fill(key, (byte) 0);
                Arrays.fill(iv, (byte) 0);
            }
        }
        return packages.serialize(new EncryptedPackage(sender, filename, publicK, publicIv, payload, signature));
    }

    /** Recipient supplies b and d locally; the uploaded JSON contains only sender public values. */
    public VerificationResult receive(byte[] json, ProcessOptions options, DhParameters parameters,
                                       BigInteger privateK, BigInteger privateIv, byte[] publicKeyPem)
            throws GeneralSecurityException {
        require(options, "options");
        options.requireReception();
        require(json, "json");
        EncryptedPackage value = packages.deserialize(json);
        boolean encrypted = value.dhPublicK() != null;
        if (options.decrypt() != encrypted) {
            throw new IllegalArgumentException(encrypted
                    ? "Select decrypt to recover the signed original bytes"
                    : "The package does not contain encrypted data");
        }
        if (options.verify()) {
            require(publicKeyPem, "publicKeyPem");
            if (value.firma() == null) {
                throw new IllegalArgumentException("The package has no signature to verify");
            }
        }
        var publicKey = options.verify() ? PemUtils.parsePublicKey(publicKeyPem) : null;
        byte[] recovered = value.ciphertext();
        if (options.decrypt()) {
            require(parameters, "parameters");
            require(privateK, "privateK");
            require(privateIv, "privateIv");
            byte[] key = dh.deriveAesKey(dh.computeSharedSecret(parameters, privateK, value.dhPublicK()));
            byte[] iv = dh.deriveIv(dh.computeSharedSecret(parameters, privateIv, value.dhPublicIv()));
            try {
                recovered = aes.decrypt(recovered, key, iv);
            } catch (BadPaddingException | IllegalBlockSizeException exception) {
                return result(value, IntegrityStatus.FAILED, null);
            } finally {
                Arrays.fill(key, (byte) 0);
                Arrays.fill(iv, (byte) 0);
            }
        }
        try {
            if (options.verify() && !rsa.verify(recovered, value.firma(), publicKey)) {
                return result(value, IntegrityStatus.FAILED, null);
            }
            return result(value, options.verify() ? IntegrityStatus.VERIFIED : IntegrityStatus.NOT_REQUESTED,
                    recovered);
        } finally {
            Arrays.fill(recovered, (byte) 0);
        }
    }

    private VerificationResult result(EncryptedPackage value, IntegrityStatus status, byte[] file) {
        return new VerificationResult(value.remitente(), value.nombreArchivo(), status, file);
    }

    private void require(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is required for the selected operation");
        }
    }
}
