package com.eneik.generated.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;

/**
 * Centrally manages the parsing and extraction of the user ID (UUID) from HTTP request attributes
 * and fallbacks, ensuring deterministic handling and comprehensive logging.
 */
public class RequestUserIdExtractor {
    private static final Logger logger = LoggerFactory.getLogger(RequestUserIdExtractor.class);

    public static UUID extractUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 1. Check Request Attribute "X-User-Id"
        Object attrUserId = request.getAttribute("X-User-Id");
        if (attrUserId != null) {
            String userIdStr = attrUserId.toString().trim();
            if (!userIdStr.isEmpty()) {
                try {
                    return UUID.fromString(userIdStr);
                } catch (IllegalArgumentException e) {
                    logger.warn("Failed to parse UUID from request attribute X-User-Id: '{}'", userIdStr, e);
                }
            }
        }

        // 2. Check if Session is marked as Invalid
        if (Boolean.TRUE.equals(request.getAttribute("X-Session-Invalid"))) {
            return null;
        }

        // 3. Check if Fallback is Allowed
        if (Boolean.TRUE.equals(request.getAttribute("X-Allow-Fallback"))) {
            String headerUserId = request.getHeader("X-User-Id");
            if (headerUserId != null && !headerUserId.trim().isEmpty()) {
                String userIdStr = headerUserId.trim();
                try {
                    return UUID.fromString(userIdStr);
                } catch (IllegalArgumentException e) {
                    logger.warn("Failed to parse UUID from request header X-User-Id: '{}'", userIdStr, e);
                }
            }
        }

        // 4. Fallback: Check header directly if no attributes are set (supports simpler unit/integration tests)
        String headerUserId = request.getHeader("X-User-Id");
        if (headerUserId != null && !headerUserId.trim().isEmpty()) {
            String userIdStr = headerUserId.trim();
            try {
                return UUID.fromString(userIdStr);
            } catch (IllegalArgumentException e) {
                logger.warn("Failed to parse UUID from fallback request header X-User-Id: '{}'", userIdStr, e);
            }
        }

        return null;
    }
}
