package com.eneik.generated.service;

import com.eneik.generated.service.TechnicalLeadCompiler.CoverageAudit;
import com.eneik.generated.service.TechnicalLeadCompiler.Specification;
import com.eneik.generated.service.TechnicalLeadCompiler.TaskPlan;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComplianceGeneratorService {

    private final TechnicalLeadCompiler technicalLeadCompiler;

    public ComplianceGeneratorService(TechnicalLeadCompiler technicalLeadCompiler) {
        this.technicalLeadCompiler = technicalLeadCompiler;
    }

    /**
     * Validates specification coverage audit.
     * Throws IllegalArgumentException if a falsified report is detected.
     */
    public void validateCoverageAudit(CoverageAudit audit, Specification specification) {
        technicalLeadCompiler.validateCoverageAudit(audit, specification);
    }

    /**
     * Validates task plan details, including coverage validation, TOC constraints, and duplicate ECHO detection.
     * Throws appropriate exceptions if validation fails.
     */
    public void validateTaskPlan(TaskPlan plan, Specification specification, List<String> implementedSections, List<TaskPlan> existingNetwork) {
        // 1. Validate task plan coverage
        technicalLeadCompiler.validateTaskPlanCoverage(plan, specification, implementedSections);

        // 2. Validate TOC constraint reference
        technicalLeadCompiler.validateTocConstraints(plan);

        // 3. Evaluate duplicate task plans to prevent ECHO duplicate generation
        if (existingNetwork != null && !existingNetwork.isEmpty()) {
            technicalLeadCompiler.evaluateEchoCoherence(existingNetwork, plan);
        }
    }

    public List<TaskPlan> detectAndMergeDuplicatePlans(List<TaskPlan> plans) {
        return technicalLeadCompiler.detectAndMergeDuplicatePlans(plans);
    }

    public double calculateJaccardSimilarity(String s1, String s2) {
        return technicalLeadCompiler.calculateJaccardSimilarity(s1, s2);
    }
}
