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
            pr.setRejectionReason("Explicitly contains security vulnerability");
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
                pr.setRejectionReason("Security scan detected missing RBAC or high severity vulnerability on ComplianceGeneratorController");
                return pr;
            }
        }

        // Evaluate Lean Waste Refusal Criteria
        boolean isInternalToolingPR = false;
        boolean providesClientProductValue = false;

        // Check changed files
        if (pr.getChangedFiles() != null && !pr.getChangedFiles().isEmpty()) {
            for (String file : pr.getChangedFiles()) {
                String lowerFile = file.toLowerCase();

                if (lowerFile.contains("github") ||
                    lowerFile.contains("webhook") ||
                    lowerFile.contains("telemetry") ||
                    lowerFile.contains("reconciliation") ||
                    lowerFile.contains("compiler") ||
                    lowerFile.contains("compliance") ||
                    lowerFile.contains("tooling") ||
                    lowerFile.contains("taskservice")) {
                    isInternalToolingPR = true;
                }

                if (lowerFile.contains("epidemiology") ||
                    lowerFile.contains("knowledgebase") ||
                    lowerFile.contains("document") ||
                    lowerFile.contains("search") ||
                    lowerFile.contains("category") ||
                    lowerFile.contains("comment") ||
                    lowerFile.contains("role") ||
                    lowerFile.contains("userrole") ||
                    lowerFile.contains("preference") ||
                    lowerFile.contains("feedback") ||
                    lowerFile.contains("financial") ||
                    lowerFile.contains("stipend") ||
                    lowerFile.contains("budget") ||
                    lowerFile.contains("academic")) {
                    providesClientProductValue = true;
                }
            }
        }

        // Check PR title and description
        String title = pr.getTitle() != null ? pr.getTitle().toLowerCase() : "";
        String desc = pr.getDescription() != null ? pr.getDescription().toLowerCase() : "";
        if (title.contains("webhook") || title.contains("telemetry") || title.contains("sync") || title.contains("compliance") || title.contains("internal tooling")) {
            isInternalToolingPR = true;
        }
        if (title.contains("epidemiology") || title.contains("knowledge") || title.contains("search") || title.contains("document") || title.contains("client spec")) {
            providesClientProductValue = true;
        }
        if (desc.contains("webhook") || desc.contains("telemetry") || desc.contains("sync") || desc.contains("compliance") || desc.contains("internal tooling")) {
            isInternalToolingPR = true;
        }
        if (desc.contains("epidemiology") || desc.contains("knowledge") || desc.contains("search") || desc.contains("document") || desc.contains("client spec")) {
            providesClientProductValue = true;
        }

        // If the PR focuses exclusively on internal tooling and fails to deliver the client product, block as Lean Waste (Muda)
        if (isInternalToolingPR && !providesClientProductValue) {
            pr.setVerdict("BLOCKED");
            pr.setRejectionReason("Lean Waste Refusal: PR exclusively patches internal tooling (webhook, sync, or telemetry) while ignoring the primary client specification for the epidemiology knowledge base. This constitutes Overproduction (Muda) and is blocked.");
            return pr;
        }

        // Otherwise approve if not already rejected
        if (pr.getVerdict() == null || pr.getVerdict().isEmpty()) {
            pr.setVerdict("APPROVED");
        }
        return pr;
    }
}
