package com.officemate.modules.auth.repository;

import com.officemate.modules.auth.entity.OTPRecord;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Redis repository for OTP storage and retrieval.
 * Uses Spring Data Redis for automatic CRUD operations with TTL support.
 */
@Repository
public interface OTPRepository extends CrudRepository<OTPRecord, String> {

    /**
     * Find OTP record by key (phone number or email)
     * 
     * @param key the key in format "phone:+1234567890" or "email:user@company.com"
     * @return Optional containing OTPRecord if found
     */
    Optional<OTPRecord> findByKey(String key);

    /**
     * Check if OTP record exists for given key
     * 
     * @param key the key to check
     * @return true if exists, false otherwise
     */
    boolean existsByKey(String key);

    /**
     * Delete OTP record by key
     * 
     * @param key the key to delete
     */
    void deleteByKey(String key);
}
