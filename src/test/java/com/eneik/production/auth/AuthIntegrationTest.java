package com.eneik.production.auth;

import com.eneik.production.auth.model.User;
import com.eneik.production.auth.model.UserSession;
import com.eneik.production.auth.repository.UserRepository;
import com.eneik.production.auth.repository.UserSessionRepository;
import com.eneik.generated.repository.UserRoleRepository;
import com.eneik.generated.model.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
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
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

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
    public void testLoginWithCorporateCredentialsEmail() throws Exception {
        User user = new User(
                UUID.randomUUID(),
                "admin",
                passwordEncoder.encode("supersecret"),
                LocalDateTime.now()
        );
        userRepository.save(user);

        // Login with a corporate email format where the username is the prefix
        Map<String, String> creds = Map.of(
                "username", "admin@corp.university.ru",
                "password", "supersecret"
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));
    }

    @Test
    public void testSessionAuthenticationCorrectlyOverridesUnverifiedHeaders() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = new User(
                userId,
                "john_doe",
                passwordEncoder.encode("mypassword"),
                LocalDateTime.now()
        );
        userRepository.save(user);

        UserRole role = new UserRole(
                UUID.randomUUID(),
                userId,
                "Teacher"
        );
        userRoleRepository.save(role);

        // Log in to get a valid secure session token
        Map<String, String> creds = Map.of(
                "username", "john_doe",
                "password", "mypassword"
        );

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andReturn();

        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        assertNotNull(setCookieHeader);
        String sessionToken = null;
        for (String part : setCookieHeader.split(";")) {
            if (part.trim().startsWith("session_token=")) {
                sessionToken = part.trim().substring("session_token=".length());
                break;
            }
        }
        assertNotNull(sessionToken);

        // Perform search request with valid session_token but a conflicting untrusted role header 'X-User-Role: InvalidRole'.
        // The secure filter should ignore the header, resolve the role from DB ('Teacher'), and authorize the request!
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/documents/search")
                        .param("q", "test")
                        .cookie(new Cookie("session_token", sessionToken))
                        .header("X-User-Role", "InvalidRole"))
                .andExpect(status().isOk());

        // Perform request with an INVALID session token.
        // Unverified headers/roles should be blocked and ignored, resulting in UNAUTHORIZED / ACCESS_DENIED.
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/documents/search")
                        .param("q", "test")
                        .cookie(new Cookie("session_token", "invalid_token_123456789012345678901234567890"))
                        .header("X-User-Role", "Teacher"))
                .andExpect(status().isUnauthorized());
    }
}
