package com.cricpulse.controller;

import com.cricpulse.service.MatchDataService;
import com.cricpulse.service.PlayerAccessService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class NoteController {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MatchDataService matchDataService;
    private final PlayerAccessService playerAccessService;

    public NoteController(MatchDataService matchDataService, PlayerAccessService playerAccessService) {
        this.matchDataService = matchDataService;
        this.playerAccessService = playerAccessService;
    }

    @GetMapping("/player-note")
    public ResponseEntity<Map<String, Object>> getNote() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("note", matchDataService.getPlayerFocusNote());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/player-note")
    public ResponseEntity<Map<String, Object>> saveNote(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletRequest request) {

        AuthController.UserSession session = AuthController.getSession(authHeader);
        if (session == null) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("ok", false);
            error.put("error", "Sign in required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        if (!playerAccessService.canSavePlayerNote(session.role())) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("ok", false);
            error.put("error", "Player access required");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }

        String note = request.getParameter("note");
        String contentType = request.getContentType();
        if (note == null && contentType != null && contentType.contains("application/json")) {
            try (InputStream in = request.getInputStream()) {
                Map<?, ?> body = MAPPER.readValue(in, Map.class);
                if (body != null && body.get("note") != null) {
                    note = body.get("note").toString();
                }
            } catch (Exception ignored) {
            }
        }

        if (note == null) {
            note = "";
        }
        matchDataService.setPlayerFocusNote(note);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        return ResponseEntity.ok(response);
    }
}
