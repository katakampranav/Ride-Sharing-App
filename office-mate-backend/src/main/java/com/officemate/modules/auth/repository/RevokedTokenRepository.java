package com.officemate.modules.auth.repository;

import com.officemate.modules.auth.entity.RevokedToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for managing revoked tokens in Redis.
 * Used for token blacklisting to prevent use of revoked tokens.
 */
@Repository
public interface RevokedTokenRepository extends CrudRepository<RevokedToken, String> {
    
    /**
     * Check if a token is revoked.
     *
     * @param tokenId the token ID (jti claim)
     * @return true if token exists in revoked list
     */
    boolean existsById(String tokenId);
}
