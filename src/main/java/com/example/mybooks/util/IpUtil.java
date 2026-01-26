package com.example.mybooks.util;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    /**
     * Extract real client IP address from request
     *
     * Handles proxies and load balancers:
     * - X-Forwarded-For header (standard)
     * - X-Real-IP header (nginx)
     * - Remote address (fallback)
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        // Check X-Forwarded-For header (may contain multiple IPs)
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty() && !"unknown".equalsIgnoreCase(xff)) {
            // Take the first IP (original client)
            int commaIndex = xff.indexOf(',');
            if (commaIndex != -1) {
                return xff.substring(0, commaIndex).trim();
            }
            return xff.trim();
        }

        // Check X-Real-IP header (nginx)
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }

        // Check Proxy-Client-IP
        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp.trim();
        }

        // Check WL-Proxy-Client-IP (WebLogic)
        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp.trim();
        }

        // Fallback to remote address
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }
}