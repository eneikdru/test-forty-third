package com.eneik.generated;

import com.eneik.generated.service.TechnicalLeadCompiler;
import com.eneik.generated.service.TechnicalLeadCompiler.CoverageAudit;
import com.eneik.generated.service.TechnicalLeadCompiler.Specification;
import com.eneik.production.services.CodeReviewSimulator;
import com.eneik.production.services.PrMetadata;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CoverageAuditAndRefusalCriteriaTest {

    private final TechnicalLeadCompiler compiler = new TechnicalLeadCompiler();
    private final CodeReviewSimulator reviewSimulator = new CodeReviewSimulator();

    @Test
    public void testCoverageAuditFakingSuccessBlocked() {
        // Given an epidemiology center specification
        Specification spec = new Specification(Arrays.asList(
            "roles_epidemiology_center",
            "educational_content_epidemiology",
            "search_functionality_epidemiology",
            "fos_gias_epidemiology"
        ));

        // When checking an audit that fakes success with empty gaps (stub)
        CoverageAudit falsifiedAudit = new CoverageAudit(
            Collections.emptyList(), // reported empty gaps (falsified stub)
            Collections.emptyList()  // zero addressed sections
        );

        // Then it must identify missing epidemiology requirements and throw an exception blocking it
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            compiler.validateCoverageAudit(falsifiedAudit, spec);
        });

        assertTrue(ex.getMessage().contains("Falsified Coverage Audit Blocked"), "Should block falsified audit");
        assertTrue(ex.getMessage().contains("roles_epidemiology_center"), "Should identify missing roles section");
        assertTrue(ex.getMessage().contains("educational_content_epidemiology"), "Should identify missing educational content");
    }

    @Test
    public void testCoverageAuditSuccessWithCorrectGaps() {
        // Given an epidemiology center specification
        Specification spec = new Specification(Arrays.asList(
            "roles_epidemiology_center",
            "educational_content_epidemiology",
            "search_functionality_epidemiology",
            "fos_gias_epidemiology"
        ));

        // When the audit correctly identifies all missing sections as gaps
        List<String> correctGaps = Arrays.asList(
            "roles_epidemiology_center",
            "educational_content_epidemiology",
            "search_functionality_epidemiology",
            "fos_gias_epidemiology"
        );
        CoverageAudit honestAudit = new CoverageAudit(correctGaps, Collections.emptyList());

        // Then the validation should succeed without throwing an exception because the gaps are honestly identified
        assertDoesNotThrow(() -> {
            compiler.validateCoverageAudit(honestAudit, spec);
        });
    }

    @Test
    public void testRefusalCriteriaBlocksInternalToolingAsLeanWaste() {
        // Given a PR that exclusively introduces/patches internal tooling (GitHub webhooks, telemetry, etc.)
        PrMetadata pr = new PrMetadata();
        pr.setPrNumber(234);
        pr.setTitle("Patch task orchestration telemetry and webhook synchronization");
        pr.setDescription("This patches the internal GitHub sync webhook and adds status transition telemetry.");
        pr.setChangedFiles(Arrays.asList(
            "src/main/java/com/eneik/generated/controller/GitHubWebhookController.java",
            "src/main/java/com/eneik/generated/service/TaskSyncScheduler.java"
        ));

        // When evaluated
        PrMetadata reviewed = reviewSimulator.reviewPullRequest(pr);

        // Then it must be BLOCKED as Lean Waste / Overproduction (Muda)
        assertEquals("BLOCKED", reviewed.getVerdict(), "PR with internal tooling only must be blocked");
        assertNotNull(reviewed.getRejectionReason());
        assertTrue(reviewed.getRejectionReason().contains("Lean Waste"), "Rejection reason must mention Lean Waste");
        assertTrue(reviewed.getRejectionReason().contains("Muda"), "Rejection reason must mention Muda/Overproduction");
    }

    @Test
    public void testRefusalCriteriaApprovesClientProductPR() {
        // Given a PR that implements the epidemiology knowledge base
        PrMetadata pr = new PrMetadata();
        pr.setPrNumber(235);
        pr.setTitle("Implement epidemiology center knowledge base endpoints and search functionality");
        pr.setDescription("Adds FOS/GIAs content, search functionality, and role verification for epidemiology knowledge base.");
        pr.setChangedFiles(Arrays.asList(
            "src/main/java/com/eneik/generated/controller/DocumentSearchController.java",
            "src/main/java/com/eneik/generated/service/DocumentSearchService.java"
        ));

        // When evaluated
        PrMetadata reviewed = reviewSimulator.reviewPullRequest(pr);

        // Then it must be APPROVED (not blocked as Lean Waste)
        assertEquals("APPROVED", reviewed.getVerdict(), "PR delivering client product must be approved");
    }
}
