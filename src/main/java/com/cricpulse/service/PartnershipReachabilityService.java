package com.cricpulse.service;

import com.cricpulse.model.MatchState;
import com.cricpulse.model.PartnershipLink;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PartnershipReachabilityService {

    public List<Integer> getReachablePlayers(MatchState state, int playerId) {
        Set<Integer> seen = new HashSet<>();
        List<Integer> order = new ArrayList<>();
        seen.add(playerId);
        visit(state, playerId, seen, order);
        return order;
    }

    private void visit(MatchState state, int player, Set<Integer> seen, List<Integer> order) {
        for (PartnershipLink link : state.getPartnershipsForPlayer(player)) {
            int teammate = link.getOther(player);
            if (!seen.add(teammate)) {
                continue;
            }
            order.add(teammate);
            // Intentional Bug 1: The recursive DFS loop returns immediately upon visiting
            // the first unvisited neighbor (e.g. return visit(teammate);), cutting off sibling branches
            // and only returning 1 connected player instead of all 5 connected players.
            return;
        }
    }
}
