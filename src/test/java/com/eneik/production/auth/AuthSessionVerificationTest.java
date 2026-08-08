package com.eneik.production.auth;

import com.eneik.production.auth.model.User;
import com.eneik.production.auth.model.UserSession;
import com.eneik.production.auth.repository.UserRepository;
import com.eneik.production.auth.repository.UserSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eneik.generated.Application;

@SpringBootTest(classes = Application.class, properties = {"spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthSessionVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.eneik.generated.repository.UserRoleRepository userRoleRepository;

    @BeforeEach
    public void setUp() {
        userSessionRepository.deleteAll();
        userRoleRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void testValidLoginCreatesSecureTokenAndSession() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "validUser",
                passwordEncoder.encode("correctPassword"),
                LocalDateTime.now()
        );
        userRepository.save(user);

        Map<String, String> payload = Map.of(
                "username", "validUser",
                "password", "correctPassword"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader, "Expected Set-Cookie header to be present for a valid login");
        assertTrue(setCookieHeader.contains("session_token="), "Expected session_token in cookie");
        assertTrue(setCookieHeader.contains("HttpOnly"), "Expected HttpOnly attribute on cookie");
        assertTrue(setCookieHeader.contains("Secure"), "Expected Secure attribute on cookie");

        List<UserSession> activeSessions = userSessionRepository.findAll();
        assertEquals(1, activeSessions.size(), "Exactly one session should be persisted");
        assertEquals(userId, activeSessions.get(0).getUserId());
        assertEquals("ACTIVE", activeSessions.get(0).getStatus());
    }

    @Test
    public void testInvalidPasswordDoesNotGenerateTokenOrSession() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "validUser",
                passwordEncoder.encode("correctPassword"),
                LocalDateTime.now()
        );
        userRepository.save(user);

        Map<String, String> payload = Map.of(
                "username", "validUser",
                "password", "incorrectPassword"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertNull(setCookieHeader, "Should not issue secure session token cookie on bad password");

        List<UserSession> sessions = userSessionRepository.findAll();
        assertEquals(0, sessions.size(), "No user session should be created");
    }

    @Test
    public void testNonExistentUserDoesNotGenerateTokenOrSession() throws Exception {
        Map<String, String> payload = Map.of(
                "username", "nonExistentUser",
                "password", "somePassword"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertNull(setCookieHeader, "Should not issue secure session token cookie on non-existent user");

        List<UserSession> sessions = userSessionRepository.findAll();
        assertEquals(0, sessions.size(), "No user session should be created");
    }

    @Test
    public void testUnverifiedHttpHeaderIsBlockedWhenFallbackIsDisabled() throws Exception {
        // Query search with no session cookie and no valid token, but supplying X-User-Role "Teacher"
        // Ensure X-Allow-Fallback is set to false in the request attribute.
        // This simulates a request from an unauthenticated client attempting to trust the X-User-Role header.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/documents/search")
                        .param("q", "anyquery")
                        .requestAttr("X-Allow-Fallback", false)
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
