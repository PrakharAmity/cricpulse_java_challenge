package com.cricpulse.service;

import com.cricpulse.model.MatchState;
import com.cricpulse.model.Over;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RollingRateService {

    public double calculateRollingRunRate(MatchState state) {
        List<Over> overs = state.getOvers();
        if (overs.isEmpty()) {
            return 0.0;
        }

        // Intentional Bug 3: Computes full-innings average (161 / 12 = 13.416667)
        // instead of the average over the last 3 overs ((5 + 5 + 24) / 3.0 = 11.333333).
        int totalRuns = 0;
        for (Over over : overs) {
            totalRuns += over.runs();
        }
        return (double) totalRuns / overs.size();
    }
}
