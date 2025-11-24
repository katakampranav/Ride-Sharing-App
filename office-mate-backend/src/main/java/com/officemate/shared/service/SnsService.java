package com.officemate.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending SMS messages via Amazon SNS.
 * Used for mobile OTP delivery during authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SnsService {

    private final SnsClient snsClient;

    @Value("${aws.sns.sender-id}")
    private String senderId;

    @Value("${aws.sns.sms-type}")
    private String smsType;

    @Value("${aws.sns.max-price}")
    private String maxPrice;

    /**
     * Send SMS message to a phone number
     *
     * @param phoneNumber The recipient phone number in E.164 format (e.g., +1234567890)
     * @param message     The message content
     * @return Message ID if successful
     */
    public String sendSms(String phoneNumber, String message) {
        try {
            Map<String, MessageAttributeValue> smsAttributes = new HashMap<>();
            
            // Set SMS type (Transactional for OTP)
            smsAttributes.put("AWS.SNS.SMS.SMSType", MessageAttributeValue.builder()
                    .stringValue(smsType)
                    .dataType("String")
                    .build());
            
            // Set sender ID
            smsAttributes.put("AWS.SNS.SMS.SenderID", MessageAttributeValue.builder()
                    .stringValue(senderId)
                    .dataType("String")
                    .build());
            
            // Set max price
            smsAttributes.put("AWS.SNS.SMS.MaxPrice", MessageAttributeValue.builder()
                    .stringValue(maxPrice)
                    .dataType("String")
                    .build());

            PublishRequest request = PublishRequest.builder()
                    .phoneNumber(phoneNumber)
                    .message(message)
                    .messageAttributes(smsAttributes)
                    .build();

            PublishResponse response = snsClient.publish(request);
            
            log.info("SMS sent successfully to {} with message ID: {}", 
                    maskPhoneNumber(phoneNumber), response.messageId());
            
            return response.messageId();
            
        } catch (SnsException e) {
            log.error("Failed to send SMS to {}: {}", maskPhoneNumber(phoneNumber), e.getMessage(), e);
            throw new RuntimeException("Failed to send SMS: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Send OTP SMS message
     *
     * @param phoneNumber The recipient phone number
     * @param otp         The OTP code
     * @return Message ID if successful
     */
    public String sendOtpSms(String phoneNumber, String otp) {
        String message = String.format("Your OfficeMate verification code is: %s. Valid for 5 minutes.", otp);
        return sendSms(phoneNumber, message);
    }

    /**
     * Mask phone number for logging (show only last 4 digits)
     */
    private String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 4) {
            return "****";
        }
        return "****" + phoneNumber.substring(phoneNumber.length() - 4);
    }
}
