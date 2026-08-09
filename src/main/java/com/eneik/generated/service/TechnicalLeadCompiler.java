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
        // Finding 2: If the entire specification is unaddressed but the audit claims there are 0 gaps, block it!
        Set<String> addressed = new HashSet<>(audit.getAddressedSections());
        List<String> actualGaps = new ArrayList<>();
        for (String section : specification.getSections()) {
            if (!addressed.contains(section)) {
                actualGaps.add(section);
            }
        }

        if (!actualGaps.isEmpty() && audit.getGaps().isEmpty()) {
            throw new IllegalArgumentException(
                "Falsified Coverage Audit Blocked: Empty 'gaps' array reported while the following specification sections are completely unaddressed: " + actualGaps
            );
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

    /**
     * Auto-detects and merges duplicate task plans in a list of plans.
     * Two plans are considered duplicates if they have the same title (case-insensitive)
     * or if their JTBD similarity is greater than 0.85.
     * When merged, requirement references are combined (unique list),
     * coverageComplete is true if any of the merged plans was true,
     * and the title/jtbd/tocConstraintRef from the first plan are retained.
     */
    public List<TaskPlan> detectAndMergeDuplicatePlans(List<TaskPlan> plans) {
        if (plans == null || plans.isEmpty()) {
            return new ArrayList<>();
        }

        List<TaskPlan> mergedPlans = new ArrayList<>();

        for (TaskPlan plan : plans) {
            boolean merged = false;
            for (int i = 0; i < mergedPlans.size(); i++) {
                TaskPlan existing = mergedPlans.get(i);
                double similarity = calculateJaccardSimilarity(existing.getJtbd(), plan.getJtbd());
                if (similarity > 0.85 || existing.getTitle().equalsIgnoreCase(plan.getTitle())) {
                    // Merge plan into existing
                    Set<String> combinedRequirements = new LinkedHashSet<>(existing.getRequirementRefs());
                    if (plan.getRequirementRefs() != null) {
                        combinedRequirements.addAll(plan.getRequirementRefs());
                    }
                    boolean combinedCoverage = existing.isCoverageComplete() || plan.isCoverageComplete();

                    TaskPlan mergedPlan = new TaskPlan(
                        existing.getTitle(),
                        existing.getJtbd(),
                        combinedCoverage,
                        existing.getTocConstraintRef() != null ? existing.getTocConstraintRef() : plan.getTocConstraintRef(),
                        new ArrayList<>(combinedRequirements)
                    );
                    mergedPlans.set(i, mergedPlan);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                mergedPlans.add(plan);
            }
        }
        return mergedPlans;
    }

    public double calculateJaccardSimilarity(String s1, String s2) {
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
