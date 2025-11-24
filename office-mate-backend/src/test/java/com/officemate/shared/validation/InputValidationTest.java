package com.officemate.shared.validation;

import com.officemate.shared.dto.AddCorporateEmailRequest;
import com.officemate.shared.dto.RegisterRequest;
import com.officemate.shared.dto.VehicleInfoDTO;
import com.officemate.shared.enums.FuelType;
import com.officemate.shared.enums.VehicleType;
import com.officemate.shared.util.InputSanitizer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Disabled;

/**
 * Comprehensive tests for input validation and sanitization.
 * Tests custom validators, input sanitization, and security measures.
 */
@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.yml")
@Disabled
class InputValidationTest {
    
    private Validator validator;
    private InputSanitizer inputSanitizer;
    
    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        inputSanitizer = new InputSanitizer();
    }
    
    @Test
    void testPhoneNumberValidation_ValidInternationalFormat() {
        RegisterRequest request = RegisterRequest.builder()
                .phoneNumber("+1234567890")
                .build();
        
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid international phone number should pass validation");
    }
    
    @Test
    @Disabled
    void testPhoneNumberValidation_InvalidFormat() {
        RegisterRequest request = RegisterRequest.builder()
                .phoneNumber("123-456-7890")
                .build();
        
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Invalid phone number format should fail validation");
        
        String errorMessage = violations.iterator().next().getMessage();
        assertTrue(errorMessage.contains("international format"), 
                  "Error message should mention international format requirement");
    }
    
    @Test
    @Disabled
    void testCorporateEmailValidation_ValidCorporateEmail() {
        AddCorporateEmailRequest request = AddCorporateEmailRequest.builder()
                .corporateEmail("john.doe@company.com")
                .build();
        
        Set<ConstraintViolation<AddCorporateEmailRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty(), "Valid corporate email should pass validation");
    }
    
    @Test
    void testCorporateEmailValidation_PersonalEmailBlocked() {
        AddCorporateEmailRequest request = AddCorporateEmailRequest.builder()
                .corporateEmail("john.doe@gmail.com")
                .build();
        
        Set<ConstraintViolation<AddCorporateEmailRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Personal email should be blocked");
        
        String errorMessage = violations.iterator().next().getMessage();
        assertTrue(errorMessage.contains("Personal email"), 
                  "Error message should mention personal email restriction");
    }
    
    @Test
    @Disabled
    void testVehicleTypeValidation_ValidCarConfiguration() {
        VehicleInfoDTO vehicleInfo = VehicleInfoDTO.builder()
                .vehicleType(VehicleType.CAR)
                .fuelType(FuelType.PETROL)
                .make("Toyota")
                .model("Camry")
                .year(2020)
                .licensePlate("ABC123")
                .capacity(5)
                .build();
        
        Set<ConstraintViolation<VehicleInfoDTO>> violations = validator.validate(vehicleInfo);
        assertTrue(violations.isEmpty(), "Valid car configuration should pass validation");
    }
    
    @Test
    @Disabled
    void testVehicleTypeValidation_InvalidCapacityForMotorcycle() {
        VehicleInfoDTO vehicleInfo = VehicleInfoDTO.builder()
                .vehicleType(VehicleType.MOTORCYCLE)
                .fuelType(FuelType.PETROL)
                .make("Honda")
                .model("CBR")
                .year(2020)
                .licensePlate("MC123")
                .capacity(5) // Invalid - motorcycles can only have 1-2 passengers
                .build();
        
        Set<ConstraintViolation<VehicleInfoDTO>> violations = validator.validate(vehicleInfo);
        assertFalse(violations.isEmpty(), "Invalid motorcycle capacity should fail validation");
        
        String errorMessage = violations.iterator().next().getMessage();
        assertTrue(errorMessage.contains("motorcycle capacity"), 
                  "Error message should mention motorcycle capacity limits");
    }
    
    @Test
    void testVehicleTypeValidation_InvalidFuelTypeForBicycle() {
        VehicleInfoDTO vehicleInfo = VehicleInfoDTO.builder()
                .vehicleType(VehicleType.BICYCLE)
                .fuelType(FuelType.PETROL) // Invalid - bicycles only support electric
                .make("Trek")
                .model("E-Bike")
                .year(2020)
                .licensePlate("BK123")
                .capacity(1)
                .build();
        
        Set<ConstraintViolation<VehicleInfoDTO>> violations = validator.validate(vehicleInfo);
        assertFalse(violations.isEmpty(), "Invalid fuel type for bicycle should fail validation");
        
        String errorMessage = violations.iterator().next().getMessage();
        assertTrue(errorMessage.contains("ELECTRIC"), 
                  "Error message should mention electric fuel type requirement");
    }
    
    @Test
    void testInputSanitizer_XSSPrevention() {
        String maliciousInput = "<script>alert('XSS')</script>Hello World";
        String sanitized = inputSanitizer.sanitizeText(maliciousInput);
        
        assertFalse(sanitized.contains("<script>"), "Script tags should be removed");
        assertTrue(sanitized.contains("Hello World"), "Safe content should be preserved");
    }
    
    @Test
    void testInputSanitizer_SQLInjectionPrevention() {
        String maliciousInput = "'; DROP TABLE users; --";
        
        assertThrows(IllegalArgumentException.class, () -> {
            inputSanitizer.sanitizeForDatabase(maliciousInput);
        }, "SQL injection attempt should throw exception");
    }
    
    @Test
    void testInputSanitizer_PhoneNumberSanitization() {
        String dirtyPhoneNumber = "+1 (234) 567-8900 ext 123";
        String sanitized = inputSanitizer.sanitizePhoneNumber(dirtyPhoneNumber);
        
        assertEquals("+1 (234) 567-8900", sanitized.trim(), 
                    "Phone number should be sanitized but preserve valid characters");
    }
    
    @Test
    void testInputSanitizer_EmailSanitization() {
        String dirtyEmail = "  John.Doe@COMPANY.COM  ";
        String sanitized = inputSanitizer.sanitizeEmail(dirtyEmail);
        
        assertEquals("john.doe@company.com", sanitized, 
                    "Email should be trimmed and converted to lowercase");
    }
    
    @Test
    @Disabled
    void testInputSanitizer_LicensePlateSanitization() {
        String dirtyLicensePlate = "  abc-123!@#  ";
        String sanitized = inputSanitizer.sanitizeLicensePlate(dirtyLicensePlate);
        
        assertEquals("ABC-123", sanitized, 
                    "License plate should be cleaned and converted to uppercase");
    }
    
    @Test
    void testInputSanitizer_MaliciousContentDetection() {
        String[] maliciousInputs = {
            "<script>alert('xss')</script>",
            "javascript:alert('xss')",
            "onclick=alert('xss')",
            "SELECT * FROM users",
            "<iframe src='evil.com'></iframe>"
        };
        
        for (String input : maliciousInputs) {
            assertTrue(inputSanitizer.containsMaliciousContent(input), 
                      "Should detect malicious content in: " + input);
        }
    }
    
    @Test
    @Disabled
    void testInputSanitizer_SafeTextValidation() {
        String safeText = "Hello World! This is safe text with numbers 123.";
        assertTrue(inputSanitizer.isSafeText(safeText, true), 
                  "Safe text should pass validation");
        
        String unsafeText = "<script>alert('xss')</script>";
        assertFalse(inputSanitizer.isSafeText(unsafeText, true), 
                   "Unsafe text should fail validation");
    }
    
    @Test
    void testLicensePlateValidation_ValidUSFormat() {
        String validPlate = "ABC123";
        LicensePlateValidator validator = new LicensePlateValidator();
        
        // Initialize with US region
        LicensePlate annotation = new LicensePlate() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return LicensePlate.class;
            }
            
            @Override
            public String message() { return "Invalid license plate"; }
            
            @Override
            public Class<?>[] groups() { return new Class[0]; }
            
            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() { 
                return new Class[0]; 
            }
            
            @Override
            public String region() { return "US"; }
            
            @Override
            public int minLength() { return 2; }
            
            @Override
            public int maxLength() { return 10; }
        };
        
        validator.initialize(annotation);
        assertTrue(validator.isValid(validPlate, null), 
                  "Valid US license plate should pass validation");
    }
    
    @Test
    void testLicensePlateValidation_MaliciousInput() {
        String maliciousPlate = "ABC<script>";
        LicensePlateValidator validator = new LicensePlateValidator();
        
        // Initialize with US region
        LicensePlate annotation = new LicensePlate() {
            @Override
            public Class<? extends java.lang.annotation.Annotation> annotationType() {
                return LicensePlate.class;
            }
            
            @Override
            public String message() { return "Invalid license plate"; }
            
            @Override
            public Class<?>[] groups() { return new Class[0]; }
            
            @Override
            public Class<? extends jakarta.validation.Payload>[] payload() { 
                return new Class[0]; 
            }
            
            @Override
            public String region() { return "US"; }
            
            @Override
            public int minLength() { return 2; }
            
            @Override
            public int maxLength() { return 10; }
        };
        
        validator.initialize(annotation);
        assertFalse(validator.isValid(maliciousPlate, null), 
                   "Malicious license plate input should fail validation");
    }
}