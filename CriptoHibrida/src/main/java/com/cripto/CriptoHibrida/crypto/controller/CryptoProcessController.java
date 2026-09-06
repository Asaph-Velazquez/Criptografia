package com.cripto.CriptoHibrida.crypto.controller;

import com.cripto.CriptoHibrida.crypto.dto.ApiError;
import com.cripto.CriptoHibrida.crypto.dto.CryptoRequestOptions;
import com.cripto.CriptoHibrida.crypto.model.ProcessOptions;
import com.cripto.CriptoHibrida.crypto.model.VerificationResult;
import com.cripto.CriptoHibrida.crypto.model.VerificationResult.IntegrityStatus;
import com.cripto.CriptoHibrida.crypto.service.HybridCryptoOrchestrator;
import jakarta.servlet.MultipartConfigElement;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import tools.jackson.core.JacksonException;

/**
 * POST /api/crypto/process: file + options(application/json) + optional privateKey(PEM).
 * POST /api/crypto/verify-decrypt: package(JSON file) + options + optional publicKey(PEM).
 * Success downloads JSON or binary bytes, respectively. Reception exposes
 * X-Integrity-Status: VERIFIED or NOT_REQUESTED. FAILED produces HTTP 422 without file bytes.
 * Files are limited to 10 MiB, JSON packages to 16 MiB, and PEM keys to 64 KiB.
 */
@RestController
@RequestMapping("/api/crypto")
public class CryptoProcessController {
    private static final int FILE_LIMIT = 10 * 1024 * 1024;
    private static final int PACKAGE_LIMIT = 16 * 1024 * 1024;
    private final HybridCryptoOrchestrator orchestrator;

    public CryptoProcessController(HybridCryptoOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping(value = "/process", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> process(@RequestPart("file") MultipartFile file,
            @RequestPart("options") CryptoRequestOptions options,
            @RequestPart(value = "privateKey", required = false) MultipartFile privateKey)
            throws GeneralSecurityException, IOException {
        ProcessOptions selected = options.processOptions();
        selected.requireEmission();
        byte[] privatePem = selected.sign() ? upload(privateKey, 65536) : null;
        try {
            byte[] json = orchestrator.emit(upload(file, FILE_LIMIT), options.remitente(),
                    filename(file.getOriginalFilename()), selected,
                    selected.encrypt() ? options.parameters() : null,
                    selected.encrypt() ? CryptoRequestOptions.integer(options.publicK(), "publicK") : null,
                    selected.encrypt() ? CryptoRequestOptions.integer(options.publicIv(), "publicIv") : null,
                    privatePem);
            return download(json, "package.json", MediaType.APPLICATION_JSON);
        } finally {
            if (privatePem != null) {
                Arrays.fill(privatePem, (byte) 0);
            }
        }
    }

    @PostMapping(value = "/verify-decrypt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> receive(@RequestPart("package") MultipartFile json,
            @RequestPart("options") CryptoRequestOptions options,
            @RequestPart(value = "publicKey", required = false) MultipartFile publicKey)
            throws GeneralSecurityException, IOException {
        ProcessOptions selected = options.processOptions();
        selected.requireReception();
        VerificationResult result = orchestrator.receive(upload(json, PACKAGE_LIMIT), selected,
                selected.decrypt() ? options.parameters() : null,
                selected.decrypt() ? CryptoRequestOptions.integer(options.privateK(), "privateK") : null,
                selected.decrypt() ? CryptoRequestOptions.integer(options.privateIv(), "privateIv") : null,
                selected.verify() ? upload(publicKey, 65536) : null);
        if (result.integrity() == IntegrityStatus.FAILED) {
            return ResponseEntity.status(422).header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .header("X-Integrity-Status", "FAILED")
                    .body(new ApiError("INTEGRITY_FAILED", "No se pudo recuperar o verificar el archivo."));
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition(result.nombreArchivo()))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Integrity-Status", result.integrity().name()).body(result.file());
    }

    private byte[] upload(MultipartFile file, int limit) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("A required file part is missing");
        }
        if (file.getSize() > limit) {
            throw new MaxUploadSizeExceededException(limit);
        }
        try (var input = file.getInputStream()) {
            byte[] bytes = input.readNBytes(limit + 1);
            if (bytes.length > limit) {
                throw new MaxUploadSizeExceededException(limit);
            }
            return bytes;
        }
    }

    private ResponseEntity<byte[]> download(byte[] bytes, String name, MediaType type) {
        return ResponseEntity.ok().contentType(type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition(name))
                .header(HttpHeaders.CACHE_CONTROL, "no-store").body(bytes);
    }

    private static String filename(String supplied) {
        if (supplied == null || supplied.isBlank()) {
            return "archivo.bin";
        }
        String normalized = supplied.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "");
        return normalized.isBlank() || normalized.equals(".") || normalized.equals("..")
                ? "archivo.bin" : normalized;
    }

    private String disposition(String name) {
        return ContentDisposition.attachment().filename(filename(name), StandardCharsets.UTF_8).build().toString();
    }

    @ExceptionHandler({IllegalArgumentException.class, InvalidKeyException.class, InvalidKeySpecException.class,
            JacksonException.class, HttpMessageNotReadableException.class, MissingServletRequestPartException.class})
    public ResponseEntity<ApiError> badRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Revisa opciones, parámetros DH, archivos y llaves PEM.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(MaxUploadSizeExceededException exception) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "UPLOAD_TOO_LARGE", "El archivo excede el límite permitido.");
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiError> unreadable(IOException exception) {
        return error(HttpStatus.BAD_REQUEST, "UNREADABLE_UPLOAD", "No se pudo leer el archivo cargado.");
    }

    @ExceptionHandler(GeneralSecurityException.class)
    public ResponseEntity<ApiError> cryptoError(GeneralSecurityException exception) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "CRYPTO_ERROR", "No se pudo completar la operación criptográfica.");
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(new ApiError(code, message));
    }

    /** Public stateless file-processing API, with CORS origins configurable by deployment. */
    @Configuration(proxyBeanMethods = false)
    public static class ApiConfiguration {
        @Bean
        @Order(1)
        SecurityFilterChain cryptoApiSecurity(HttpSecurity http,
                @Value("${crypto.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000}") String origins)
                throws Exception {
            CorsConfiguration cors = new CorsConfiguration();
            cors.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim)
                    .filter(value -> !value.isEmpty()).toList());
            cors.setAllowedMethods(List.of("POST", "OPTIONS"));
            cors.setAllowedHeaders(List.of("Content-Type", "Accept"));
            cors.setExposedHeaders(List.of("Content-Disposition", "X-Integrity-Status"));
            cors.setAllowCredentials(false);
            UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
            source.registerCorsConfiguration("/api/crypto/**", cors);
            return http.securityMatcher("/api/crypto/**")
                    .cors(config -> config.configurationSource(source))
                    .csrf(config -> config.ignoringRequestMatchers("/api/crypto/**"))
                    .sessionManagement(config -> config.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .requestCache(config -> config.disable())
                    .authorizeHttpRequests(config -> config
                            .requestMatchers(org.springframework.http.HttpMethod.POST,
                                    "/api/crypto/process", "/api/crypto/verify-decrypt").permitAll()
                            .anyRequest().denyAll())
                    .build();
        }

        @Bean
        @Order(2)
        SecurityFilterChain otherRoutes(HttpSecurity http) throws Exception {
            return http.authorizeHttpRequests(config -> config
                    .dispatcherTypeMatchers(jakarta.servlet.DispatcherType.ERROR).permitAll()
                    .anyRequest().denyAll()).build();
        }

        @Bean
        MultipartConfigElement cryptoMultipartConfig() {
            return new MultipartConfigElement("", PACKAGE_LIMIT, 20L * 1024 * 1024, 0);
        }
    }
}
