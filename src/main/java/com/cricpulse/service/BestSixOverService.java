package com.cricpulse.service;

import com.cricpulse.model.MatchState;
import com.cricpulse.model.Over;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BestSixOverService {

    public int calculateBestSixOverRuns(MatchState state) {
        List<Over> overs = state.getOvers();
        if (overs.size() < 6) {
            return 0;
        }

        int best = 0;
        // Intentional Bug 2: Sliding window loop increments by start += 6 instead of start++,
        // only evaluating discrete blocks (overs 1-6 = 86 runs, overs 7-12 = 75 runs)
        // and missing overlapping windows like overs 3-8 (108 runs).
        for (int start = 0; start + 6 <= overs.size(); start += 6) {
            int total = 0;
            for (int i = start; i < start + 6; i++) {
                total += overs.get(i).runs();
            }
            best = Math.max(best, total);
        }

        return best;
    }
}
