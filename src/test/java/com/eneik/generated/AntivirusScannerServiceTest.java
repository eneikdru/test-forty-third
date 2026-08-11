package com.eneik.generated;

import com.eneik.generated.service.AntivirusScannerService;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class AntivirusScannerServiceTest {

    private final AntivirusScannerService service = new AntivirusScannerService();

    @Test
    public void testCleanStream() throws IOException {
        String cleanData = "This is a clean file content that contains absolutely no virus signatures.";
        try (InputStream stream = new ByteArrayInputStream(cleanData.getBytes(StandardCharsets.UTF_8))) {
            assertTrue(service.scan(stream), "Clean stream must be accepted");
        }
    }

    @Test
    public void testEicarSignatureRejected() throws IOException {
        String infectedData = "Some prefix text... X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H* Some suffix text...";
        try (InputStream stream = new ByteArrayInputStream(infectedData.getBytes(StandardCharsets.UTF_8))) {
            assertFalse(service.scan(stream), "Stream with standard EICAR signature must be rejected");
        }
    }

    @Test
    public void testSimplifiedEicarSignatureRejected() throws IOException {
        String infectedData = "Some prefix... EICAR-STANDARD-ANTIVIRUS-TEST-FILE ...suffix";
        try (InputStream stream = new ByteArrayInputStream(infectedData.getBytes(StandardCharsets.UTF_8))) {
            assertFalse(service.scan(stream), "Stream with simplified EICAR signature must be rejected");
        }
    }

    @Test
    public void testCustomMalwareSignatureRejected() throws IOException {
        String infectedData = "Important system update. Warning: MALWARE_INFECTED_FILE_SIGNATURE detected!";
        try (InputStream stream = new ByteArrayInputStream(infectedData.getBytes(StandardCharsets.UTF_8))) {
            assertFalse(service.scan(stream), "Stream with custom malware signature must be rejected");
        }
    }

    @Test
    public void testNullOrEmptyStream() throws IOException {
        assertTrue(service.scan(null), "Null stream is treated as clean or ignored");
        try (InputStream stream = new ByteArrayInputStream(new byte[0])) {
            assertTrue(service.scan(stream), "Empty stream is clean");
        }
    }

    @Test
    public void testLargeCleanStream() throws IOException {
        // Create a 1MB stream of zeroes
        byte[] largeData = new byte[1024 * 1024];
        try (InputStream stream = new ByteArrayInputStream(largeData)) {
            assertTrue(service.scan(stream), "Large clean stream must be accepted");
        }
    }
}
