package com.officemate.config;

import com.officemate.shared.util.RsaKeyUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.security.*;
import java.security.spec.InvalidKeySpecException;

/**
 * Configuration properties for JWT token generation and validation.
 * Reads values from application.yml under app.security.jwt
 * Supports both HMAC (secret) and RSA (key pair) signing methods.
 */
@Configuration
@ConfigurationProperties(prefix = "app.security.jwt")
@Data
@Slf4j
public class JwtConfig {
    
    /**
     * Secret key for signing JWT tokens (HMAC-SHA256)
     * Used as fallback if RSA keys are not configured
     */
    private String secret;
    
    /**
     * Access token expiration time in milliseconds (default: 1 hour)
     */
    private Long expiration = 3600000L;
    
    /**
     * Refresh token expiration time in milliseconds (default: 24 hours)
     */
    private Long refreshExpiration = 86400000L;
    
    /**
     * Base64 encoded RSA private key for signing tokens
     */
    private String rsaPrivateKey;
    
    /**
     * Base64 encoded RSA public key for verifying tokens
     */
    private String rsaPublicKey;
    
    /**
     * Signing algorithm to use (RS256 for RSA, HS256 for HMAC)
     */
    private String algorithm = "RS256";
    
    /**
     * Issuer claim for JWT tokens
     */
    private String issuer = "officemate";
    
    // Decoded key objects
    private PrivateKey privateKey;
    private PublicKey publicKey;
    
    /**
     * Initialize RSA keys after properties are loaded.
     * If RSA keys are not configured, generates new ones.
     */
    @PostConstruct
    public void init() {
        try {
            if (rsaPrivateKey != null && rsaPublicKey != null) {
                // Decode existing RSA keys
                this.privateKey = RsaKeyUtil.decodePrivateKey(rsaPrivateKey);
                this.publicKey = RsaKeyUtil.decodePublicKey(rsaPublicKey);
                log.info("Loaded RSA keys from configuration");
            } else if ("RS256".equals(algorithm)) {
                // Generate new RSA key pair
                KeyPair keyPair = RsaKeyUtil.generateKeyPair();
                this.privateKey = keyPair.getPrivate();
                this.publicKey = keyPair.getPublic();
                this.rsaPrivateKey = RsaKeyUtil.encodePrivateKey(privateKey);
                this.rsaPublicKey = RsaKeyUtil.encodePublicKey(publicKey);
                log.warn("Generated new RSA key pair. Consider storing these keys in configuration:");
                log.warn("Private Key: {}", rsaPrivateKey);
                log.warn("Public Key: {}", rsaPublicKey);
            } else {
                log.info("Using HMAC-SHA256 with secret key for JWT signing");
            }
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            log.error("Failed to initialize RSA keys", e);
            throw new RuntimeException("Failed to initialize JWT configuration", e);
        }
    }
    
    /**
     * Check if RSA signing is enabled.
     *
     * @return true if using RSA algorithm
     */
    public boolean isRsaEnabled() {
        return "RS256".equals(algorithm) && privateKey != null && publicKey != null;
    }
}
