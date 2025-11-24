package com.officemate.shared.validation;

import com.officemate.shared.dto.VehicleInfoDTO;
import com.officemate.shared.enums.FuelType;
import com.officemate.shared.enums.VehicleType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Set;

/**
 * Validator for vehicle type and related constraints.
 * Validates vehicle type, capacity, and fuel type compatibility.
 */
public class VehicleTypeValidator implements ConstraintValidator<VehicleTypeValid, Object> {
    
    private static final Logger log = LoggerFactory.getLogger(VehicleTypeValidator.class);
    
    private boolean validateCapacity;
    private boolean validateFuelType;
    
    // Valid fuel types for each vehicle type
    private static final Set<FuelType> CAR_FUEL_TYPES = EnumSet.of(
        FuelType.PETROL, FuelType.DIESEL, FuelType.ELECTRIC, FuelType.HYBRID, FuelType.CNG
    );
    
    private static final Set<FuelType> MOTORCYCLE_FUEL_TYPES = EnumSet.of(
        FuelType.PETROL, FuelType.ELECTRIC
    );
    
    private static final Set<FuelType> SCOOTER_FUEL_TYPES = EnumSet.of(
        FuelType.PETROL, FuelType.ELECTRIC
    );
    
    private static final Set<FuelType> BICYCLE_FUEL_TYPES = EnumSet.of(
        FuelType.ELECTRIC // Only electric bicycles, regular bicycles don't need fuel type
    );
    
    @Override
    public void initialize(VehicleTypeValid constraintAnnotation) {
        this.validateCapacity = constraintAnnotation.validateCapacity();
        this.validateFuelType = constraintAnnotation.validateFuelType();
    }
    
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        // Handle VehicleInfoDTO validation
        if (value instanceof VehicleInfoDTO) {
            return validateVehicleInfo((VehicleInfoDTO) value, context);
        }
        
        // Handle individual VehicleType validation
        if (value instanceof VehicleType) {
            return validateVehicleType((VehicleType) value, context);
        }
        
        return true;
    }
    
    private boolean validateVehicleInfo(VehicleInfoDTO vehicleInfo, ConstraintValidatorContext context) {
        boolean isValid = true;
        
        VehicleType vehicleType = vehicleInfo.getVehicleType();
        FuelType fuelType = vehicleInfo.getFuelType();
        Integer capacity = vehicleInfo.getCapacity();
        
        if (vehicleType == null) {
            return true; // Let @NotNull handle this
        }
        
        // Validate capacity based on vehicle type
        if (validateCapacity && capacity != null) {
            if (!isValidCapacity(vehicleType, capacity)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    getCapacityErrorMessage(vehicleType)
                ).addConstraintViolation();
                isValid = false;
            }
        }
        
        // Validate fuel type compatibility
        if (validateFuelType && fuelType != null) {
            if (!isValidFuelType(vehicleType, fuelType)) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(
                    getFuelTypeErrorMessage(vehicleType, fuelType)
                ).addConstraintViolation();
                isValid = false;
            }
        }
        
        return isValid;
    }
    
    private boolean validateVehicleType(VehicleType vehicleType, ConstraintValidatorContext context) {
        // Basic validation - ensure it's a valid enum value
        try {
            VehicleType.valueOf(vehicleType.name());
            return true;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid vehicle type: {}", vehicleType);
            return false;
        }
    }
    
    private boolean isValidCapacity(VehicleType vehicleType, Integer capacity) {
        switch (vehicleType) {
            case CAR:
                return capacity >= 1 && capacity <= 7;
            case MOTORCYCLE:
            case SCOOTER:
                return capacity >= 1 && capacity <= 2;
            case BICYCLE:
                return capacity == 1; // Bicycles can only carry 1 person
            default:
                return false;
        }
    }
    
    private boolean isValidFuelType(VehicleType vehicleType, FuelType fuelType) {
        switch (vehicleType) {
            case CAR:
                return CAR_FUEL_TYPES.contains(fuelType);
            case MOTORCYCLE:
                return MOTORCYCLE_FUEL_TYPES.contains(fuelType);
            case SCOOTER:
                return SCOOTER_FUEL_TYPES.contains(fuelType);
            case BICYCLE:
                return BICYCLE_FUEL_TYPES.contains(fuelType);
            default:
                return false;
        }
    }
    
    private String getCapacityErrorMessage(VehicleType vehicleType) {
        switch (vehicleType) {
            case CAR:
                return "Car capacity must be between 1 and 7 passengers";
            case MOTORCYCLE:
            case SCOOTER:
                return vehicleType.name().toLowerCase() + " capacity must be between 1 and 2 passengers";
            case BICYCLE:
                return "Bicycle capacity must be 1 passenger";
            default:
                return "Invalid capacity for vehicle type";
        }
    }
    
    private String getFuelTypeErrorMessage(VehicleType vehicleType, FuelType fuelType) {
        switch (vehicleType) {
            case CAR:
                return "Cars support PETROL, DIESEL, ELECTRIC, HYBRID, or CNG fuel types";
            case MOTORCYCLE:
            case SCOOTER:
                return vehicleType.name().toLowerCase() + "s support PETROL or ELECTRIC fuel types";
            case BICYCLE:
                return "Bicycles only support ELECTRIC fuel type";
            default:
                return "Invalid fuel type " + fuelType + " for vehicle type " + vehicleType;
        }
    }
}