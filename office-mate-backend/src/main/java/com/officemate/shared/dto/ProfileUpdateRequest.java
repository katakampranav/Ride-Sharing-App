package com.officemate.shared.dto;

import com.officemate.shared.validation.SafeText;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request DTO for updating basic user profile information.
 * All fields are optional - only provided fields will be updated.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    
    /**
     * User's first name
     */
    @SafeText(maxLength = 50, allowSpecialChars = false, 
              allowedPattern = "^[a-zA-Z\\s'-]+$", 
              message = "First name must contain only letters, spaces, hyphens, and apostrophes")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;
    
    /**
     * User's last name
     */
    @SafeText(maxLength = 50, allowSpecialChars = false, 
              allowedPattern = "^[a-zA-Z\\s'-]+$", 
              message = "Last name must contain only letters, spaces, hyphens, and apostrophes")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;
    
    /**
     * Date of birth
     */
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    
    /**
     * Gender (MALE, FEMALE, OTHER)
     */
    @Pattern(regexp = "^(MALE|FEMALE|OTHER)$", message = "Gender must be MALE, FEMALE, or OTHER")
    private String gender;
    
    /**
     * Profile image URL
     */
    @SafeText(maxLength = 500, message = "Profile image URL is too long")
    @Pattern(regexp = "^https?://.*\\.(jpg|jpeg|png|gif|webp)$", 
             message = "Profile image URL must be a valid HTTP/HTTPS URL ending with jpg, jpeg, png, gif, or webp")
    private String profileImageUrl;
}
