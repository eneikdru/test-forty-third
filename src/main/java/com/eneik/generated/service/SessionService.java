package com.eneik.generated.service;

import com.eneik.generated.model.User;
import com.eneik.generated.model.UserRole;
import com.eneik.generated.repository.UserRepository;
import com.eneik.generated.repository.UserRoleRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class SessionService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();

    private Supplier<String> tokenSupplier = () -> {
        byte[] randomBytes = new byte[32];
        new SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    };

    public SessionService(UserRepository userRepository, UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // Allow injecting a token supplier for deterministic test execution
    public void setTokenSupplier(Supplier<String> tokenSupplier) {
        this.tokenSupplier = tokenSupplier;
    }

    public Optional<LoginResult> login(String username, String password) {
        if (username == null || password == null) {
            return Optional.empty();
        }

        Optional<User> userOpt = userRepository.findByUsername(username.toLowerCase().trim());
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return Optional.empty();
        }

        List<UserRole> roles = userRoleRepository.findByUserId(user.getId());
        String role = roles.isEmpty() ? "Postgraduate" : roles.get(0).getRoleName();

        String token = tokenSupplier.get();
        // Session valid for 2 hours
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);

        UserSession session = new UserSession(user.getId(), user.getUsername(), role, expiresAt);
        activeSessions.put(token, session);

        return Optional.of(new LoginResult(token, user.getUsername(), role));
    }

    public Optional<UserSession> getSession(String token) {
        if (token == null) {
            return Optional.empty();
        }
        UserSession session = activeSessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            activeSessions.remove(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    public void logout(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }

    public static class UserSession {
        private final UUID userId;
        private final String username;
        private final String role;
        private final LocalDateTime expiresAt;

        public UserSession(UUID userId, String username, String role, LocalDateTime expiresAt) {
            this.userId = userId;
            this.username = username;
            this.role = role;
            this.expiresAt = expiresAt;
        }

        public UUID getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
    }

    public static class LoginResult {
        private final String token;
        private final String username;
        private final String role;

        public LoginResult(String token, String username, String role) {
            this.token = token;
            this.username = username;
            this.role = role;
        }

        public String getToken() { return token; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }
}
