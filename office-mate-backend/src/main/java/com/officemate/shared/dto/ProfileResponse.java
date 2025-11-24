package com.officemate.shared.dto;

import com.officemate.shared.enums.AccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response DTO for user profile information.
 * Contains basic profile data and verification status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    
    /**
     * Unique user identifier
     */
    private String userId;
    
    /**
     * User's first name
     */
    private String firstName;
    
    /**
     * User's last name
     */
    private String lastName;
    
    /**
     * User's phone number
     */
    private String phoneNumber;
    
    /**
     * User's corporate email
     */
    private String corporateEmail;
    
    /**
     * Profile image URL
     */
    private String profileImageUrl;
    
    /**
     * Date of birth
     */
    private LocalDate dateOfBirth;
    
    /**
     * Gender
     */
    private String gender;
    
    /**
     * Mobile verification status
     */
    private boolean mobileVerified;
    
    /**
     * Email verification status
     */
    private boolean emailVerified;
    
    /**
     * Account status
     */
    private AccountStatus accountStatus;
    
    /**
     * Flag indicating if user has driver capabilities
     */
    private boolean isDriver;
    
    /**
     * Flag indicating if user has rider capabilities
     */
    private boolean isRider;
    
    /**
     * Flag indicating if user can access ride features
     */
    private boolean canAccessRideFeatures;
    
    /**
     * Flag indicating if user has a wallet set up
     */
    private boolean hasWallet;
    
    /**
     * Wallet ID if wallet exists
     */
    private String walletId;
    
    /**
     * Current wallet balance (null if no wallet)
     */
    private java.math.BigDecimal walletBalance;
    
    /**
     * Profile creation timestamp
     */
    private LocalDateTime createdAt;
    
    /**
     * Last profile update timestamp
     */
    private LocalDateTime updatedAt;
}
