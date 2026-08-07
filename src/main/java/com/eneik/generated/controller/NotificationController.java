package com.eneik.generated.controller;

import com.eneik.generated.dto.MaxNotificationRequest;
import com.eneik.generated.dto.TelegramNotificationRequest;
import com.eneik.generated.service.NotificationDispatcher;
import com.eneik.generated.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationDispatcher notificationDispatcher;

    @Value("${notification.internal.service.key:INTERNAL_SERVICE_KEY}")
    private String internalServiceKey;

    public NotificationController(NotificationService notificationService, NotificationDispatcher notificationDispatcher) {
        this.notificationService = notificationService;
        this.notificationDispatcher = notificationDispatcher;
    }

    @PostMapping("/trigger/quarterly-review")
    public ResponseEntity<?> triggerQuarterlyReview(@RequestParam UUID documentId) {
        try {
            notificationService.triggerQuarterlyReview(documentId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "Quarterly review notification triggered."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "ERROR", "message", e.getMessage()));
        }
    }

    @PostMapping("/trigger/new-version")
    public ResponseEntity<?> triggerNewVersion(@RequestParam UUID documentId, @RequestParam Integer versionNumber) {
        try {
            notificationService.triggerNewVersionPublished(documentId, versionNumber);
            return ResponseEntity.ok(Map.of("status", "success", "message", "New version publication notification triggered."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "ERROR", "message", e.getMessage()));
        }
    }

    @PostMapping("/telegram/dispatch")
    public ResponseEntity<?> dispatchTelegram(HttpServletRequest request, @RequestBody TelegramNotificationRequest payload) {
        if (!isAuthorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Missing or invalid internal service bearer token"));
        }
        notificationDispatcher.dispatchTelegram(payload);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Telegram payload received."));
    }

    @PostMapping("/max/dispatch")
    public ResponseEntity<?> dispatchMax(HttpServletRequest request, @RequestBody MaxNotificationRequest payload) {
        if (!isAuthorized(request)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Missing or invalid internal service bearer token"));
        }
        notificationDispatcher.dispatchMax(payload);
        return ResponseEntity.ok(Map.of("status", "success", "message", "Max payload received."));
    }

    @GetMapping("/dispatched")
    public ResponseEntity<?> getDispatched() {
        return ResponseEntity.ok(Map.of(
                "telegram", notificationDispatcher.getDispatchedTelegram(),
                "max", notificationDispatcher.getDispatchedMax()
        ));
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearDispatched() {
        notificationDispatcher.clear();
        return ResponseEntity.ok(Map.of("status", "success", "message", "Dispatched logs cleared."));
    }

    private boolean isAuthorized(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            return internalServiceKey.equals(token);
        }
        return false;
    }
}
