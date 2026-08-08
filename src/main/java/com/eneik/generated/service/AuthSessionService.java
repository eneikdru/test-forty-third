package com.eneik.generated.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthSessionService {

    // Token -> SessionInfo map
    private final Map<String, SessionInfo> sessionStore = new ConcurrentHashMap<>();

    // Username -> UserCredentials map
    private static final Map<String, UserCredentials> CREDENTIALS_STORE = new ConcurrentHashMap<>();

    static {
        CREDENTIALS_STORE.put("economist", new UserCredentials(
                "economist",
                "economist_pass",
                "Economist",
                UUID.fromString("e0000000-0000-0000-0000-000000000001")
        ));
        CREDENTIALS_STORE.put("teacher", new UserCredentials(
                "teacher",
                "teacher_pass",
                "Teacher",
                UUID.fromString("e0000000-0000-0000-0000-000000000002")
        ));
        CREDENTIALS_STORE.put("postgraduate", new UserCredentials(
                "postgraduate",
                "postgraduate_pass",
                "Postgraduate",
                UUID.fromString("e0000000-0000-0000-0000-000000000003")
        ));
        CREDENTIALS_STORE.put("admin", new UserCredentials(
                "admin",
                "admin_pass",
                "Admin",
                UUID.fromString("e0000000-0000-0000-0000-000000000004")
        ));
        CREDENTIALS_STORE.put("hr", new UserCredentials(
                "hr",
                "hr_pass",
                "hr",
                UUID.fromString("e0000000-0000-0000-0000-000000000005")
        ));
    }

    /**
     * Authenticates a user with corporate credentials and returns a secure token.
     */
    public String login(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        UserCredentials creds = CREDENTIALS_STORE.get(username.trim().toLowerCase());
        if (creds != null && creds.getPassword().equals(password)) {
            String token = "sess-" + UUID.randomUUID().toString();
            sessionStore.put(token, new SessionInfo(creds.getUsername(), creds.getRole(), creds.getUserId()));
            return token;
        }
        return null;
    }

    /**
     * Resolves session info from a token (with backward compatibility for mock role-as-token headers).
     */
    public SessionInfo getSession(String token) {
        if (token == null) {
            return null;
        }
        SessionInfo info = sessionStore.get(token);
        if (info != null) {
            return info;
        }

        // Backward compatibility fallback for mock HTTP header testing:
        // If the token matches a known role, return a mock session.
        String lower = token.trim().toLowerCase();
        if (lower.equals("economist") || lower.equals("teacher") || lower.equals("postgraduate") ||
                lower.equals("admin") || lower.equals("hr") || lower.equals("administrator") ||
                lower.equals("content_manager") || lower.equals("student")) {
            String roleName = token.trim();
            // Deterministic UUID for the role
            UUID fallbackId = UUID.nameUUIDFromBytes(roleName.getBytes());
            return new SessionInfo("mock_" + lower, roleName, fallbackId);
        }

        return null;
    }

    public void logout(String token) {
        if (token != null) {
            sessionStore.remove(token);
        }
    }

    public static class SessionInfo {
        private final String username;
        private final String role;
        private final UUID userId;

        public SessionInfo(String username, String role, UUID userId) {
            this.username = username;
            this.role = role;
            this.userId = userId;
        }

        public String getUsername() { return username; }
        public String getRole() { return role; }
        public UUID getUserId() { return userId; }
    }

    private static class UserCredentials {
        private final String username;
        private final String password;
        private final String role;
        private final UUID userId;

        public UserCredentials(String username, String password, String role, UUID userId) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.userId = userId;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
        public UUID getUserId() { return userId; }
    }
}
