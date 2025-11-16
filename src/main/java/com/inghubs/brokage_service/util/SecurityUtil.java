package com.inghubs.brokage_service.util;

import com.inghubs.brokage_service.config.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {
    
    private final JwtUtil jwtUtil;
    
    public SecurityUtil(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }
    
    public Long getAuthenticatedCustomerId(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            return jwtUtil.extractCustomerId(token);
        }
        return null;
    }
    
    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getAuthorities() != null) {
            return authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        }
        return false;
    }
    
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}

