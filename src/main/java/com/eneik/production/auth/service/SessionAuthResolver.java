package com.eneik.production.auth.service;

import com.eneik.production.auth.model.UserSession;
import com.eneik.production.auth.repository.UserSessionRepository;
import com.eneik.generated.repository.UserRoleRepository;
import com.eneik.generated.model.UserRole;
import com.eneik.generated.util.TimeProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class SessionAuthResolver {

    private final UserSessionRepository userSessionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;
    private final boolean allowHeaderFallback;

    public SessionAuthResolver(UserSessionRepository userSessionRepository,
                               UserRoleRepository userRoleRepository,
                               PasswordEncoder passwordEncoder,
                               TimeProvider timeProvider,
                               org.springframework.core.env.Environment environment) {
        this.userSessionRepository = userSessionRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.timeProvider = timeProvider;
        this.allowHeaderFallback = environment.acceptsProfiles(org.springframework.core.env.Profiles.of("test"));
    }

    public static class AuthResult {
        private final UUID userId;
        private final String role;
        private final boolean authenticated;

        public AuthResult(UUID userId, String role, boolean authenticated) {
            this.userId = userId;
            this.role = role;
            this.authenticated = authenticated;
        }

        public UUID getUserId() { return userId; }
        public String getRole() { return role; }
        public boolean isAuthenticated() { return authenticated; }
    }

    public AuthResult resolveAuth(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null && !token.isEmpty()) {
            String hashedToken = hashToken(token);
            java.util.Optional<UserSession> sessionOpt = userSessionRepository.findByTokenHash(hashedToken);
            if (sessionOpt.isPresent()) {
                UserSession session = sessionOpt.get();
                if ("ACTIVE".equals(session.getStatus()) && session.getExpiresAt().isAfter(timeProvider.now())) {
                    // Fetch user's roles
                    List<UserRole> roles = userRoleRepository.findByUserId(session.getUserId());
                    String resolvedRole = null;
                    if (!roles.isEmpty()) {
                        resolvedRole = roles.get(0).getRoleName();
                    }
                    return new AuthResult(session.getUserId(), resolvedRole, true);
                }
            }
            // If a token was provided but is invalid/expired, we block unverified access!
            return new AuthResult(null, null, false);
        }

        // If NO session token was provided, fall back to headers for backward compatibility (ONLY in tests!)
        if (allowHeaderFallback) {
            String xUserRole = request.getHeader("X-User-Role");
            String xUserIdHeader = request.getHeader("X-User-Id");

            UUID userId = null;
            if (xUserIdHeader != null && !xUserIdHeader.trim().isEmpty()) {
                try {
                    userId = UUID.fromString(xUserIdHeader.trim());
                } catch (IllegalArgumentException e) {
                    // Ignore invalid UUID
                }
            }

            // Check Authorization Bearer fallback (old way in tests)
            String authHeader = request.getHeader("Authorization");
            String headerRole = xUserRole;
            if (headerRole == null && authHeader != null && authHeader.startsWith("Bearer ")) {
                String bToken = authHeader.substring(7).trim();
                if (!bToken.isEmpty() && bToken.length() < 30) { // short bearer tokens represent roles in old tests
                    headerRole = bToken;
                }
            }

            if (xUserIdHeader != null || headerRole != null) {
                return new AuthResult(userId, headerRole != null ? headerRole.trim() : null, true);
            }
        }

        return new AuthResult(null, null, false);
    }

    public boolean hasSessionToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("session_token".equals(cookie.getName())) {
                    return true;
                }
            }
        }
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty() && token.length() >= 30) {
                return true;
            }
        }
        return false;
    }

    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash token", e);
        }
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Try Cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("session_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // 2. Try Authorization Header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty() && token.length() >= 30) {
                return token;
            }
        }
        return null;
    }
}
