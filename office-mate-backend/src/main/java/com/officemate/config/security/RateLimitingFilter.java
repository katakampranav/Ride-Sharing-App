package com.officemate.config.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiting filter to prevent abuse and DoS attacks.
 * Implements basic in-memory rate limiting by IP address.
 */
@Component
public class RateLimitingFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
    private static final long WINDOW_SIZE_MS = 60000; // 1 minute
    
    private final ConcurrentHashMap<String, RequestWindow> requestCounts = new ConcurrentHashMap<>();
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Rate limiting filter initialized with {} requests per minute", MAX_REQUESTS_PER_MINUTE);
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            
            String clientIp = getClientIpAddress(httpRequest);
            
            if (isRateLimited(clientIp)) {
                log.warn("Rate limit exceeded for IP: {}", clientIp);
                httpResponse.setStatus(429); // Too Many Requests
                httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded\",\"message\":\"Too many requests\"}");
                return;
            }
        }
        
        chain.doFilter(request, response);
    }
    
    @Override
    public void destroy() {
        log.info("Rate limiting filter destroyed");
    }
    
    private boolean isRateLimited(String clientIp) {
        long currentTime = System.currentTimeMillis();
        
        RequestWindow window = requestCounts.computeIfAbsent(clientIp, k -> new RequestWindow(currentTime));
        
        // Reset window if expired
        if (currentTime - window.windowStart > WINDOW_SIZE_MS) {
            window.windowStart = currentTime;
            window.requestCount.set(0);
        }
        
        int currentCount = window.requestCount.incrementAndGet();
        return currentCount > MAX_REQUESTS_PER_MINUTE;
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private static class RequestWindow {
        volatile long windowStart;
        final AtomicInteger requestCount;
        
        RequestWindow(long windowStart) {
            this.windowStart = windowStart;
            this.requestCount = new AtomicInteger(0);
        }
    }
}