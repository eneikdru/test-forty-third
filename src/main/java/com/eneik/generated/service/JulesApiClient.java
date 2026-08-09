package com.eneik.generated.service;

import org.springframework.stereotype.Component;

/**
 * JulesApiClient handles API payload processing with safety checks and limits.
 * By default, the payload limit is configured to be 50MB (52,428,800 bytes)
 * which comfortably exceeds the previous restrictive 10MB limit, preventing pipeline stalls.
 */
@Component
public class JulesApiClient {

    // Default payload limit of 50 Megabytes (exceeds the 10MB bottleneck)
    private static final long DEFAULT_MAX_PAYLOAD_LIMIT = 50 * 1024 * 1024L;

    private long maxPayloadLimit;

    public JulesApiClient() {
        this.maxPayloadLimit = DEFAULT_MAX_PAYLOAD_LIMIT;
    }

    public JulesApiClient(long maxPayloadLimit) {
        this.maxPayloadLimit = maxPayloadLimit;
    }

    public long getMaxPayloadLimit() {
        return maxPayloadLimit;
    }

    public void setMaxPayloadLimit(long maxPayloadLimit) {
        this.maxPayloadLimit = maxPayloadLimit;
    }

    /**
     * Processes a payload.
     *
     * @param payload the data to process
     * @return true if successfully processed
     * @throws IllegalArgumentException if payload is null
     * @throws IllegalStateException if the payload exceeds the configured maximum limit
     */
    public boolean processRequest(byte[] payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Payload cannot be null");
        }

        if (payload.length > maxPayloadLimit) {
            throw new IllegalStateException("Payload size " + payload.length
                + " bytes exceeds the maximum configured limit of " + maxPayloadLimit + " bytes.");
        }

        // Successfully processed payload without stalling
        return true;
    }

    /**
     * Processes a streaming payload.
     *
     * @param payloadStream the input stream of data to process
     * @return true if successfully processed
     * @throws IllegalArgumentException if payloadStream is null
     * @throws IllegalStateException if the payload exceeds the configured maximum limit
     */
    public boolean processRequest(java.io.InputStream payloadStream) {
        if (payloadStream == null) {
            throw new IllegalArgumentException("Payload stream cannot be null");
        }

        byte[] buffer = new byte[8192];
        long totalBytesRead = 0;
        try {
            int bytesRead;
            while ((bytesRead = payloadStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > maxPayloadLimit) {
                    throw new IllegalStateException("Payload size exceeds the maximum configured limit of "
                        + maxPayloadLimit + " bytes.");
                }
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Error reading payload stream", e);
        }

        return true;
    }
}
