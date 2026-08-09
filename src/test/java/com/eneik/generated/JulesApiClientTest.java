package com.eneik.generated;

import com.eneik.generated.service.JulesApiClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Focused tests for JulesApiClient to verify correct handling of large payloads and custom limit configurations.
 */
public class JulesApiClientTest {

    @Test
    public void testDefaultPayloadLimitExceedsTenMegabytes() {
        JulesApiClient client = new JulesApiClient();
        // 50MB exceeds 10MB
        assertTrue(client.getMaxPayloadLimit() > 10 * 1024 * 1024L,
            "Default payload limit should be configured to exceed 10MB");
    }

    @Test
    public void testPayloadExceedingTenMegabytesCompletesSuccessfully() {
        JulesApiClient client = new JulesApiClient();

        // 11MB payload
        int size11MB = 11 * 1024 * 1024;
        byte[] largePayload = new byte[size11MB];

        boolean result = client.processRequest(largePayload);
        assertTrue(result, "Payload of size exceeding 10MB (11MB) should be processed successfully without stalling");
    }

    @Test
    public void testCustomPayloadLimitSettingAndEnforcement() {
        // Create client with custom limit of 5MB
        long customLimit = 5 * 1024 * 1024L;
        JulesApiClient client = new JulesApiClient(customLimit);

        assertEquals(customLimit, client.getMaxPayloadLimit());

        // 4MB payload should succeed
        byte[] payload4MB = new byte[4 * 1024 * 1024];
        assertTrue(client.processRequest(payload4MB));

        // 6MB payload should fail (exceeds 5MB)
        byte[] payload6MB = new byte[6 * 1024 * 1024];
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            client.processRequest(payload6MB);
        });
        assertTrue(ex.getMessage().contains("exceeds the maximum configured limit"));
    }

    @Test
    public void testNullPayloadThrowsException() {
        JulesApiClient client = new JulesApiClient();
        assertThrows(IllegalArgumentException.class, () -> {
            client.processRequest((byte[]) null);
        });
    }

    @Test
    public void testDynamicSetterForPayloadLimit() {
        JulesApiClient client = new JulesApiClient();

        // Dynamically change limit to 15MB
        long limit15MB = 15 * 1024 * 1024L;
        client.setMaxPayloadLimit(limit15MB);
        assertEquals(limit15MB, client.getMaxPayloadLimit());

        // 12MB payload should succeed
        byte[] payload12MB = new byte[12 * 1024 * 1024];
        assertTrue(client.processRequest(payload12MB));

        // 16MB payload should fail
        byte[] payload16MB = new byte[16 * 1024 * 1024];
        assertThrows(IllegalStateException.class, () -> {
            client.processRequest(payload16MB);
        });
    }

    @Test
    public void testStreamingPayloadExceedingTenMegabytesSucceeds() {
        JulesApiClient client = new JulesApiClient();

        // Simulate 12MB of streaming payload using a DummyInputStream to prevent test OOM
        java.io.InputStream stream12MB = new DummyInputStream(12 * 1024 * 1024L);
        boolean result = client.processRequest(stream12MB);
        assertTrue(result, "Streaming payload of size exceeding 10MB (12MB) should be processed successfully without stalling");
    }

    @Test
    public void testStreamingPayloadExceedingConfiguredLimitFails() {
        // Set limit of 5MB
        long customLimit = 5 * 1024 * 1024L;
        JulesApiClient client = new JulesApiClient(customLimit);

        // Stream exactly 5MB + 1 byte
        java.io.InputStream streamExceeded = new DummyInputStream(customLimit + 1);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            client.processRequest(streamExceeded);
        });
        assertTrue(ex.getMessage().contains("exceeds the maximum configured limit"));
    }

    @Test
    public void testNullStreamingPayloadThrowsException() {
        JulesApiClient client = new JulesApiClient();
        assertThrows(IllegalArgumentException.class, () -> {
            client.processRequest((java.io.InputStream) null);
        });
    }

    static class DummyInputStream extends java.io.InputStream {
        private final long limit;
        private long bytesRead = 0;

        public DummyInputStream(long limit) {
            this.limit = limit;
        }

        @Override
        public int read() {
            if (bytesRead >= limit) {
                return -1;
            }
            bytesRead++;
            return 0;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            if (bytesRead >= limit) {
                return -1;
            }
            long remaining = limit - bytesRead;
            int toRead = (int) Math.min(len, remaining);
            bytesRead += toRead;
            return toRead;
        }
    }
}
