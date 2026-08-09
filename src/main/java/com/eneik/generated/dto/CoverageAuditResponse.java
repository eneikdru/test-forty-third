package com.eneik.generated.dto;

import java.util.List;

public class CoverageAuditResponse {
    private List<String> gaps;
    private double coveragePercentage;
    private boolean coverageComplete;
    private boolean valid;

    public CoverageAuditResponse() {}

    public CoverageAuditResponse(List<String> gaps, double coveragePercentage, boolean coverageComplete, boolean valid) {
        this.gaps = gaps;
        this.coveragePercentage = coveragePercentage;
        this.coverageComplete = coverageComplete;
        this.valid = valid;
    }

    public List<String> getGaps() {
        return gaps;
    }

    public void setGaps(List<String> gaps) {
        this.gaps = gaps;
    }

    public double getCoveragePercentage() {
        return coveragePercentage;
    }

    public void setCoveragePercentage(double coveragePercentage) {
        this.coveragePercentage = coveragePercentage;
    }

    public boolean isCoverageComplete() {
        return coverageComplete;
    }

    public void setCoverageComplete(boolean coverageComplete) {
        this.coverageComplete = coverageComplete;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}
