package com.officemate.modules.profile.entity;

import com.officemate.modules.auth.entity.UserAccount;
import com.officemate.shared.entity.AuditableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * JPA Entity representing a user profile in the PostgreSQL database.
 * Contains basic profile information for all users.
 * Requires both mobile and email verification before creation.
 */
@Entity
@Table(name = "user_profiles")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile extends AuditableEntity {

    /**
     * Unique identifier for the user profile (same as UserAccount userId)
     */
    @Id
    @Column(name = "user_id", updatable = false, nullable = false)
    private UUID userId;

    /**
     * One-to-one relationship with UserAccount
     */
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount userAccount;

    /**
     * User's first name
     */
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /**
     * User's last name
     */
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /**
     * URL to user's profile image
     */
    @Size(max = 500, message = "Profile image URL must not exceed 500 characters")
    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /**
     * User's date of birth
     */
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /**
     * User's gender (MALE, FEMALE, OTHER)
     */
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "Gender must be MALE, FEMALE, or OTHER")
    @Column(name = "gender", length = 10)
    private String gender;



    /**
     * Gets the user's full name
     * 
     * @return Full name (first name + last name)
     */
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Checks if the profile is complete with all required information
     * 
     * @return true if all required fields are populated
     */
    public boolean isComplete() {
        return firstName != null && !firstName.isBlank() 
            && lastName != null && !lastName.isBlank();
    }
}
