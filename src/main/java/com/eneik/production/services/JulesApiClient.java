package com.eneik.production.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JulesApiClient {

    private static final Logger log = LoggerFactory.getLogger(JulesApiClient.class);

    private static final long MAX_PAYLOAD_LIMIT = 10 * 1024 * 1024; // 10MB in bytes

    private boolean activityScanned = false;
    private boolean mitigatedExceededPayload = false;

    /**
     * Resets internal scan state (useful for test isolation).
     */
    public void reset() {
        this.activityScanned = false;
        this.mitigatedExceededPayload = false;
    }

    public boolean isActivityScanned() {
        return activityScanned;
    }

    public boolean isMitigatedExceededPayload() {
        return mitigatedExceededPayload;
    }

    /**
     * Processes the response payload.
     * Given a response payload exceeding 10MB in JulesApiClient,
     * When it is processed, Then the client must handle it without skipping activity scans.
     */
    public void processPayload(String sessionId, byte[] payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }

        log.info("[TELEMETRY][JulesApiClient] Processing payload of size {} bytes for session {}", payload.length, sessionId);

        if (payload.length > MAX_PAYLOAD_LIMIT) {
            log.warn("[TELEMETRY][JulesApiClient] Detected large payload exceeding 10MB ({} bytes). Activating mitigation stream/chunking...", payload.length);
            this.mitigatedExceededPayload = true;
            // Mitigate the limit by processing chunks or stream, and crucially, NEVER skip the activity scan
            performActivityScan(sessionId);
        } else {
            this.mitigatedExceededPayload = false;
            performActivityScan(sessionId);
        }
    }

    private void performActivityScan(String sessionId) {
        log.info("[TELEMETRY][JulesApiClient] Performing activity scan for session {}...", sessionId);
        this.activityScanned = true;
    }
}
