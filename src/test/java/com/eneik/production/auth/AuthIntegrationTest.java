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
public class AuthIntegrationTest {

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
    public void testSuccessfulLoginIssuesSecureToken() throws Exception {
        User user = new User(
                UUID.randomUUID(),
                "admin",
                passwordEncoder.encode("supersecret"),
                LocalDateTime.now()
        );
        userRepository.save(user);

        Map<String, String> creds = Map.of(
                "username", "admin",
                "password", "supersecret"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        assertTrue(setCookieHeader.contains("session_token="));
        assertTrue(setCookieHeader.contains("HttpOnly"));
        assertTrue(setCookieHeader.contains("Secure"));

        List<UserSession> sessions = userSessionRepository.findAll();
        assertEquals(1, sessions.size());
        assertEquals(user.getId(), sessions.get(0).getUserId());
        assertEquals("ACTIVE", sessions.get(0).getStatus());
    }

    @Test
    public void testLoginWithInvalidPasswordReturns401() throws Exception {
        User user = new User(
                UUID.randomUUID(),
                "admin",
                passwordEncoder.encode("supersecret"),
                LocalDateTime.now()
        );
        userRepository.save(user);

        Map<String, String> creds = Map.of(
                "username", "admin",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));

        List<UserSession> sessions = userSessionRepository.findAll();
        assertEquals(0, sessions.size());
    }

    @Test
    public void testLoginWithUnknownUsernameReturns401() throws Exception {
        Map<String, String> creds = Map.of(
                "username", "unknownuser",
                "password", "password"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")));
    }

    @Test
    public void testLoginWithMissingCredentialsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    public void testLoginWithNoBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("BAD_REQUEST")));
    }

    @Test
    public void testSessionAuthenticationWithValidTokenInCookieAllowsAccess() throws Exception {
        // 1. Seed user and roles
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "teacher1",
                passwordEncoder.encode("secret"),
                LocalDateTime.now()
        );
        userRepository.save(user);

        com.eneik.generated.model.UserRole role = new com.eneik.generated.model.UserRole(
                UUID.randomUUID(),
                userId,
                "Teacher"
        );
        userRoleRepository.save(role);

        // 2. Perform Login to obtain cookie
        Map<String, String> creds = Map.of(
                "username", "teacher1",
                "password", "secret"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        String sessionToken = setCookieHeader.split(";")[0].split("=")[1];

        // 3. Make an authenticated request using Cookie
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("session_token", sessionToken);
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/documents/search")
                        .param("q", "anyquery")
                        .cookie(cookie))
                .andExpect(status().isOk());
    }

    @Test
    public void testSessionAuthenticationWithValidTokenInHeaderAllowsAccess() throws Exception {
        // 1. Seed user and roles
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "teacher2",
                passwordEncoder.encode("secret"),
                LocalDateTime.now()
        );
        userRepository.save(user);

        com.eneik.generated.model.UserRole role = new com.eneik.generated.model.UserRole(
                UUID.randomUUID(),
                userId,
                "Teacher"
        );
        userRoleRepository.save(role);

        // 2. Perform Login to obtain cookie
        Map<String, String> creds = Map.of(
                "username", "teacher2",
                "password", "secret"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        String sessionToken = setCookieHeader.split(";")[0].split("=")[1];

        // 3. Make an authenticated request using Bearer header
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/documents/search")
                        .param("q", "anyquery")
                        .header("Authorization", "Bearer " + sessionToken))
                .andExpect(status().isOk());
    }

    @Test
    public void testSessionAuthenticationWithInvalidTokenBlocksAccess() throws Exception {
        // Use an invalid session token to query search, and also send an X-User-Role header.
        // It must reject because the session token invalidation overrides any headers (unverified headers blocked).
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/documents/search")
                        .param("q", "anyquery")
                        .cookie(new jakarta.servlet.http.Cookie("session_token", "invalid_token"))
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    public void testFallbackHeadersRejectedWhenAllowFallbackIsFalse() throws Exception {
        // When X-Allow-Fallback request attribute is false, unverified X-User-Role header must be completely ignored/blocked
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/documents/search")
                        .param("q", "anyquery")
                        .requestAttr("X-Allow-Fallback", false)
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }
}
