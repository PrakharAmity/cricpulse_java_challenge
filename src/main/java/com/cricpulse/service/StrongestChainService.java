package com.cricpulse.service;

import com.cricpulse.model.MatchState;
import com.cricpulse.model.PartnershipLink;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

@Service
public class StrongestChainService {

    public List<Integer> findStrongestChain(MatchState state, int source, int destination) {
        // Intentional Bug 4: Standard BFS using java.util.Queue finds path with fewest hops
        // (0 -> 1 -> 5, bottleneck 20) instead of maximizing minimum link capacity
        // (0 -> 2 -> 4 -> 5, bottleneck 30).
        Map<Integer, Integer> previous = new HashMap<>();
        Queue<Integer> frontier = new ArrayDeque<>();
        frontier.add(source);
        previous.put(source, source);

        while (!frontier.isEmpty()) {
            int current = frontier.poll();
            if (current == destination) {
                break;
            }
            for (PartnershipLink link : state.getPartnershipsForPlayer(current)) {
                int neighbor = link.getOther(current);
                if (previous.containsKey(neighbor)) {
                    continue;
                }
                previous.put(neighbor, current);
                frontier.add(neighbor);
            }
        }

        if (!previous.containsKey(destination)) {
            return Collections.emptyList();
        }

        List<Integer> chain = new ArrayList<>();
        for (int p = destination; p != source; p = previous.get(p)) {
            chain.add(p);
        }
        chain.add(source);
        Collections.reverse(chain);
        return chain;
    }

    public int calculateChainStrength(MatchState state, List<Integer> chain) {
        if (chain.size() < 2) {
            return 0;
        }
        int weakest = Integer.MAX_VALUE;
        for (int i = 1; i < chain.size(); i++) {
            int u = chain.get(i - 1);
            int v = chain.get(i);
            int runs = state.getPartnershipRuns(u, v);
            weakest = Math.min(weakest, runs);
        }
        return weakest;
    }
}
