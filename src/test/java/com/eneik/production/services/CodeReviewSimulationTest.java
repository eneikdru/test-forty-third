package com.eneik.production.services;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class CodeReviewSimulationTest {

    private final CodeReviewSimulator simulator = new CodeReviewSimulator();

    @Test
    public void testReviewExplicitSecurityVulnerabilityBlocked() {
        // Given a PR explicitly flagged with containing a security vulnerability
        PrMetadata pr = new PrMetadata();
        pr.setPrNumber(216);
        pr.setContainsSecurityVulnerability(true);
        pr.setChangedFiles(Collections.singletonList("src/main/java/com/eneik/generated/controller/ComplianceGeneratorController.java"));
        pr.setSecurityScanResults("High severity concern: Missing RBAC/authentication checks on ComplianceGeneratorController endpoints.");

        // When code review simulation runs
        PrMetadata reviewed = simulator.reviewPullRequest(pr);

        // Then it explicitly blocks the PR in the test
        assertEquals("BLOCKED", reviewed.getVerdict(), "PR with security vulnerabilities must be BLOCKED");
    }

    @Test
    public void testReviewMissingRbacOnComplianceControllerBlocked() {
        // Given a PR with a security vulnerability identified via scan results on ComplianceGeneratorController
        PrMetadata pr = new PrMetadata();
        pr.setPrNumber(216);
        pr.setContainsSecurityVulnerability(false);
        pr.setChangedFiles(Arrays.asList("src/main/java/com/eneik/generated/controller/ComplianceGeneratorController.java"));
        pr.setSecurityScanResults("Security scan detected: High severity concern - missing RBAC / authorization check");

        // When code review simulation runs
        PrMetadata reviewed = simulator.reviewPullRequest(pr);

        // Then it explicitly blocks the PR in the test
        assertEquals("BLOCKED", reviewed.getVerdict(), "PR modifying ComplianceGeneratorController with high severity scan findings must be BLOCKED");
    }

    @Test
    public void testReviewSafePrApproved() {
        // Given a clean, safe PR
        PrMetadata pr = new PrMetadata();
        pr.setPrNumber(217);
        pr.setContainsSecurityVulnerability(false);
        pr.setChangedFiles(Collections.singletonList("src/main/java/com/eneik/generated/util/IdProvider.java"));
        pr.setSecurityScanResults("No issues found");

        // When code review simulation runs
        PrMetadata reviewed = simulator.reviewPullRequest(pr);

        // Then the PR is approved
        assertEquals("APPROVED", reviewed.getVerdict(), "Safe PR without security vulnerabilities should be APPROVED");
    }
}
