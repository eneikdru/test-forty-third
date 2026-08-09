package com.eneik.generated.service;

import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class TechnicalLeadCompiler {

    // Representation of a Client Specification
    public static class Specification {
        private final List<String> sections;

        public Specification(List<String> sections) {
            this.sections = sections;
        }

        public List<String> getSections() {
            return sections;
        }
    }

    // Representation of a Coverage Audit report
    public static class CoverageAudit {
        private final List<String> gaps;
        private final List<String> addressedSections;

        public CoverageAudit(List<String> gaps, List<String> addressedSections) {
            this.gaps = gaps != null ? gaps : new ArrayList<>();
            this.addressedSections = addressedSections != null ? addressedSections : new ArrayList<>();
        }

        public List<String> getGaps() {
            return gaps;
        }

        public List<String> getAddressedSections() {
            return addressedSections;
        }
    }

    // Representation of a Task Plan
    public static class TaskPlan {
        private final String title;
        private final String jtbd;
        private final boolean coverageComplete;
        private final String tocConstraintRef;
        private final List<String> requirementRefs;

        public TaskPlan(String title, String jtbd, boolean coverageComplete, String tocConstraintRef, List<String> requirementRefs) {
            this.title = title;
            this.jtbd = jtbd;
            this.coverageComplete = coverageComplete;
            this.tocConstraintRef = tocConstraintRef;
            this.requirementRefs = requirementRefs != null ? requirementRefs : new ArrayList<>();
        }

        public String getTitle() {
            return title;
        }

        public String getJtbd() {
            return jtbd;
        }

        public boolean isCoverageComplete() {
            return coverageComplete;
        }

        public String getTocConstraintRef() {
            return tocConstraintRef;
        }

        public List<String> getRequirementRefs() {
            return requirementRefs;
        }
    }

    /**
     * Validates a CoverageAudit against a Specification.
     * Prevents falsified coverage reports (where gaps is empty but specifications are actually unaddressed).
     */
    public void validateCoverageAudit(CoverageAudit audit, Specification specification) {
        Set<String> addressed = new HashSet<>(audit.getAddressedSections());
        List<String> actualGaps = new ArrayList<>();

        // Epidemiology Knowledge Base requirements list
        List<String> requiredEpidemiologySections = Arrays.asList(
            "roles_epidemiology_center",
            "educational_content_epidemiology",
            "search_functionality_epidemiology",
            "fos_gias_epidemiology"
        );

        // Check if the specification contains any epidemiology or knowledge base keywords
        boolean requiresEpidemiology = false;
        for (String section : specification.getSections()) {
            String lowerSection = section.toLowerCase();
            if (lowerSection.contains("epidemiology") || lowerSection.contains("kb") || lowerSection.contains("knowledge") || lowerSection.contains("gaps")) {
                requiresEpidemiology = true;
                break;
            }
        }

        // If the specification requires epidemiology KB compliance, identify any missing epidemiology sections
        if (requiresEpidemiology) {
            for (String req : requiredEpidemiologySections) {
                if (!addressed.contains(req)) {
                    actualGaps.add(req);
                }
            }
        }

        // Also evaluate any other sections defined in the specification
        for (String section : specification.getSections()) {
            if (!addressed.contains(section) && !actualGaps.contains(section)) {
                actualGaps.add(section);
            }
        }

        // If gaps are found, check if reported gaps list is empty or incomplete (falsified stub)
        if (!actualGaps.isEmpty()) {
            boolean isIncomplete = audit.getGaps().isEmpty() || !new HashSet<>(audit.getGaps()).containsAll(actualGaps);
            if (isIncomplete) {
                throw new IllegalArgumentException(
                    "Falsified Coverage Audit Blocked: Empty or incomplete 'gaps' array reported while the following specification sections are completely unaddressed: " + actualGaps
                );
            }
        }
    }

    /**
     * Validates a TaskPlan against a Specification.
     * Prevents task-plan.json from faking success by declaring coverageComplete=true without actual validation.
     */
    public void validateTaskPlanCoverage(TaskPlan plan, Specification specification, List<String> implementedSections) {
        // Finding 3: If the plan declares coverageComplete = true but we still have unaddressed/unimplemented specifications, reject it!
        Set<String> implemented = new HashSet<>(implementedSections);
        List<String> missing = new ArrayList<>();
        for (String section : specification.getSections()) {
            if (!implemented.contains(section)) {
                missing.add(section);
            }
        }

        if (plan.isCoverageComplete() && !missing.isEmpty()) {
            throw new IllegalArgumentException(
                "Falsified Task Plan Blocked: Declared 'coverageComplete' is true, but the original client specification is ignored or unaddressed: missing sections: " + missing
            );
        }
    }

    /**
     * Validates TOC constraints in a TaskPlan.
     * Enforces strict TOC constraints and rejects qualitative descriptive phrases as tocConstraintRef.
     */
    public void validateTocConstraints(TaskPlan plan) {
        String ref = plan.getTocConstraintRef();
        if (ref == null || ref.trim().isEmpty()) {
            throw new IllegalArgumentException("TOC constraint reference cannot be empty.");
        }

        // Finding 4 & 5: Reject qualitative descriptive phrases
        List<String> forbiddenRefs = Arrays.asList(
            "task-status-sync-reliability",
            "Task status tracking accuracy"
        );

        if (forbiddenRefs.contains(ref) || ref.contains(" ") || (!ref.contains("-") && ref.toLowerCase().equals(ref))) {
            throw new IllegalArgumentException(
                "TOC Integrity Refusal: Invalid tocConstraintRef '" + ref + "'. Must explicitly cite a true system bottleneck (e.g. system metric like lowercase-hyphenated throughput/time/loop/processing keys), not a qualitative descriptive phrase."
            );
        }
    }

    /**
     * Evaluates ECHO (Explanatory Coherence) duplicate plan scenarios.
     * Detects identical/highly similar duplicate task planning cycles and halts them.
     */
    public void evaluateEchoCoherence(List<TaskPlan> existingNetwork, TaskPlan newPlan) {
        if (newPlan == null) return;

        for (TaskPlan existing : existingNetwork) {
            // Compare Jtbd or Title for duplicate planning cycles
            double similarity = calculateJaccardSimilarity(existing.getJtbd(), newPlan.getJtbd());
            if (similarity > 0.85 || existing.getTitle().equalsIgnoreCase(newPlan.getTitle())) {
                throw new IllegalStateException(
                    "ECHO Coherence Violation: Duplicate task planning cycle detected (Similarity: " + similarity + "). Halting execution to prevent ECHO duplicate generation."
                );
            }
        }
    }

    private double calculateJaccardSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) return 0.0;
        Set<String> set1 = tokenize(s1);
        Set<String> set2 = tokenize(s2);
        if (set1.isEmpty() && set2.isEmpty()) return 1.0;
        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\s]", "").split("\\s+");
        return new HashSet<>(Arrays.asList(words));
    }
}
