package com.eneik.generated;

import com.eneik.generated.service.TechnicalLeadCompiler;
import com.eneik.generated.service.TechnicalLeadCompiler.Specification;
import com.eneik.generated.service.TechnicalLeadCompiler.CoverageAudit;
import com.eneik.generated.service.TechnicalLeadCompiler.TaskPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class PlanningIntegrityAndEchoPreventionTest {

    private TechnicalLeadCompiler compiler;
    private Specification spec;

    @BeforeEach
    public void setUp() {
        compiler = new TechnicalLeadCompiler();
        // Client specification consists of 9 key sections (educational knowledge base)
        spec = new Specification(Arrays.asList(
            "sec1_document_storage",
            "sec2_fulltext_search",
            "sec3_rbac_policies",
            "sec4_regulatory_compliance",
            "sec5_version_control",
            "sec6_audit_logging",
            "sec7_notification_dispatcher",
            "sec8_feedback_loops",
            "sec9_external_integrations"
        ));
    }

    @Test
    public void testCoverageAuditFalsificationBlocked() {
        // Audit reports an empty gaps array but hasn't addressed any specification sections (Finding 2)
        CoverageAudit falsifiedAudit = new CoverageAudit(
            new ArrayList<>(), // gaps reported as empty
            new ArrayList<>()  // zero sections actually addressed
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            compiler.validateCoverageAudit(falsifiedAudit, spec);
        });

        assertTrue(ex.getMessage().contains("Falsified Coverage Audit Blocked"), "Expected falsification error, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("unaddressed"), "Expected gaps to be listed, got: " + ex.getMessage());
    }

    @Test
    public void testCoverageAuditSuccessAllowed() {
        // Audit correctly addresses all specification sections and gaps list is legitimately empty
        CoverageAudit validAudit = new CoverageAudit(
            new ArrayList<>(),
            Arrays.asList(
                "sec1_document_storage",
                "sec2_fulltext_search",
                "sec3_rbac_policies",
                "sec4_regulatory_compliance",
                "sec5_version_control",
                "sec6_audit_logging",
                "sec7_notification_dispatcher",
                "sec8_feedback_loops",
                "sec9_external_integrations"
            )
        );

        assertDoesNotThrow(() -> compiler.validateCoverageAudit(validAudit, spec));
    }

    @Test
    public void testTaskPlanCoverageFalsificationBlocked() {
        // Task plan fakes success by claiming coverageComplete=true while specification sections are unimplemented (Finding 3)
        TaskPlan falsifiedPlan = new TaskPlan(
            "PR sync Reliability Fix Plan",
            "When patching the PR sync, I want to make sure the status transitions to failed.",
            true, // claims coverage is complete
            "sync-queue-processing",
            Collections.singletonList("R1")
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            compiler.validateTaskPlanCoverage(falsifiedPlan, spec, Collections.singletonList("sec1_document_storage"));
        });

        assertTrue(ex.getMessage().contains("Falsified Task Plan Blocked"), "Expected falsification error, got: " + ex.getMessage());
    }

    @Test
    public void testTaskPlanCoverageSuccessAllowed() {
        // Plan has coverageComplete=true and all specification sections are implemented
        TaskPlan validPlan = new TaskPlan(
            "Complete Knowledge Base Plan",
            "Implement all 9 sections of the educational knowledge base specification.",
            true,
            "document-retrieval-throughput",
            Arrays.asList("R1", "R2", "R3")
        );

        List<String> implemented = Arrays.asList(
            "sec1_document_storage",
            "sec2_fulltext_search",
            "sec3_rbac_policies",
            "sec4_regulatory_compliance",
            "sec5_version_control",
            "sec6_audit_logging",
            "sec7_notification_dispatcher",
            "sec8_feedback_loops",
            "sec9_external_integrations"
        );

        assertDoesNotThrow(() -> compiler.validateTaskPlanCoverage(validPlan, spec, implemented));
    }

    @Test
    public void testTocConstraintDescriptivePhraseRefused() {
        // Violates TOC constraint by using qualitative descriptive phrases (Finding 4 & 5)
        TaskPlan planWithReliabilityRef = new TaskPlan(
            "Task plan patch",
            "Implement reliability updates.",
            false,
            "task-status-sync-reliability", // forbidden qualitative ref
            Collections.singletonList("R1")
        );

        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            compiler.validateTocConstraints(planWithReliabilityRef);
        });
        assertTrue(ex1.getMessage().contains("TOC Integrity Refusal"), "Expected TOC error, got: " + ex1.getMessage());

        TaskPlan planWithAccuracyRef = new TaskPlan(
            "Task plan patch 2",
            "Implement tracking accuracy.",
            false,
            "Task status tracking accuracy", // forbidden qualitative ref with spaces
            Collections.singletonList("R1")
        );

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            compiler.validateTocConstraints(planWithAccuracyRef);
        });
        assertTrue(ex2.getMessage().contains("TOC Integrity Refusal"), "Expected TOC error, got: " + ex2.getMessage());
    }

    @Test
    public void testTocConstraintValidBottleneckAllowed() {
        // Valid lowercase hyphenated system bottleneck keys
        TaskPlan validPlan1 = new TaskPlan(
            "Plan 1",
            "JTBD 1",
            false,
            "compiler-validation-loop",
            Collections.singletonList("R1")
        );
        assertDoesNotThrow(() -> compiler.validateTocConstraints(validPlan1));

        TaskPlan validPlan2 = new TaskPlan(
            "Plan 2",
            "JTBD 2",
            false,
            "document-retrieval-throughput",
            Collections.singletonList("R1")
        );
        assertDoesNotThrow(() -> compiler.validateTocConstraints(validPlan2));
    }

    @Test
    public void testEchoPreventionHaltsDuplicatePlan() {
        // Identical duplicate task planning cycles (as seen in PRs 196 and 197) are detected and halted (Finding 8)
        TaskPlan plan1 = new TaskPlan(
            "Task status tracking accuracy plan",
            "When testing the planning patch for this epic, I want to execute unit and integration tests against the compiler, so that falsification prevention and ECHO duplicate halting are verified.",
            false,
            "compiler-validation-loop",
            Collections.singletonList("R1")
        );

        TaskPlan plan2 = new TaskPlan(
            "Task status sync reliability plan",
            "When testing the planning patch for this epic, I want to execute unit and integration tests against the compiler, so that falsification prevention and ECHO duplicate halting are verified.",
            false,
            "compiler-validation-loop",
            Collections.singletonList("R1")
        );

        List<TaskPlan> existingNetwork = Collections.singletonList(plan1);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            compiler.evaluateEchoCoherence(existingNetwork, plan2);
        });

        assertTrue(ex.getMessage().contains("ECHO Coherence Violation"), "Expected ECHO error, got: " + ex.getMessage());
        assertTrue(ex.getMessage().contains("Duplicate task planning cycle detected"), "Expected duplicates mentioned, got: " + ex.getMessage());
    }

    @Test
    public void testEchoPreventionAllowsUniquePlan() {
        TaskPlan plan1 = new TaskPlan(
            "Knowledge Base search plan",
            "Implement synonym filters for full-text search.",
            false,
            "document-retrieval-throughput",
            Collections.singletonList("R1")
        );

        TaskPlan plan2 = new TaskPlan(
            "GitHub status webhook plan",
            "Implement webhook listener for closed unmerged PRs.",
            false,
            "sync-queue-processing",
            Collections.singletonList("R1")
        );

        List<TaskPlan> existingNetwork = Collections.singletonList(plan1);

        assertDoesNotThrow(() -> compiler.evaluateEchoCoherence(existingNetwork, plan2));
    }

    @Test
    public void testDetectAndMergeDuplicatePlansCombinedRequirementsAndCoverage() {
        // Given duplicate plans (by same title and highly similar JTBD)
        TaskPlan plan1 = new TaskPlan(
            "PR Sync Fix Plan",
            "When patching the PR sync, I want to make sure the status transitions to failed.",
            false, // not complete
            "sync-queue-processing",
            Arrays.asList("R1", "R2")
        );

        TaskPlan plan2 = new TaskPlan(
            "PR Sync Fix Plan",
            "When patching the PR sync, I want to make sure the status transitions to failed.",
            true, // coverage complete
            "sync-queue-processing",
            Arrays.asList("R2", "R3")
        );

        TaskPlan uniquePlan = new TaskPlan(
            "Completely Unique Plan",
            "This is a totally different task plan for some other epic issue.",
            false,
            "compiler-validation-loop",
            Collections.singletonList("R4")
        );

        List<TaskPlan> plans = Arrays.asList(plan1, plan2, uniquePlan);

        // When duplicate plan detection and merging is evaluated
        List<TaskPlan> mergedPlans = compiler.detectAndMergeDuplicatePlans(plans);

        // Then duplicates must be auto-detected and merged
        assertEquals(2, mergedPlans.size(), "Duplicate plans should be merged into one, plus the unique plan");

        // Verify merged duplicate plan properties
        TaskPlan mergedPrSyncPlan = mergedPlans.stream()
            .filter(p -> p.getTitle().equalsIgnoreCase("PR Sync Fix Plan"))
            .findFirst()
            .orElseThrow();

        assertTrue(mergedPrSyncPlan.isCoverageComplete(), "Merged plan coverageComplete must be true (combined with OR)");
        assertEquals(3, mergedPrSyncPlan.getRequirementRefs().size(), "Requirements must be merged uniquely");
        assertTrue(mergedPrSyncPlan.getRequirementRefs().containsAll(Arrays.asList("R1", "R2", "R3")));

        // Verify unique plan remains unaffected
        TaskPlan uniqueMerged = mergedPlans.stream()
            .filter(p -> p.getTitle().equalsIgnoreCase("Completely Unique Plan"))
            .findFirst()
            .orElseThrow();
        assertEquals(1, uniqueMerged.getRequirementRefs().size());
        assertEquals("R4", uniqueMerged.getRequirementRefs().get(0));
    }
}
