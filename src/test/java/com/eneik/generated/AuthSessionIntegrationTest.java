package com.eneik.generated;

import com.eneik.generated.service.AuthSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the new secure session-based authentication flow (BARCAN-TAG-07).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthSessionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthSessionService authSessionService;

    @Test
    public void testSuccessfulLoginAndSessionAccess() throws Exception {
        // 1. Perform a valid login as an Economist
        String loginPayload = "{\"username\": \"economist\", \"password\": \"economist_pass\"}";

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", startsWith("sess-")))
                .andExpect(jsonPath("$.role", is("Economist")))
                .andExpect(jsonPath("$.userId", is("e0000000-0000-0000-0000-000000000001")))
                .andExpect(jsonPath("$.username", is("economist")))
                .andReturn().getResponse().getContentAsString();

        // Extract token from response
        String token = response.substring(response.indexOf("sess-"), response.indexOf("\"", response.indexOf("sess-")));

        // 2. Use the secure token to access the budget endpoint
        mockMvc.perform(get("/api/financial/budget")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testFailedLoginInvalidPassword() throws Exception {
        String badPayload = "{\"username\": \"economist\", \"password\": \"wrong_pass\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badPayload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Неверное имя пользователя или пароль")));
    }

    @Test
    public void testBlockedAccessWhenUnauthenticated() throws Exception {
        // Accessing the budget endpoint without credentials must fail with 401 Unauthorized
        mockMvc.perform(get("/api/financial/budget")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", is("Missing or invalid credentials")));
    }

    @Test
    public void testEnforcedRolePermissionsWithSessionToken() throws Exception {
        // 1. Login as a Postgraduate/Student
        String loginPayload = "{\"username\": \"postgraduate\", \"password\": \"postgraduate_pass\"}";

        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = response.substring(response.indexOf("sess-"), response.indexOf("\"", response.indexOf("sess-")));

        // 2. Postgraduate cannot access load templates (Forbidden - 403)
        mockMvc.perform(get("/api/financial/load")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("does not have access to 'Load'")));
    }

    @Test
    public void testLogoutInvalidatesSession() throws Exception {
        // 1. Login
        String loginPayload = "{\"username\": \"teacher\", \"password\": \"teacher_pass\"}";
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = response.substring(response.indexOf("sess-"), response.indexOf("\"", response.indexOf("sess-")));

        // Verify session works
        mockMvc.perform(get("/api/financial/load")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // 2. Logout
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Вы успешно вышли из системы")));

        // 3. Verify session no longer works
        mockMvc.perform(get("/api/financial/load")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
