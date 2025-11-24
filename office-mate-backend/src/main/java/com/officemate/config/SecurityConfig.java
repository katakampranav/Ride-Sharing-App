package com.officemate.config;

import com.officemate.config.security.CustomAccessDeniedHandler;
import com.officemate.config.security.CustomAuthenticationEntryPoint;
import com.officemate.config.security.InputSanitizationFilter;
import com.officemate.config.security.JwtAuthenticationFilter;
import com.officemate.config.security.RateLimitingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security configuration for JWT-based authentication.
 * Configures security filters, CORS, rate limiting, and endpoint access rules.
 * 
 * Security Features:
 * - JWT-based stateless authentication
 * - CORS configuration for cross-origin requests
 * - Rate limiting using Redis
 * - Method-level security with verification requirements
 * - Permission-based access control
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final InputSanitizationFilter inputSanitizationFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    /**
     * Configure the security filter chain with comprehensive security settings.
     * 
     * @param http HttpSecurity configuration
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF for stateless JWT authentication
                .csrf(AbstractHttpConfigurer::disable)
                
                // Enable CORS with custom configuration
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                
                // Configure session management as stateless
                .sessionManagement(session -> 
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - authentication not required
                        .requestMatchers(
                                "/auth/register",
                                "/auth/login",
                                "/auth/verify-mobile-otp",
                                "/auth/resend-otp",
                                "/health",
                                "/actuator/health",
                                "/actuator/info",
                                "/error"
                        ).permitAll()
                        
                        // Email verification endpoints - require authentication
                        .requestMatchers(
                                "/auth/add-corporate-email",
                                "/auth/verify-email-otp",
                                "/auth/resend-email-otp",
                                "/auth/update-corporate-email"
                        ).authenticated()
                        
                        // Profile endpoints - require authentication
                        .requestMatchers("/users/**").authenticated()
                        
                        // Admin endpoints - require ADMIN role (future use)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        
                        // All other endpoints require authentication
                        .anyRequest().authenticated()
                )
                
                // Add custom security filters in order of execution
                // Input sanitization filter runs first to clean all input
                .addFilterBefore(inputSanitizationFilter, UsernamePasswordAuthenticationFilter.class)
                
                // Rate limiting filter runs after input sanitization
                .addFilterAfter(rateLimitingFilter, inputSanitizationFilter.getClass())
                
                // JWT authentication filter runs after rate limiting
                .addFilterAfter(jwtAuthenticationFilter, rateLimitingFilter.getClass())
                
                // Configure security headers
                .headers(headers -> headers
                        .contentTypeOptions(contentType -> {})
                        .xssProtection(xss -> {})
                        .cacheControl(cache -> {})
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .frameOptions(frame -> frame.deny())
                )
                
                // Configure exception handling
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        
        return http.build();
    }
}
