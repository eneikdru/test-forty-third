package com.eneik.production.services;

import org.springframework.stereotype.Service;

@Service
public class CodeReviewSimulator {

    /**
     * Runs a code review simulation on a PR.
     * If the PR contains security vulnerabilities, it explicitly blocks the PR.
     */
    public PrMetadata reviewPullRequest(PrMetadata pr) {
        if (pr == null) {
            throw new IllegalArgumentException("PR metadata cannot be null");
        }

        // If explicitly flagged with security vulnerability
        if (pr.isContainsSecurityVulnerability()) {
            pr.setVerdict("BLOCKED");
            return pr;
        }

        // If files include security-sensitive controllers and scan results indicate vulnerability
        if (pr.getChangedFiles() != null) {
            boolean hasSensitiveController = pr.getChangedFiles().stream()
                    .anyMatch(file -> file.contains("ComplianceGeneratorController"));

            String scanResults = pr.getSecurityScanResults() != null ? pr.getSecurityScanResults().toLowerCase() : "";
            boolean hasHighSeverityVulnerability = scanResults.contains("high") || scanResults.contains("vulnerability") || scanResults.contains("missing rbac");

            if (hasSensitiveController && hasHighSeverityVulnerability) {
                pr.setVerdict("BLOCKED");
                return pr;
            }
        }

        // Otherwise approve if not already rejected
        if (pr.getVerdict() == null || pr.getVerdict().isEmpty()) {
            pr.setVerdict("APPROVED");
        }
        return pr;
    }
}
