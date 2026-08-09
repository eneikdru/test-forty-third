package com.eneik.generated.dto;

import java.util.List;

public class TaskPlanResponse {
    private boolean coverageComplete;
    private boolean validated;
    private List<String> failures;

    public TaskPlanResponse() {}

    public TaskPlanResponse(boolean coverageComplete, boolean validated, List<String> failures) {
        this.coverageComplete = coverageComplete;
        this.validated = validated;
        this.failures = failures;
    }

    public boolean isCoverageComplete() {
        return coverageComplete;
    }

    public void setCoverageComplete(boolean coverageComplete) {
        this.coverageComplete = coverageComplete;
    }

    public boolean isValidated() {
        return validated;
    }

    public void setValidated(boolean validated) {
        this.validated = validated;
    }

    public List<String> getFailures() {
        return failures;
    }

    public void setFailures(List<String> failures) {
        this.failures = failures;
    }
}
