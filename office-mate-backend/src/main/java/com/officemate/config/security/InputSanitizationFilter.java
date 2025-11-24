package com.officemate.config.security;

import com.officemate.shared.util.InputSanitizer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Filter for input sanitization and XSS prevention.
 * Sanitizes all request parameters and headers before processing.
 */
@Component
public class InputSanitizationFilter implements Filter {
    
    private static final Logger log = LoggerFactory.getLogger(InputSanitizationFilter.class);
    private final InputSanitizer inputSanitizer;
    
    public InputSanitizationFilter(InputSanitizer inputSanitizer) {
        this.inputSanitizer = inputSanitizer;
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Input sanitization filter initialized");
    }
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            SanitizedHttpServletRequestWrapper sanitizedRequest = 
                new SanitizedHttpServletRequestWrapper(httpRequest, inputSanitizer);
            chain.doFilter(sanitizedRequest, response);
        } else {
            chain.doFilter(request, response);
        }
    }
    
    @Override
    public void destroy() {
        log.info("Input sanitization filter destroyed");
    }
    
    /**
     * Wrapper class that sanitizes request parameters and headers.
     */
    private static class SanitizedHttpServletRequestWrapper extends HttpServletRequestWrapper {
        
        private final InputSanitizer inputSanitizer;
        private final Map<String, String[]> sanitizedParameters;
        private final Map<String, String> sanitizedHeaders;
        
        public SanitizedHttpServletRequestWrapper(HttpServletRequest request, InputSanitizer inputSanitizer) {
            super(request);
            this.inputSanitizer = inputSanitizer;
            this.sanitizedParameters = sanitizeParameters(request);
            this.sanitizedHeaders = sanitizeHeaders(request);
        }
        
        @Override
        public String getParameter(String name) {
            String[] values = sanitizedParameters.get(name);
            return values != null && values.length > 0 ? values[0] : null;
        }
        
        @Override
        public String[] getParameterValues(String name) {
            return sanitizedParameters.get(name);
        }
        
        @Override
        public Map<String, String[]> getParameterMap() {
            return Collections.unmodifiableMap(sanitizedParameters);
        }
        
        @Override
        public Enumeration<String> getParameterNames() {
            return Collections.enumeration(sanitizedParameters.keySet());
        }
        
        @Override
        public String getHeader(String name) {
            // Only sanitize specific headers that might contain user input
            if (shouldSanitizeHeader(name)) {
                return sanitizedHeaders.get(name.toLowerCase());
            }
            return super.getHeader(name);
        }
        
        private Map<String, String[]> sanitizeParameters(HttpServletRequest request) {
            Map<String, String[]> sanitized = new HashMap<>();
            Map<String, String[]> originalParams = request.getParameterMap();
            
            for (Map.Entry<String, String[]> entry : originalParams.entrySet()) {
                String paramName = entry.getKey();
                String[] paramValues = entry.getValue();
                
                if (paramValues != null) {
                    String[] sanitizedValues = new String[paramValues.length];
                    for (int i = 0; i < paramValues.length; i++) {
                        sanitizedValues[i] = sanitizeParameterValue(paramName, paramValues[i]);
                    }
                    sanitized.put(paramName, sanitizedValues);
                }
            }
            
            return sanitized;
        }
        
        private Map<String, String> sanitizeHeaders(HttpServletRequest request) {
            Map<String, String> sanitized = new HashMap<>();
            
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                if (shouldSanitizeHeader(headerName)) {
                    String headerValue = request.getHeader(headerName);
                    if (headerValue != null) {
                        sanitized.put(headerName.toLowerCase(), 
                                    inputSanitizer.sanitizeText(headerValue));
                    }
                }
            }
            
            return sanitized;
        }
        
        private String sanitizeParameterValue(String paramName, String paramValue) {
            if (paramValue == null) {
                return null;
            }
            
            // Apply specific sanitization based on parameter name
            switch (paramName.toLowerCase()) {
                case "phonenumber":
                case "phone":
                    return inputSanitizer.sanitizePhoneNumber(paramValue);
                case "email":
                case "corporateemail":
                    return inputSanitizer.sanitizeEmail(paramValue);
                case "licenseplate":
                    return inputSanitizer.sanitizeLicensePlate(paramValue);
                default:
                    return inputSanitizer.sanitizeText(paramValue);
            }
        }
        
        private boolean shouldSanitizeHeader(String headerName) {
            if (headerName == null) {
                return false;
            }
            
            // List of headers that might contain user input and should be sanitized
            List<String> headersToSanitize = Arrays.asList(
                "user-agent", "referer", "x-forwarded-for", "x-real-ip",
                "x-requested-with", "accept-language"
            );
            
            return headersToSanitize.contains(headerName.toLowerCase());
        }
    }
}