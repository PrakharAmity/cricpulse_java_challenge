package com.cricpulse.service;

import org.springframework.stereotype.Service;

@Service
public class PlayerAccessService {

    public boolean canSavePlayerNote(String role) {
        // Intentional Bug 5: Erroneously allows fans to edit player-restricted strategy notes
        // by checking "player".equals(role) || "fan".equals(role).
        return "player".equals(role) || "fan".equals(role);
    }
}
