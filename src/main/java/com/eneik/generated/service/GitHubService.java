package com.eneik.generated.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GitHubService {

    public static class PrStatus {
        private final String state;
        private final boolean merged;

        public PrStatus(String state, boolean merged) {
            this.state = state;
            this.merged = merged;
        }

        public String getState() {
            return state;
        }

        public boolean isMerged() {
            return merged;
        }
    }

    private final Map<Integer, PrStatus> prRegistry = new ConcurrentHashMap<>();

    /**
     * Seed or stub a PR status for testing or simulation.
     */
    public void registerPrStatus(int prNumber, String state, boolean merged) {
        prRegistry.put(prNumber, new PrStatus(state, merged));
    }

    /**
     * Clear all registered statuses (useful for test isolation).
     */
    public void clearRegistry() {
        prRegistry.clear();
    }

    /**
     * Fetches the PR status. If not registered, defaults to open and unmerged.
     */
    public PrStatus getPrStatus(int prNumber) {
        return prRegistry.getOrDefault(prNumber, new PrStatus("open", false));
    }
}
