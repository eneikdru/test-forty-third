package com.eneik.generated;

import com.eneik.generated.dto.CoverageAuditRequest;
import com.eneik.generated.dto.TaskPlanRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class ComplianceGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCoverageAuditCalculatesCorrectGaps() throws Exception {
        // Given specifications and some covered/addressed requirements
        List<String> specifications = List.of("REQ-001", "REQ-002", "REQ-003", "REQ-004");
        List<String> addressed = List.of("REQ-001", "REQ-003");

        CoverageAuditRequest request = new CoverageAuditRequest(specifications, addressed);

        // When requesting a compliance coverage audit
        mockMvc.perform(post("/api/v1/compliance/coverage-audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then it correctly outputs unmet specifications as gaps (and doesn't fake an empty gaps array)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gaps", hasSize(2)))
                .andExpect(jsonPath("$.gaps", containsInAnyOrder("REQ-002", "REQ-004")))
                .andExpect(jsonPath("$.coveragePercentage", is(50.0)))
                .andExpect(jsonPath("$.coverageComplete", is(false)))
                .andExpect(jsonPath("$.valid", is(true)));
    }

    @Test
    public void testCoverageAuditCompleteWhenNoGaps() throws Exception {
        // Given all specifications are addressed
        List<String> specifications = List.of("REQ-001", "REQ-002");
        List<String> addressed = List.of("REQ-001", "REQ-002");

        CoverageAuditRequest request = new CoverageAuditRequest(specifications, addressed);

        mockMvc.perform(post("/api/v1/compliance/coverage-audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gaps", hasSize(0)))
                .andExpect(jsonPath("$.coveragePercentage", is(100.0)))
                .andExpect(jsonPath("$.coverageComplete", is(true)));
    }

    @Test
    public void testTaskPlanValidationFailsOnFalsifiedRequest() throws Exception {
        // Given a task plan request that attempts to claim compliance but has empty client specs or no root-cause repairs
        TaskPlanRequest request = new TaskPlanRequest(
                List.of(), // Empty specifications
                List.of(), // No root-cause repairs
                List.of("TSK-001")
        );

        // When validating the task plan
        mockMvc.perform(post("/api/v1/compliance/task-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then the system blocks falsified record generation by enforcing true checks (returns 400 Bad Request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.coverageComplete", is(false)))
                .andExpect(jsonPath("$.validated", is(false)))
                .andExpect(jsonPath("$.failures", hasSize(2)))
                .andExpect(jsonPath("$.failures", containsInAnyOrder(
                        "Validation failure: No client specifications provided in plan context.",
                        "Validation failure: Faked compliance detected - missing valid root-cause repairs for identified integrity failures."
                )));
    }

    @Test
    public void testTaskPlanValidationSucceedsOnFullyCompliantRequest() throws Exception {
        // Given a fully compliant task plan request with specifications, root-cause repairs, and planned tasks
        TaskPlanRequest request = new TaskPlanRequest(
                List.of("REQ-001", "REQ-002"),
                List.of("RC-001"),
                List.of("TSK-001")
        );

        mockMvc.perform(post("/api/v1/compliance/task-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageComplete", is(true)))
                .andExpect(jsonPath("$.validated", is(true)))
                .andExpect(jsonPath("$.failures", hasSize(0)));
    }
}
