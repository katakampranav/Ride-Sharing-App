package com.officemate.modules.auth.repository;

import com.officemate.modules.auth.entity.UserSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing user sessions in Redis.
 * Provides CRUD operations and custom queries for session management.
 */
@Repository
public interface UserSessionRepository extends CrudRepository<UserSession, String> {
    
    /**
     * Find all active sessions for a specific user.
     *
     * @param userId the user ID
     * @return list of active sessions
     */
    List<UserSession> findByUserId(String userId);
    
    /**
     * Find session by refresh token.
     *
     * @param refreshToken the refresh token
     * @return optional session
     */
    Optional<UserSession> findByRefreshToken(String refreshToken);
    
    /**
     * Delete all sessions for a specific user.
     *
     * @param userId the user ID
     */
    void deleteByUserId(String userId);
}
