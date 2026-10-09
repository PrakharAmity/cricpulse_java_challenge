package com.cricpulse.service;

import com.cricpulse.model.MatchState;
import org.springframework.stereotype.Service;

@Service
public class MatchDataService {

    public static final String INITIAL_NOTE = "Play straight early; accelerate after the powerplay.";

    private final MatchState matchState;
    private volatile String playerFocusNote = INITIAL_NOTE;

    public MatchDataService() {
        this.matchState = createSampleMatch();
    }

    public static MatchState createSampleMatch() {
        MatchState state = new MatchState(161, 3, 211, 12);

        int[] runs = {7, 7, 18, 18, 18, 18, 18, 18, 5, 5, 5, 24};
        for (int i = 0; i < runs.length; i++) {
            state.addOver(i + 1, runs[i]);
        }

        state.addPlayer(0, "Rohit Sharma", "Opener");
        state.addPlayer(1, "Virat Kohli", "Batter");
        state.addPlayer(2, "Shubman Gill", "Batter");
        state.addPlayer(3, "Suryakumar Yadav", "Batter");
        state.addPlayer(4, "Hardik Pandya", "All-rounder");
        state.addPlayer(5, "Ravindra Jadeja", "All-rounder");

        state.addPartnership(0, 1, 38);
        state.addPartnership(0, 2, 30);
        state.addPartnership(0, 3, 14);
        state.addPartnership(1, 5, 20);
        state.addPartnership(2, 4, 45);
        state.addPartnership(3, 4, 22);
        state.addPartnership(4, 5, 34);

        return state;
    }

    public MatchState getMatchState() {
        return matchState;
    }

    public String getPlayerFocusNote() {
        return playerFocusNote;
    }

    public void setPlayerFocusNote(String note) {
        this.playerFocusNote = note;
    }
}
