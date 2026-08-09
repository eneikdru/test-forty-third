package com.eneik.generated;

import com.eneik.generated.dto.CoverageAuditRequest;
import com.eneik.generated.dto.CoverageAuditResponse;
import com.eneik.generated.dto.TaskPlanRequest;
import com.eneik.generated.dto.TaskPlanResponse;
import com.eneik.generated.service.ComplianceGeneratorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("test")
public class PlanningIntegrityAndEchoPreventionTest {

    @Autowired
    private ComplianceGeneratorService complianceGeneratorService;

    @Test
    public void testCoverageAuditFailsWhenRealSpecificationCoverageIsMissing() {
        // Given specifications and some unaddressed real specification
        List<String> specifications = List.of("Document Comments and Update Requests", "Search Auto-Suggestions");
        List<String> addressed = List.of("Document Comments and Update Requests"); // missing "Search Auto-Suggestions"

        CoverageAuditRequest request = new CoverageAuditRequest(specifications, addressed);

        // When
        CoverageAuditResponse response = complianceGeneratorService.auditCoverage(request);

        // Then the audit itself should return valid = false because real coverage is missing for non-mock specs
        assertFalse(response.isValid(), "Audit must fail validation if real coverage is missing");
        assertFalse(response.isCoverageComplete());
        assertEquals(1, response.getGaps().size());
        assertTrue(response.getGaps().contains("Search Auto-Suggestions"));
    }

    @Test
    public void testCoverageAuditFailsWhenMockSpecificationsHaveGaps() {
        // Given
        List<String> specifications = List.of("REQ-001", "REQ-002");
        List<String> addressed = List.of("REQ-001");

        CoverageAuditRequest request = new CoverageAuditRequest(specifications, addressed);

        // When
        CoverageAuditResponse response = complianceGeneratorService.auditCoverage(request);

        // Then
        assertFalse(response.isValid(), "Audit with only mock spec gaps must fail to block falsified success reports");
        assertFalse(response.isCoverageComplete());
        assertEquals(1, response.getGaps().size());
        assertTrue(response.getGaps().contains("REQ-002"));
    }

    @Test
    public void testTaskPlanValidationFailsWhenSpecificationLacksRealCoverage() {
        // Given a task plan that includes an unimplemented specification
        TaskPlanRequest request = new TaskPlanRequest(
                List.of("Search Auto-Suggestions"),
                List.of("RC-001: Implement suggestions utilizing Elasticsearch with synonym filters"),
                List.of("TSK-001")
        );

        // When
        TaskPlanResponse response = complianceGeneratorService.validateTaskPlan(request);

        // Then
        assertFalse(response.isValidated(), "Task plan validation must fail if specification lacks real coverage");
        assertFalse(response.isCoverageComplete());
        assertTrue(response.getFailures().stream()
                .anyMatch(f -> f.contains("lacks real test coverage")), "Should contain a coverage failure message");
    }

    @Test
    public void testTaskPlanValidationFailsOnFakedOrPlaceholderRootCauseRepairs() {
        // Given a task plan with 'todo' or 'placeholder' in root-cause repairs
        TaskPlanRequest requestWithTodo = new TaskPlanRequest(
                List.of("REQ-001"),
                List.of("TODO: fix it later"),
                List.of("TSK-001")
        );

        // When
        TaskPlanResponse responseWithTodo = complianceGeneratorService.validateTaskPlan(requestWithTodo);

        // Then
        assertFalse(responseWithTodo.isValidated(), "Task plan validation must fail on 'TODO' root-cause repairs");
        assertTrue(responseWithTodo.getFailures().stream()
                .anyMatch(f -> f.contains("invalid or faked root-cause repair")), "Should detect faked repair description");
    }

    @Test
    public void testTaskPlanValidationFailsOnTooShortRootCauseRepairs() {
        // Given root cause repair description is too short (less than 5 characters)
        TaskPlanRequest requestWithShort = new TaskPlanRequest(
                List.of("REQ-001"),
                List.of("abc"),
                List.of("TSK-001")
        );

        // When
        TaskPlanResponse responseWithShort = complianceGeneratorService.validateTaskPlan(requestWithShort);

        // Then
        assertFalse(responseWithShort.isValidated());
        assertTrue(responseWithShort.getFailures().stream()
                .anyMatch(f -> f.contains("too short to be genuine")));
    }

    @Test
    public void testTaskPlanValidationSucceedsWithGenuineRequirements() {
        // Given fully compliant genuine specs, repairs and tasks
        TaskPlanRequest request = new TaskPlanRequest(
                List.of("Document Comments and Update Requests"),
                List.of("RC-001: Resolve status synchronization by updating internal DB to match GitHub API reality"),
                List.of("TSK-001")
        );

        // When
        TaskPlanResponse response = complianceGeneratorService.validateTaskPlan(request);

        // Then
        assertTrue(response.isValidated());
        assertTrue(response.isCoverageComplete());
        assertEquals(0, response.getFailures().size());
    }

    @Test
    public void testCoverageAuditFailsOnPRSubmissionWithoutTestFiles() {
        // Given a PR submission where there is no Test.java file in changedFiles
        List<String> specifications = List.of("Document Comments and Update Requests");
        List<String> addressed = List.of("Document Comments and Update Requests");
        List<String> changedFiles = List.of("src/main/java/com/eneik/generated/service/ComplianceGeneratorService.java");

        CoverageAuditRequest request = new CoverageAuditRequest(specifications, addressed, changedFiles);

        // When
        CoverageAuditResponse response = complianceGeneratorService.auditCoverage(request);

        // Then it must fail validation due to missing automated verification artifacts (Test.java) to prove epistemic certainty
        assertFalse(response.isValid(), "Audit must fail if there is a PR submission with no *Test.java files");
    }

    @Test
    public void testCoverageAuditSucceedsOnPRSubmissionWithTestFiles() {
        // Given a PR submission with a Test.java file in changedFiles and no gaps
        List<String> specifications = List.of("Document Comments and Update Requests");
        List<String> addressed = List.of("Document Comments and Update Requests");
        List<String> changedFiles = List.of(
                "src/main/java/com/eneik/generated/service/ComplianceGeneratorService.java",
                "src/test/java/com/eneik/generated/ComplianceGeneratorServiceTest.java"
        );

        CoverageAuditRequest request = new CoverageAuditRequest(specifications, addressed, changedFiles);

        // When
        CoverageAuditResponse response = complianceGeneratorService.auditCoverage(request);

        // Then the audit must succeed because both the requirement is tested/addressed and test files exist
        assertTrue(response.isValid(), "Audit must pass if requirement is addressed and a *Test.java file is provided");
    }
}