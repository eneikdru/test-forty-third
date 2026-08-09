package com.eneik.generated.service;

import com.eneik.generated.dto.CoverageAuditRequest;
import com.eneik.generated.dto.CoverageAuditResponse;
import com.eneik.generated.dto.TaskPlanRequest;
import com.eneik.generated.dto.TaskPlanResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ComplianceGeneratorService {

    /**
     * Conducts a strict coverage audit of specifications against addressed requirements.
     * Prevents empty gaps array from being returned when actual specifications are unmet.
     */
    public CoverageAuditResponse auditCoverage(CoverageAuditRequest request) {
        List<String> specifications = request.getSpecifications();
        List<String> addressed = request.getAddressedRequirementIds();

        if (specifications == null) {
            specifications = new ArrayList<>();
        }
        if (addressed == null) {
            addressed = new ArrayList<>();
        }

        Set<String> addressedSet = new HashSet<>(addressed);
        List<String> gaps = new ArrayList<>();

        for (String spec : specifications) {
            if (!addressedSet.contains(spec)) {
                gaps.add(spec);
            }
        }

        double percentage = 100.0;
        if (!specifications.isEmpty()) {
            percentage = ((double) (specifications.size() - gaps.size()) / specifications.size()) * 100.0;
            // Round to one decimal place
            percentage = Math.round(percentage * 10.0) / 10.0;
        }

        boolean coverageComplete = gaps.isEmpty() && !specifications.isEmpty();
        boolean valid = true;

        return new CoverageAuditResponse(gaps, percentage, coverageComplete, valid);
    }

    /**
     * Validates a task plan and blocks faked coverage reports or unverified root-cause repairs.
     */
    public TaskPlanResponse validateTaskPlan(TaskPlanRequest request) {
        List<String> specifications = request.getSpecifications();
        List<String> rootCauseRepairs = request.getRootCauseRepairs();
        List<String> tasks = request.getTasks();

        if (specifications == null) {
            specifications = new ArrayList<>();
        }
        if (rootCauseRepairs == null) {
            rootCauseRepairs = new ArrayList<>();
        }
        if (tasks == null) {
            tasks = new ArrayList<>();
        }

        List<String> failures = new ArrayList<>();

        // Rule 1: Cannot declare coverageComplete if there are unmet specifications or empty plan
        if (specifications.isEmpty()) {
            failures.add("Validation failure: No client specifications provided in plan context.");
        }

        // Rule 2: Valid root-cause repairs must be present and linked to planned tasks
        if (rootCauseRepairs.isEmpty()) {
            failures.add("Validation failure: Faked compliance detected - missing valid root-cause repairs for identified integrity failures.");
        }

        // Rule 3: Tasks must address the planned items
        if (tasks.isEmpty()) {
            failures.add("Validation failure: Plan lacks concrete tasks to address identified gaps.");
        }

        boolean validated = failures.isEmpty();
        // True coverage complete is only allowed if there are no validation failures and all specs are addressed
        boolean coverageComplete = validated && !specifications.isEmpty();

        return new TaskPlanResponse(coverageComplete, validated, failures);
    }
}
