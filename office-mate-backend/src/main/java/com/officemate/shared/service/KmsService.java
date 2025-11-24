package com.officemate.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for encryption and key management via AWS KMS.
 * Used for encrypting sensitive data and signing JWT tokens.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KmsService {

    private final KmsClient kmsClient;

    @Value("${aws.kms.key-id}")
    private String keyId;

    @Value("${aws.kms.key-alias}")
    private String keyAlias;

    /**
     * Encrypt data using KMS
     *
     * @param plaintext The data to encrypt
     * @return Base64-encoded encrypted data
     */
    public String encrypt(String plaintext) {
        try {
            SdkBytes plaintextBytes = SdkBytes.fromByteArray(plaintext.getBytes(StandardCharsets.UTF_8));

            EncryptRequest request = EncryptRequest.builder()
                    .keyId(getKeyIdentifier())
                    .plaintext(plaintextBytes)
                    .build();

            EncryptResponse response = kmsClient.encrypt(request);
            
            byte[] encryptedData = response.ciphertextBlob().asByteArray();
            String encoded = Base64.getEncoder().encodeToString(encryptedData);
            
            log.debug("Data encrypted successfully using KMS key");
            
            return encoded;
            
        } catch (KmsException e) {
            log.error("Failed to encrypt data with KMS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to encrypt data: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Decrypt data using KMS
     *
     * @param ciphertext Base64-encoded encrypted data
     * @return Decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(ciphertext);
            SdkBytes ciphertextBytes = SdkBytes.fromByteArray(encryptedData);

            DecryptRequest request = DecryptRequest.builder()
                    .ciphertextBlob(ciphertextBytes)
                    .keyId(getKeyIdentifier())
                    .build();

            DecryptResponse response = kmsClient.decrypt(request);
            
            byte[] decryptedData = response.plaintext().asByteArray();
            String plaintext = new String(decryptedData, StandardCharsets.UTF_8);
            
            log.debug("Data decrypted successfully using KMS key");
            
            return plaintext;
            
        } catch (KmsException e) {
            log.error("Failed to decrypt data with KMS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to decrypt data: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Generate data key for envelope encryption
     *
     * @return Data key response containing plaintext and encrypted key
     */
    public GenerateDataKeyResponse generateDataKey() {
        try {
            GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
                    .keyId(getKeyIdentifier())
                    .keySpec(DataKeySpec.AES_256)
                    .build();

            GenerateDataKeyResponse response = kmsClient.generateDataKey(request);
            
            log.debug("Data key generated successfully");
            
            return response;
            
        } catch (KmsException e) {
            log.error("Failed to generate data key with KMS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate data key: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Sign data using KMS (for JWT token signing)
     *
     * @param message The message to sign
     * @return Base64-encoded signature
     */
    public String sign(String message) {
        try {
            SdkBytes messageBytes = SdkBytes.fromByteArray(message.getBytes(StandardCharsets.UTF_8));

            SignRequest request = SignRequest.builder()
                    .keyId(getKeyIdentifier())
                    .message(messageBytes)
                    .signingAlgorithm(SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256)
                    .build();

            SignResponse response = kmsClient.sign(request);
            
            byte[] signature = response.signature().asByteArray();
            String encoded = Base64.getEncoder().encodeToString(signature);
            
            log.debug("Message signed successfully using KMS key");
            
            return encoded;
            
        } catch (KmsException e) {
            log.error("Failed to sign message with KMS: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sign message: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Verify signature using KMS
     *
     * @param message   The original message
     * @param signature Base64-encoded signature
     * @return true if signature is valid
     */
    public boolean verify(String message, String signature) {
        try {
            SdkBytes messageBytes = SdkBytes.fromByteArray(message.getBytes(StandardCharsets.UTF_8));
            byte[] signatureData = Base64.getDecoder().decode(signature);
            SdkBytes signatureBytes = SdkBytes.fromByteArray(signatureData);

            VerifyRequest request = VerifyRequest.builder()
                    .keyId(getKeyIdentifier())
                    .message(messageBytes)
                    .signature(signatureBytes)
                    .signingAlgorithm(SigningAlgorithmSpec.RSASSA_PKCS1_V1_5_SHA_256)
                    .build();

            VerifyResponse response = kmsClient.verify(request);
            
            boolean isValid = response.signatureValid();
            log.debug("Signature verification result: {}", isValid);
            
            return isValid;
            
        } catch (KmsException e) {
            log.error("Failed to verify signature with KMS: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get the key identifier (use key ID if available, otherwise use alias)
     */
    private String getKeyIdentifier() {
        if (keyId != null && !keyId.isEmpty()) {
            return keyId;
        }
        return keyAlias;
    }
}
