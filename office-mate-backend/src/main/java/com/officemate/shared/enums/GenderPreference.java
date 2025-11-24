package com.officemate.shared.enums;

/**
 * Enum representing rider gender preferences for ride matching.
 * Used to ensure rider safety and comfort preferences are respected.
 */
public enum GenderPreference {
    /**
     * Rider prefers rides with female drivers only
     */
    FEMALE_ONLY,
    
    /**
     * Rider (male) prefers rides with single female passengers
     */
    MALE_SINGLE_FEMALE,
    
    /**
     * Rider (male) prefers rides with all female passengers
     */
    MALE_ALL_FEMALE,
    
    /**
     * Rider has no gender preference for ride matching
     */
    NO_PREFERENCE
}
