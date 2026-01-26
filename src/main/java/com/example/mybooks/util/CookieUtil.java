package com.example.mybooks.util;

import jakarta.servlet.http.Cookie;

public class CookieUtil {

    /**
     * Creates a secure cookie with all security attributes
     *
     * Task 2.5: Secure cookie attributes
     * - HttpOnly: Prevents JavaScript access (XSS protection)
     * - Secure: Only sent over HTTPS (in production)
     * - SameSite: Prevents CSRF attacks
     */
    public static Cookie createSecureCookie(String name, String value, int maxAge, boolean secure) {
        Cookie cookie = new Cookie(name, value);

        // HttpOnly: Cannot be accessed via JavaScript
        cookie.setHttpOnly(true);

        // Secure: Only sent over HTTPS (set to false for development, true for production)
        cookie.setSecure(secure);

        // Path: Available throughout the application
        cookie.setPath("/");

        // MaxAge: How long the cookie should persist (in seconds)
        cookie.setMaxAge(maxAge);

        // Note: SameSite attribute must be set via Set-Cookie header
        // This is handled in the response directly

        return cookie;
    }

    /**
     * Creates a JWT access token cookie
     * Short-lived (15 minutes)
     */
    public static Cookie createJwtCookie(String token, boolean secure) {
        return createSecureCookie("jwt", token, 15 * 60, secure); // 15 minutes
    }

    /**
     * Creates a refresh token cookie
     * Long-lived (7 days)
     */
    public static Cookie createRefreshTokenCookie(String token, boolean secure) {
        return createSecureCookie("refreshToken", token, 7 * 24 * 60 * 60, secure); // 7 days
    }

    /**
     * Deletes a cookie by setting maxAge to 0
     */
    public static Cookie deleteCookie(String name) {
        return createSecureCookie(name, null, 0, false);
    }
}