package com.eneik.generated.service;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * JulesApiClient handles API payload processing with safety checks, limits,
 * and reliable downstream transmission.
 * By default, the payload limit is configured to be 50MB (52,428,800 bytes)
 * which comfortably exceeds the previous restrictive 10MB limit, preventing pipeline stalls.
 */
@Component
public class JulesApiClient {

    // Default payload limit of 50 Megabytes (exceeds the 10MB bottleneck)
    private static final long DEFAULT_MAX_PAYLOAD_LIMIT = 50 * 1024 * 1024L;

    private long maxPayloadLimit;

    @Value("${jules.api.client.endpoint-url:http://localhost:8080/api/v1/integrations/downstream}")
    private String endpointUrl;

    private final HttpClient httpClient;

    public JulesApiClient() {
        this.maxPayloadLimit = DEFAULT_MAX_PAYLOAD_LIMIT;
        this.httpClient = HttpClient.newHttpClient();
    }

    public JulesApiClient(long maxPayloadLimit) {
        this.maxPayloadLimit = maxPayloadLimit;
        this.httpClient = HttpClient.newHttpClient();
    }

    public JulesApiClient(long maxPayloadLimit, HttpClient httpClient) {
        this.maxPayloadLimit = maxPayloadLimit;
        this.httpClient = httpClient;
    }

    public long getMaxPayloadLimit() {
        return maxPayloadLimit;
    }

    public void setMaxPayloadLimit(long maxPayloadLimit) {
        this.maxPayloadLimit = maxPayloadLimit;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Processes a payload and transmits it to the downstream endpoint.
     *
     * @param payload the data to process
     * @return true if successfully processed and transmitted
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

        transmit(payload);
        return true;
    }

    /**
     * Processes a streaming payload and transmits it to the downstream endpoint.
     *
     * @param payloadStream the input stream of data to process
     * @return true if successfully processed and transmitted
     * @throws IllegalArgumentException if payloadStream is null
     * @throws IllegalStateException if the payload exceeds the configured maximum limit
     */
    public boolean processRequest(InputStream payloadStream) {
        if (payloadStream == null) {
            throw new IllegalArgumentException("Payload stream cannot be null");
        }

        byte[] buffer = new byte[8192];
        long totalBytesRead = 0;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            int bytesRead;
            while ((bytesRead = payloadStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                if (totalBytesRead > maxPayloadLimit) {
                    throw new IllegalStateException("Payload size exceeds the maximum configured limit of "
                        + maxPayloadLimit + " bytes.");
                }
                baos.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading payload stream", e);
        }

        byte[] payloadBytes = baos.toByteArray();
        transmit(payloadBytes);
        return true;
    }

    private void transmit(byte[] payload) {
        if (endpointUrl == null || endpointUrl.isEmpty()) {
            return;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpointUrl))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray(payload))
                .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new RuntimeException("Failed to transmit payload to downstream endpoint. HTTP Status: " + statusCode);
            }
        } catch (IOException e) {
            throw new RuntimeException("Network error transmitting payload to downstream endpoint: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payload transmission was interrupted: " + e.getMessage(), e);
        }
    }
}
