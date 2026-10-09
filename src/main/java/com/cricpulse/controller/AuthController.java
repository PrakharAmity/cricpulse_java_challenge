package com.cricpulse.controller;

import com.cricpulse.service.PollLimiterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api")
public class AuthController {

    public record UserSession(String userId, String role, String name) {}

    private static final Map<String, UserSession> SESSIONS = new ConcurrentHashMap<>();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final PollLimiterService pollLimiterService;

    public AuthController(PollLimiterService pollLimiterService) {
        this.pollLimiterService = pollLimiterService;
    }

    public static UserSession getSession(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        return SESSIONS.get(token);
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(HttpServletRequest request) {
        String user = request.getParameter("user");
        String password = request.getParameter("password");

        String contentType = request.getContentType();
        if ((user == null || password == null) && contentType != null && contentType.contains("application/json")) {
            try (InputStream in = request.getInputStream()) {
                Map<?, ?> body = MAPPER.readValue(in, Map.class);
                if (body != null) {
                    if (body.get("user") != null) user = body.get("user").toString();
                    if (body.get("password") != null) password = body.get("password").toString();
                }
            } catch (Exception ignored) {
            }
        }

        UserSession session = null;
        if ("rohit".equals(user) && "coverdrive".equals(password)) {
            session = new UserSession("player-rohit", "player", "Rohit Sharma");
        } else if ("fan".equals(user) && "fanpass".equals(password)) {
            session = new UserSession("fan-101", "fan", "Aarav Mehta");
        } else if ("fan2".equals(user) && "fanpass".equals(password)) {
            session = new UserSession("fan-102", "fan", "Riya Sen");
        }

        if (session == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("ok", false);
            error.put("error", "Invalid sign-in");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        String token = "cp-" + session.userId() + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        SESSIONS.put(token, session);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("token", token);
        response.put("user", session.name());
        response.put("role", session.role());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/poll")
    public ResponseEntity<Map<String, Object>> poll(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        UserSession session = getSession(authHeader);
        if (session == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("ok", false);
            error.put("error", "Sign in required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        boolean allowed = pollLimiterService.allowFanPoll(session.userId(), System.currentTimeMillis());
        if (!allowed) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("ok", false);
            error.put("error", "Poll limit reached");
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(error);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        response.put("message", "Vote counted");
        return ResponseEntity.ok(response);
    }
}
