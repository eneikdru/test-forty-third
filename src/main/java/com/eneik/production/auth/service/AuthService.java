package com.eneik.production.auth.service;

import com.eneik.production.auth.model.User;
import com.eneik.production.auth.model.UserSession;
import com.eneik.production.auth.repository.UserRepository;
import com.eneik.production.auth.repository.UserSessionRepository;
import com.eneik.generated.util.TimeProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, UserSessionRepository userSessionRepository, PasswordEncoder passwordEncoder, TimeProvider timeProvider) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.timeProvider = timeProvider;
    }

    public String authenticateAndGenerateToken(String username, String rawPassword) {
        if (username == null || rawPassword == null || username.trim().isEmpty() || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Username and password must not be empty");
        }

        String targetUsername = username.trim();
        Optional<User> userOpt = userRepository.findByUsername(targetUsername);

        // Support corporate credentials format (e.g. username@company.com or username@corp.edu)
        if (userOpt.isEmpty() && targetUsername.contains("@")) {
            String stripped = targetUsername.substring(0, targetUsername.indexOf("@"));
            if (!stripped.isEmpty()) {
                userOpt = userRepository.findByUsername(stripped);
            }
        }

        if (userOpt.isEmpty()) {
            throw new SecurityException("Invalid username or password");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new SecurityException("Invalid username or password");
        }

        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        UserSession session = new UserSession(
                UUID.randomUUID(),
                user.getId(),
                tokenHash,
                timeProvider.now().plusHours(24),
                timeProvider.now(),
                "ACTIVE"
        );
        userSessionRepository.save(session);

        return rawToken;
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

    private String generateSecureToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
}
