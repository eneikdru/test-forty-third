package com.eneik.generated;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ComplianceGeneratorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testCoverageAuditUnauthenticatedReturns401() throws Exception {
        String requestBody = "{"
                + "  \"specification\": {\"sections\": [\"sec1\"]},"
                + "  \"audit\": {\"gaps\": [], \"addressedSections\": []}"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/coverage-audit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    public void testCoverageAuditUnauthorizedReturns403() throws Exception {
        String requestBody = "{"
                + "  \"specification\": {\"sections\": [\"sec1\"]},"
                + "  \"audit\": {\"gaps\": [], \"addressedSections\": []}"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/coverage-audit")
                        .header("X-User-Role", "Student")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    public void testCoverageAuditFalsificationBlocked() throws Exception {
        // Spec has sec1 and sec2, audit claims 0 gaps and 0 addressed sections (FALSIFIED!)
        String requestBody = "{"
                + "  \"specification\": {\"sections\": [\"sec1_document_storage\", \"sec2_fulltext_search\"]},"
                + "  \"audit\": {\"gaps\": [], \"addressedSections\": []}"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/coverage-audit")
                        .header("X-User-Role", "Administrator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Falsified Coverage Audit Blocked")));
    }

    @Test
    public void testCoverageAuditValidAllowed() throws Exception {
        String requestBody = "{"
                + "  \"specification\": {\"sections\": [\"sec1_document_storage\", \"sec2_fulltext_search\"]},"
                + "  \"audit\": {\"gaps\": [], \"addressedSections\": [\"sec1_document_storage\", \"sec2_fulltext_search\"]}"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/coverage-audit")
                        .header("X-User-Role", "Content Manager")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("VALIDATED")))
                .andExpect(jsonPath("$.audit.addressedSections", containsInAnyOrder("sec1_document_storage", "sec2_fulltext_search")));
    }

    @Test
    public void testTaskPlanUnauthenticatedReturns401() throws Exception {
        String requestBody = "{"
                + "  \"plan\": {"
                + "    \"title\": \"PR sync Fix\","
                + "    \"jtbd\": \"When patching the PR sync... \","
                + "    \"coverageComplete\": true,"
                + "    \"tocConstraintRef\": \"sync-queue-processing\","
                + "    \"requirementRefs\": [\"R1\"]"
                + "  },"
                + "  \"specification\": {\"sections\": [\"sec1\"]},"
                + "  \"implementedSections\": []"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/task-plan")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("UNAUTHORIZED")));
    }

    @Test
    public void testTaskPlanUnauthorizedReturns403() throws Exception {
        String requestBody = "{"
                + "  \"plan\": {"
                + "    \"title\": \"PR sync Fix\","
                + "    \"jtbd\": \"When patching the PR sync... \","
                + "    \"coverageComplete\": true,"
                + "    \"tocConstraintRef\": \"sync-queue-processing\","
                + "    \"requirementRefs\": [\"R1\"]"
                + "  },"
                + "  \"specification\": {\"sections\": [\"sec1\"]},"
                + "  \"implementedSections\": []"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/task-plan")
                        .header("X-User-Role", "Teacher")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("ACCESS_DENIED")));
    }

    @Test
    public void testTaskPlanFalsificationBlocked() throws Exception {
        // Claims coverage is complete, but sec1 is unimplemented (FALSIFIED!)
        String requestBody = "{"
                + "  \"plan\": {"
                + "    \"title\": \"Knowledge Base Plan\","
                + "    \"jtbd\": \"Implement knowledge base features.\","
                + "    \"coverageComplete\": true,"
                + "    \"tocConstraintRef\": \"sync-queue-processing\","
                + "    \"requirementRefs\": [\"R1\"]"
                + "  },"
                + "  \"specification\": {\"sections\": [\"sec1_document_storage\"]},"
                + "  \"implementedSections\": []"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/task-plan")
                        .header("X-User-Role", "Administrator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("Falsified Task Plan Blocked")));
    }

    @Test
    public void testTaskPlanInvalidTocRefBlocked() throws Exception {
        // Uses qualitative phrase "task-status-sync-reliability" as TOC constraint ref
        String requestBody = "{"
                + "  \"plan\": {"
                + "    \"title\": \"Knowledge Base Plan\","
                + "    \"jtbd\": \"Implement knowledge base features.\","
                + "    \"coverageComplete\": false,"
                + "    \"tocConstraintRef\": \"task-status-sync-reliability\","
                + "    \"requirementRefs\": [\"R1\"]"
                + "  },"
                + "  \"specification\": {\"sections\": [\"sec1_document_storage\"]},"
                + "  \"implementedSections\": []"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/task-plan")
                        .header("X-User-Role", "Administrator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("TOC Integrity Refusal")));
    }

    @Test
    public void testTaskPlanEchoPreventionBlocked() throws Exception {
        // Similar plan exists in existingNetwork (ECHO duplicates!)
        String requestBody = "{"
                + "  \"plan\": {"
                + "    \"title\": \"PR sync Fix\","
                + "    \"jtbd\": \"When patching the PR sync, I want to make sure the status transitions to failed.\","
                + "    \"coverageComplete\": false,"
                + "    \"tocConstraintRef\": \"sync-queue-processing\","
                + "    \"requirementRefs\": [\"R1\"]"
                + "  },"
                + "  \"specification\": {\"sections\": [\"sec1_document_storage\"]},"
                + "  \"implementedSections\": [],"
                + "  \"existingNetwork\": ["
                + "    {"
                + "      \"title\": \"PR sync Fix\","
                + "      \"jtbd\": \"When patching the PR sync, I want to make sure the status transitions to failed.\","
                + "      \"coverageComplete\": false,"
                + "      \"tocConstraintRef\": \"sync-queue-processing\","
                + "      \"requirementRefs\": [\"R1\"]"
                + "    }"
                + "  ]"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/task-plan")
                        .header("X-User-Role", "Administrator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.message", containsString("ECHO Coherence Violation")));
    }

    @Test
    public void testTaskPlanValidAllowed() throws Exception {
        String requestBody = "{"
                + "  \"plan\": {"
                + "    \"title\": \"PR sync Fix\","
                + "    \"jtbd\": \"When patching the PR sync, I want to make sure the status transitions to failed.\","
                + "    \"coverageComplete\": true,"
                + "    \"tocConstraintRef\": \"sync-queue-processing\","
                + "    \"requirementRefs\": [\"R1\"]"
                + "  },"
                + "  \"specification\": {\"sections\": [\"sec1_document_storage\"]},"
                + "  \"implementedSections\": [\"sec1_document_storage\"],"
                + "  \"existingNetwork\": ["
                + "    {"
                + "      \"title\": \"Completely different plan\","
                + "      \"jtbd\": \"A unique plan about comments notification.\","
                + "      \"coverageComplete\": false,"
                + "      \"tocConstraintRef\": \"sync-queue-processing\","
                + "      \"requirementRefs\": [\"R1\"]"
                + "    }"
                + "  ]"
                + "}";

        mockMvc.perform(post("/api/v1/compliance/task-plan")
                        .header("X-User-Role", "Administrator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("VALIDATED")))
                .andExpect(jsonPath("$.plan.title", is("PR sync Fix")));
    }
}
