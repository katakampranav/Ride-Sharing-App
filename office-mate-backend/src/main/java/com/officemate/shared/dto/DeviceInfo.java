package com.officemate.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO containing device information for session tracking.
 * Used to support multi-device session management.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
    
    /**
     * Type of device (IOS, ANDROID, WEB)
     */
    private String deviceType;
    
    /**
     * Unique identifier for the device
     */
    private String deviceId;
    
    /**
     * Application version running on the device
     */
    private String appVersion;
    
    /**
     * Device operating system version
     */
    private String osVersion;
    
    /**
     * Device model/name
     */
    private String deviceModel;
    
    /**
     * IP address of the device
     */
    private String ipAddress;
    
    /**
     * User agent string from the browser/app
     */
    private String userAgent;
}
