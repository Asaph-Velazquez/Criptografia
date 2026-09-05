package com.cripto.CriptoHibrida.crypto.service;

import com.cripto.CriptoHibrida.crypto.model.EncryptedPackage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** JSON file bytes in/out; stream ownership remains with the caller. */
@Service
public class PackageManagerService {
    private static final Set<String> FIELDS = Set.of(
            "remitente", "nombre_archivo", "dh_public_k", "dh_public_iv", "ciphertext", "firma");
    private final JsonMapper mapper = JsonMapper.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    public byte[] serialize(EncryptedPackage value) {
        Objects.requireNonNull(value, "value");
        ObjectNode root = mapper.createObjectNode();
        root.put("remitente", value.remitente());
        root.put("nombre_archivo", value.nombreArchivo());
        // Decimal strings preserve arbitrary precision in JavaScript clients as well.
        root.put("dh_public_k", value.dhPublicK() == null ? null : value.dhPublicK().toString());
        root.put("dh_public_iv", value.dhPublicIv() == null ? null : value.dhPublicIv().toString());
        root.put("ciphertext", Base64.getEncoder().encodeToString(value.ciphertext()));
        byte[] signature = value.firma();
        root.put("firma", signature == null ? null : Base64.getEncoder().encodeToString(signature));
        return mapper.writeValueAsBytes(root);
    }

    /** Returns only schema-validated content; this does not verify cryptographic integrity. */
    public EncryptedPackage deserialize(byte[] json) {
        Objects.requireNonNull(json, "json");
        JsonNode root = mapper.readTree(json);
        if (root == null || !root.isObject() || !Set.copyOf(root.propertyNames()).equals(FIELDS)) {
            throw new IllegalArgumentException("Expected exactly the six encrypted-package fields");
        }
        return new EncryptedPackage(text(root, "remitente"), text(root, "nombre_archivo"),
                dhValue(root, "dh_public_k"), dhValue(root, "dh_public_iv"),
                decode(text(root, "ciphertext")),
                root.get("firma").isNull() ? null : decode(text(root, "firma")));
    }

    public void write(EncryptedPackage value, OutputStream output) throws IOException {
        Objects.requireNonNull(output, "output").write(serialize(value));
    }

    /** Caller supplies an upload limit in bytes; at most limit + 1 bytes are read. */
    public EncryptedPackage read(InputStream input, int maxJsonBytes) throws IOException {
        Objects.requireNonNull(input, "input");
        if (maxJsonBytes <= 0 || maxJsonBytes == Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Upload limit must be positive and below Integer.MAX_VALUE");
        }
        byte[] json = input.readNBytes(maxJsonBytes + 1);
        if (json.length > maxJsonBytes) {
            throw new IllegalArgumentException("JSON upload exceeds the configured limit");
        }
        return deserialize(json);
    }

    private String text(JsonNode root, String field) {
        JsonNode value = root.get(field);
        if (!value.isString()) {
            throw new IllegalArgumentException(field + " must be a string");
        }
        return value.asString();
    }

    private BigInteger dhValue(JsonNode root, String field) {
        if (root.get(field).isNull()) {
            return null;
        }
        String value = text(root, field);
        if (value.length() > 2500 || !value.matches("[1-9][0-9]*")) {
            throw new IllegalArgumentException(field + " must be a positive decimal string of at most 2500 digits");
        }
        return new BigInteger(value);
    }

    private byte[] decode(String value) {
        byte[] decoded = Base64.getDecoder().decode(value);
        if (!Base64.getEncoder().encodeToString(decoded).equals(value)) {
            throw new IllegalArgumentException("Expected canonical padded Base64");
        }
        return decoded;
    }
}
