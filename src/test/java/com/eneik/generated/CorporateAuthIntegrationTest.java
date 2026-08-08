package com.eneik.generated;

import com.eneik.generated.controller.AuthController;
import com.eneik.generated.model.User;
import com.eneik.generated.model.UserRole;
import com.eneik.generated.repository.UserRepository;
import com.eneik.generated.repository.UserRoleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CorporateAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @BeforeEach
    public void setUp() {
        // Clean database and seed specifically for isolated auth testing
        userRoleRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Economist
        UUID economistId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        userRepository.save(new User(economistId, "economist", "$2a$10$6iBtSQaIIfnBMa4/Qn.X5uPQuylTlEBBmaI0TA1MHt79AFwtluD1S"));
        userRoleRepository.save(new UserRole(UUID.randomUUID(), economistId, "Economist"));

        // 2. Teacher
        UUID teacherId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        userRepository.save(new User(teacherId, "teacher", "$2a$10$6iBtSQaIIfnBMa4/Qn.X5uPQuylTlEBBmaI0TA1MHt79AFwtluD1S"));
        userRoleRepository.save(new UserRole(UUID.randomUUID(), teacherId, "Teacher"));

        // 3. Postgraduate
        UUID postgraduateId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        userRepository.save(new User(postgraduateId, "postgraduate", "$2a$10$6iBtSQaIIfnBMa4/Qn.X5uPQuylTlEBBmaI0TA1MHt79AFwtluD1S"));
        userRoleRepository.save(new UserRole(UUID.randomUUID(), postgraduateId, "Postgraduate"));

        // 4. Admin
        UUID adminId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        userRepository.save(new User(adminId, "admin", "$2a$10$6iBtSQaIIfnBMa4/Qn.X5uPQuylTlEBBmaI0TA1MHt79AFwtluD1S"));
        userRoleRepository.save(new UserRole(UUID.randomUUID(), adminId, "Administrator"));
    }

    @Test
    public void testCorporateLoginSuccessful() throws Exception {
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("economist");
        request.setPassword("password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username", is("economist")))
                .andExpect(jsonPath("$.role", is("Economist")));
    }

    @Test
    public void testCorporateLoginFailure() throws Exception {
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("economist");
        request.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Неверное имя пользователя или пароль")));
    }

    @Test
    public void testSecureSessionTokenAuthorization() throws Exception {
        // 1. Login to obtain session token
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("economist");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(jsonResponse, Map.class);
        String token = (String) responseMap.get("token");

        // 2. Access protected financial budget endpoint using token
        mockMvc.perform(get("/api/financial/budget")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void testSecureSessionTokenRbacEnforcement() throws Exception {
        // 1. Login as teacher
        AuthController.LoginRequest request = new AuthController.LoginRequest();
        request.setUsername("teacher");
        request.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        String jsonResponse = result.getResponse().getContentAsString();
        Map<?, ?> responseMap = objectMapper.readValue(jsonResponse, Map.class);
        String token = (String) responseMap.get("token");

        // 2. Try to access restricted budget endpoint as teacher -> should be forbidden
        mockMvc.perform(get("/api/financial/budget")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", containsString("does not have access to 'Budget'")));
    }
}
