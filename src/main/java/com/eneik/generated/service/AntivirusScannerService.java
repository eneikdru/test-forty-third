package com.eneik.generated.service;

import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.io.IOException;

@Service
public class AntivirusScannerService {

    private static final byte[] EICAR_SIGNATURE = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*".getBytes();
    private static final byte[] SIMPLIFIED_EICAR = "EICAR-STANDARD-ANTIVIRUS-TEST-FILE".getBytes();
    private static final byte[] CUSTOM_MALWARE_SIGNATURE = "MALWARE_INFECTED_FILE_SIGNATURE".getBytes();

    private static final byte[][] SIGNATURES = {
            EICAR_SIGNATURE,
            SIMPLIFIED_EICAR,
            CUSTOM_MALWARE_SIGNATURE
    };

    /**
     * Scans the given InputStream for known virus/malware signatures.
     * Uses a memory-efficient streamed sliding match to avoid loading the entire file into memory (OOM mitigation).
     *
     * @param inputStream the stream to scan
     * @return true if the stream is clean; false if malware was detected
     * @throws IOException if an I/O error occurs
     */
    public boolean scan(InputStream inputStream) throws IOException {
        if (inputStream == null) {
            return true;
        }

        int[] matchIndices = new int[SIGNATURES.length];
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            for (int i = 0; i < bytesRead; i++) {
                byte b = buffer[i];
                for (int s = 0; s < SIGNATURES.length; s++) {
                    byte[] sig = SIGNATURES[s];
                    if (b == sig[matchIndices[s]]) {
                        matchIndices[s]++;
                        if (matchIndices[s] == sig.length) {
                            return false; // Infected!
                        }
                    } else {
                        // KMP-like reset or simple fallback: check if current byte starts the signature
                        if (b == sig[0]) {
                            matchIndices[s] = 1;
                        } else {
                            matchIndices[s] = 0;
                        }
                    }
                }
            }
        }

        return true; // Clean!
    }
}
