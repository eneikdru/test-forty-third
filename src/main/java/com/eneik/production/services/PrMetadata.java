package com.eneik.production.services;

import java.util.List;

public class PrMetadata {
    private int prNumber;
    private List<String> changedFiles;
    private boolean containsSecurityVulnerability;
    private String securityScanResults;
    private String verdict; // e.g., "APPROVED", "BLOCKED", "REJECTED"

    public PrMetadata() {}

    public PrMetadata(int prNumber, List<String> changedFiles, boolean containsSecurityVulnerability, String securityScanResults, String verdict) {
        this.prNumber = prNumber;
        this.changedFiles = changedFiles;
        this.containsSecurityVulnerability = containsSecurityVulnerability;
        this.securityScanResults = securityScanResults;
        this.verdict = verdict;
    }

    public int getPrNumber() {
        return prNumber;
    }

    public void setPrNumber(int prNumber) {
        this.prNumber = prNumber;
    }

    public List<String> getChangedFiles() {
        return changedFiles;
    }

    public void setChangedFiles(List<String> changedFiles) {
        this.changedFiles = changedFiles;
    }

    public boolean isContainsSecurityVulnerability() {
        return containsSecurityVulnerability;
    }

    public void setContainsSecurityVulnerability(boolean containsSecurityVulnerability) {
        this.containsSecurityVulnerability = containsSecurityVulnerability;
    }

    public String getSecurityScanResults() {
        return securityScanResults;
    }

    public void setSecurityScanResults(String securityScanResults) {
        this.securityScanResults = securityScanResults;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }
}
