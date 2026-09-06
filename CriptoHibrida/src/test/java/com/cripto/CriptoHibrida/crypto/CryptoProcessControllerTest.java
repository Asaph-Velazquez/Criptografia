package com.cripto.CriptoHibrida.crypto;

import com.cripto.CriptoHibrida.crypto.model.DhParameters;
import com.cripto.CriptoHibrida.crypto.model.EncryptedPackage;
import com.cripto.CriptoHibrida.crypto.service.DiffieHellmanService;
import com.cripto.CriptoHibrida.crypto.service.PackageManagerService;
import java.math.BigInteger;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Full application context and real security filters, using real cryptographic services. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CryptoProcessControllerTest {
    @Autowired private MockMvc mvc;
    @Autowired private PackageManagerService packages;
    @LocalServerPort private int port;
    private static DhParameters parameters;
    private static byte[] privatePem;
    private static byte[] publicPem;
    private static final BigInteger B = BigInteger.valueOf(1234567);
    private static final BigInteger D = BigInteger.valueOf(7654321);
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @BeforeAll
    static void setup() throws Exception {
        parameters = new DhParameters(BigInteger.TWO, BigInteger.probablePrime(2048, new SecureRandom()));
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keys = generator.generateKeyPair();
        privatePem = pem("PRIVATE KEY", keys.getPrivate().getEncoded());
        publicPem = pem("PUBLIC KEY", keys.getPublic().getEncoded());
    }

    private static byte[] pem(String label, byte[] bytes) {
        return ("-----BEGIN " + label + "-----\n" + Base64.getEncoder().encodeToString(bytes)
                + "\n-----END " + label + "-----").getBytes(StandardCharsets.US_ASCII);
    }

    private Map<String, Object> emission(boolean encrypt, boolean sign) {
        Map<String, Object> options = new HashMap<>();
        options.put("encrypt", encrypt);
        options.put("sign", sign);
        options.put("remitente", "Ana");
        if (encrypt) {
            var dh = new DiffieHellmanService();
            options.put("g", parameters.g().toString());
            options.put("n", parameters.n().toString());
            options.put("publicK", dh.computePublicKey(parameters, B).toString());
            options.put("publicIv", dh.computePublicKey(parameters, D).toString());
        }
        return options;
    }

    private Map<String, Object> reception(boolean decrypt, boolean verify) {
        Map<String, Object> options = new HashMap<>();
        options.put("decrypt", decrypt);
        options.put("verify", verify);
        if (decrypt) {
            options.put("g", parameters.g().toString());
            options.put("n", parameters.n().toString());
            options.put("privateK", B.toString());
            options.put("privateIv", D.toString());
        }
        return options;
    }

    private MockMultipartFile optionsPart(Map<String, Object> values) {
        return new MockMultipartFile("options", "", "application/json", MAPPER.writeValueAsBytes(values));
    }

    private byte[] emit(byte[] data, boolean encrypt, boolean sign) throws Exception {
        var request = multipart("/api/crypto/process")
                .file(new MockMultipartFile("file", "multimedia.mp4", "application/octet-stream", data))
                .file(optionsPart(emission(encrypt, sign)));
        if (sign) {
            request.file(new MockMultipartFile("privateKey", "private.pem", "application/x-pem-file", privatePem));
        }
        return mvc.perform(request).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.nombre_archivo").value("multimedia.mp4"))
                .andReturn().getResponse().getContentAsByteArray();
    }

    private ResultActions receive(byte[] json, boolean decrypt, boolean verify) throws Exception {
        var request = multipart("/api/crypto/verify-decrypt")
                .file(new MockMultipartFile("package", "package.json", "application/json", json))
                .file(optionsPart(reception(decrypt, verify)));
        if (verify) {
            request.file(new MockMultipartFile("publicKey", "public.pem", "application/x-pem-file", publicPem));
        }
        return mvc.perform(request);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3})
    void downloadsExactFileThroughAllModes(int mode) throws Exception {
        byte[] data = new byte[4097];
        new Random(42).nextBytes(data);
        boolean encrypt = (mode & 1) != 0;
        boolean sign = (mode & 2) != 0;
        receive(emit(data, encrypt, sign), encrypt, sign)
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(content().bytes(data))
                .andExpect(header().string("X-Integrity-Status", sign ? "VERIFIED" : "NOT_REQUESTED"))
                .andExpect(header().exists(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void supportsEmptyFiles() throws Exception {
        receive(emit(new byte[0], true, true), true, true)
                .andExpect(status().isOk()).andExpect(content().bytes(new byte[0]));
    }

    @Test
    void tamperingReturns422WithoutRecoveredFile() throws Exception {
        EncryptedPackage original = packages.deserialize(emit(new byte[64], true, true));
        byte[] corrupted = original.ciphertext();
        corrupted[0] ^= 1;
        byte[] changed = packages.serialize(new EncryptedPackage(original.remitente(), original.nombreArchivo(),
                original.dhPublicK(), original.dhPublicIv(), corrupted, original.firma()));
        receive(changed, true, true).andExpect(status().is(422))
                .andExpect(jsonPath("$.code").value("INTEGRITY_FAILED"))
                .andExpect(jsonPath("$.file").doesNotExist())
                .andExpect(header().string("X-Integrity-Status", "FAILED"))
                .andExpect(header().doesNotExist(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void invalidSignatureAndWrongPublicKeyReturn422() throws Exception {
        EncryptedPackage original = packages.deserialize(emit(new byte[]{1}, false, true));
        byte[] changed = packages.serialize(new EncryptedPackage(original.remitente(), original.nombreArchivo(),
                null, null, original.ciphertext(), new byte[256]));
        receive(changed, false, true).andExpect(status().is(422));
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        mvc.perform(multipart("/api/crypto/verify-decrypt")
                .file(new MockMultipartFile("package", packages.serialize(original)))
                .file(optionsPart(reception(false, true)))
                .file(new MockMultipartFile("publicKey", pem("PUBLIC KEY", generator.generateKeyPair()
                        .getPublic().getEncoded()))))
                .andExpect(status().is(422)).andExpect(jsonPath("$.code").value("INTEGRITY_FAILED"));
    }

    @Test
    void missingPartsInvalidModesAndPemReturn400() throws Exception {
        mvc.perform(multipart("/api/crypto/process").file(optionsPart(emission(false, true))))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/crypto/process").file(new MockMultipartFile("file", new byte[]{1}))
                .file(optionsPart(emission(false, false))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(multipart("/api/crypto/process").file(new MockMultipartFile("file", new byte[]{1}))
                .file(optionsPart(emission(false, true))))
                .andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/crypto/process").file(new MockMultipartFile("file", new byte[]{1}))
                .file(optionsPart(emission(false, true))).file(new MockMultipartFile("privateKey", new byte[]{1})))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        var mixed = emission(false, true);
        mixed.put("verify", true);
        mvc.perform(multipart("/api/crypto/process").file(new MockMultipartFile("file", new byte[]{1}))
                .file(optionsPart(mixed))).andExpect(status().isBadRequest());
    }

    @Test
    void malformedJsonAndIncompatibleReceiveOptionsReturn400() throws Exception {
        receive(new byte[]{123}, false, true).andExpect(status().isBadRequest());
        receive(emit(new byte[]{1}, true, false), false, true).andExpect(status().isBadRequest());
        receive(emit(new byte[]{1}, true, false), true, true).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/crypto/process").file(new MockMultipartFile("file", new byte[]{1}))
                .file(new MockMultipartFile("options", "", "application/json", new byte[]{123})))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsInvalidAndOversizedDhParameters() throws Exception {
        for (String n : new String[]{"23", "-1", "9".repeat(2501)}) {
            var values = emission(true, false);
            values.put("n", n);
            mvc.perform(multipart("/api/crypto/process").file(new MockMultipartFile("file", new byte[]{1}))
                    .file(optionsPart(values))).andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsOversizedFilesAndPem() throws Exception {
        mvc.perform(multipart("/api/crypto/process")
                .file(new MockMultipartFile("file", new byte[10 * 1024 * 1024 + 1]))
                .file(optionsPart(emission(true, false))))
                .andExpect(status().is(413)).andExpect(jsonPath("$.code").value("UPLOAD_TOO_LARGE"));
        mvc.perform(multipart("/api/crypto/process").file(new MockMultipartFile("file", new byte[1]))
                .file(optionsPart(emission(false, true))).file(new MockMultipartFile("privateKey", new byte[65537])))
                .andExpect(status().is(413));
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://127.0.0.1:5173",
            "http://localhost:3000", "http://127.0.0.1:3000"})
    void corsAllowsConfiguredFrontendAndExposesDownloadHeaders(String origin) throws Exception {
        mvc.perform(options("/api/crypto/process").header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin));
        mvc.perform(multipart("/api/crypto/process").file(new MockMultipartFile("file", new byte[0]))
                .file(optionsPart(emission(false, true))).file(new MockMultipartFile("privateKey", privatePem))
                .header(HttpHeaders.ORIGIN, origin))
                .andExpect(status().isOk()).andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        "Content-Disposition, X-Integrity-Status"));
        mvc.perform(options("/api/crypto/process").header(HttpHeaders.ORIGIN, "https://unconfigured.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")).andExpect(status().isForbidden());
    }

    @Test
    void downloadFilenameCannotInjectHeadersOrPaths() throws Exception {
        EncryptedPackage original = packages.deserialize(emit(new byte[]{1, 2}, false, true));
        byte[] json = packages.serialize(new EncryptedPackage(original.remitente(), "../../mal\r\nicioso.mp4",
                null, null, original.ciphertext(), original.firma()));
        var response = receive(json, false, true).andExpect(status().isOk()).andReturn().getResponse();
        assertEquals("malicioso.mp4", ContentDisposition.parse(response.getHeader(HttpHeaders.CONTENT_DISPOSITION))
                .getFilename());
        assertFalse(response.getHeader(HttpHeaders.CONTENT_DISPOSITION).contains("\r\n"));
    }

    @Test
    void realHttpServerParsesMultipartAndEnforcesServletSizeLimit() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            byte[] original = new byte[]{0, -1, 2, 3};
            HttpResponse<byte[]> emitted = postMultipart(client, "/process", "file", original,
                    emission(false, true), "privateKey", privatePem);
            assertEquals(200, emitted.statusCode());
            HttpResponse<byte[]> received = postMultipart(client, "/verify-decrypt", "package", emitted.body(),
                    reception(false, true), "publicKey", publicPem);
            assertEquals(200, received.statusCode());
            assertArrayEquals(original, received.body());
            assertEquals("VERIFIED", received.headers().firstValue("X-Integrity-Status").orElseThrow());
            HttpResponse<byte[]> oversized = postMultipart(client, "/process", "file",
                    new byte[16 * 1024 * 1024 + 1], emission(false, true), "privateKey", privatePem);
            assertEquals(413, oversized.statusCode());
        }
    }

    private HttpResponse<byte[]> postMultipart(HttpClient client, String path, String filePart, byte[] file,
            Map<String, Object> values, String keyPart, byte[] key) throws Exception {
        String boundary = "crypto-integration-test-boundary";
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        String[] names = {filePart, "options", keyPart};
        byte[][] contents = {file, MAPPER.writeValueAsBytes(values), key};
        for (int i = 0; i < names.length; i++) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + names[i]
                    + "\"; filename=\"upload.bin\"\r\nContent-Type: "
                    + (i == 1 ? "application/json" : "application/octet-stream") + "\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            body.write(contents[i]);
            body.write(new byte[]{13, 10});
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/crypto" + path))
                .timeout(java.time.Duration.ofSeconds(20))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
}
