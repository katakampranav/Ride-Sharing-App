package com.officemate.shared.util;

import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class for RSA key pair generation and management.
 * Provides methods to generate, encode, and decode RSA keys for JWT signing.
 */
@Slf4j
public class RsaKeyUtil {
    
    private static final int KEY_SIZE = 2048;
    private static final String ALGORITHM = "RSA";
    
    /**
     * Generate a new RSA key pair.
     *
     * @return KeyPair containing public and private keys
     * @throws NoSuchAlgorithmException if RSA algorithm is not available
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(KEY_SIZE);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        log.info("Generated new RSA key pair with size: {}", KEY_SIZE);
        return keyPair;
    }
    
    /**
     * Encode private key to Base64 string.
     *
     * @param privateKey the private key
     * @return Base64 encoded private key
     */
    public static String encodePrivateKey(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }
    
    /**
     * Encode public key to Base64 string.
     *
     * @param publicKey the public key
     * @return Base64 encoded public key
     */
    public static String encodePublicKey(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }
    
    /**
     * Decode private key from Base64 string.
     *
     * @param encodedKey Base64 encoded private key
     * @return PrivateKey object
     * @throws NoSuchAlgorithmException if RSA algorithm is not available
     * @throws InvalidKeySpecException if key specification is invalid
     */
    public static PrivateKey decodePrivateKey(String encodedKey) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePrivate(keySpec);
    }
    
    /**
     * Decode public key from Base64 string.
     *
     * @param encodedKey Base64 encoded public key
     * @return PublicKey object
     * @throws NoSuchAlgorithmException if RSA algorithm is not available
     * @throws InvalidKeySpecException if key specification is invalid
     */
    public static PublicKey decodePublicKey(String encodedKey) 
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] keyBytes = Base64.getDecoder().decode(encodedKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(ALGORITHM);
        return keyFactory.generatePublic(keySpec);
    }
}
