package com.eneik.generated.controller;

import com.eneik.generated.service.AuthSessionService;
import com.eneik.generated.service.AuthSessionService.SessionInfo;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthSessionService authSessionService;

    public AuthController(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials, HttpServletResponse response) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "BAD_REQUEST", "message", "Имя пользователя и пароль обязательны"));
        }

        String token = authSessionService.login(username, password);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Неверное имя пользователя или пароль"));
        }

        // Set HttpOnly secure session cookie to protect against XSS token theft
        Cookie cookie = new Cookie("auth_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true in production with HTTPS
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60); // 1 day
        response.addCookie(cookie);

        SessionInfo info = authSessionService.getSession(token);
        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", info.getRole(),
                "userId", info.getUserId().toString(),
                "username", info.getUsername()
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Сессия не найдена"));
        }

        SessionInfo info = authSessionService.getSession(token);
        if (info == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "UNAUTHORIZED", "message", "Невалидная или просроченная сессия"));
        }

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", info.getRole(),
                "userId", info.getUserId().toString(),
                "username", info.getUsername()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractToken(request);
        if (token != null) {
            authSessionService.logout(token);
        }

        // Clear session cookie
        Cookie cookie = new Cookie("auth_token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Immediately delete
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("status", "success", "message", "Вы успешно вышли из системы"));
    }

    private String extractToken(HttpServletRequest request) {
        // 1. Check Authorization bearer header
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 2. Check HttpOnly cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("auth_token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }

        return null;
    }
}
