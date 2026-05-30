package co.za.kingstechco.kingstechco.invoeaserapp.util;

import jakarta.servlet.http.HttpServletRequest;

public class TokenExtractor {

    public static String extractTokenFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }

    public static String getClientIp(HttpServletRequest request) {
        String xRealIp = request.getHeader("X-Real-IP");
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xRealIp != null && !xRealIp.isBlank()) return xRealIp;
        if (xForwardedFor != null && !xForwardedFor.isBlank()) return xForwardedFor.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}

