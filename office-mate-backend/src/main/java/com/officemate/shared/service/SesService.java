package com.officemate.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

/**
 * Service for sending emails via Amazon SES.
 * Used for corporate email OTP delivery and verification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SesService {

    private final SesClient sesClient;

    @Value("${aws.ses.from-email}")
    private String fromEmail;

    @Value("${aws.ses.from-name}")
    private String fromName;

    @Value("${aws.ses.configuration-set:}")
    private String configurationSet;

    /**
     * Send email to a recipient
     *
     * @param toEmail The recipient email address
     * @param subject The email subject
     * @param body    The email body (HTML or text)
     * @param isHtml  Whether the body is HTML
     * @return Message ID if successful
     */
    public String sendEmail(String toEmail, String subject, String body, boolean isHtml) {
        try {
            Content subjectContent = Content.builder()
                    .data(subject)
                    .build();

            Content bodyContent = Content.builder()
                    .data(body)
                    .build();

            Body emailBody = Body.builder()
                    .html(isHtml ? bodyContent : null)
                    .text(isHtml ? null : bodyContent)
                    .build();

            Message message = Message.builder()
                    .subject(subjectContent)
                    .body(emailBody)
                    .build();

            Destination destination = Destination.builder()
                    .toAddresses(toEmail)
                    .build();

            SendEmailRequest.Builder requestBuilder = SendEmailRequest.builder()
                    .source(String.format("%s <%s>", fromName, fromEmail))
                    .destination(destination)
                    .message(message);

            // Add configuration set if provided
            if (configurationSet != null && !configurationSet.isEmpty()) {
                requestBuilder.configurationSetName(configurationSet);
            }

            SendEmailResponse response = sesClient.sendEmail(requestBuilder.build());
            
            log.info("Email sent successfully to {} with message ID: {}", 
                    maskEmail(toEmail), response.messageId());
            
            return response.messageId();
            
        } catch (SesException e) {
            log.error("Failed to send email to {}: {}", maskEmail(toEmail), e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    /**
     * Send OTP email for corporate email verification
     *
     * @param toEmail The recipient corporate email
     * @param otp     The OTP code
     * @return Message ID if successful
     */
    public String sendOtpEmail(String toEmail, String otp) {
        String subject = "OfficeMate - Verify Your Corporate Email";
        String body = buildOtpEmailBody(otp);
        return sendEmail(toEmail, subject, body, true);
    }

    /**
     * Send email change notification
     *
     * @param toEmail The recipient email
     * @param oldEmail The old corporate email
     * @param newEmail The new corporate email
     * @return Message ID if successful
     */
    public String sendEmailChangeNotification(String toEmail, String oldEmail, String newEmail) {
        String subject = "OfficeMate - Corporate Email Changed";
        String body = buildEmailChangeNotificationBody(oldEmail, newEmail);
        return sendEmail(toEmail, subject, body, true);
    }

    /**
     * Build HTML email body for OTP
     */
    private String buildOtpEmailBody(String otp) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; margin-top: 20px; }
                        .otp-code { font-size: 32px; font-weight: bold; color: #4CAF50; text-align: center; 
                                    padding: 20px; background-color: white; border-radius: 5px; margin: 20px 0; 
                                    letter-spacing: 5px; }
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>OfficeMate</h1>
                        </div>
                        <div class="content">
                            <h2>Verify Your Corporate Email</h2>
                            <p>Thank you for registering with OfficeMate. Please use the following verification code to complete your registration:</p>
                            <div class="otp-code">%s</div>
                            <p><strong>This code will expire in 10 minutes.</strong></p>
                            <p>If you didn't request this verification, please ignore this email.</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from OfficeMate. Please do not reply to this email.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, otp);
    }

    /**
     * Build HTML email body for email change notification
     */
    private String buildEmailChangeNotificationBody(String oldEmail, String newEmail) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background-color: #4CAF50; color: white; padding: 20px; text-align: center; }
                        .content { background-color: #f9f9f9; padding: 30px; border-radius: 5px; margin-top: 20px; }
                        .footer { text-align: center; margin-top: 20px; font-size: 12px; color: #666; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>OfficeMate</h1>
                        </div>
                        <div class="content">
                            <h2>Corporate Email Changed</h2>
                            <p>Your corporate email has been successfully changed.</p>
                            <p><strong>Old Email:</strong> %s</p>
                            <p><strong>New Email:</strong> %s</p>
                            <p>If you didn't make this change, please contact support immediately.</p>
                        </div>
                        <div class="footer">
                            <p>This is an automated message from OfficeMate. Please do not reply to this email.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, maskEmail(oldEmail), maskEmail(newEmail));
    }

    /**
     * Mask email for logging (show only first 2 chars and domain)
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****@****.com";
        }
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domain = parts[1];
        
        if (localPart.length() <= 2) {
            return "**@" + domain;
        }
        return localPart.substring(0, 2) + "****@" + domain;
    }
}
