package com.cricpulse.controller;

import com.cricpulse.model.MatchState;
import com.cricpulse.service.BestSixOverService;
import com.cricpulse.service.MatchDataService;
import com.cricpulse.service.PartnershipReachabilityService;
import com.cricpulse.service.RollingRateService;
import com.cricpulse.service.StrongestChainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class MatchController {

    private final MatchDataService matchDataService;
    private final BestSixOverService bestSixOverService;
    private final RollingRateService rollingRateService;
    private final StrongestChainService strongestChainService;
    private final PartnershipReachabilityService reachabilityService;

    public MatchController(MatchDataService matchDataService,
                           BestSixOverService bestSixOverService,
                           RollingRateService rollingRateService,
                           StrongestChainService strongestChainService,
                           PartnershipReachabilityService reachabilityService) {
        this.matchDataService = matchDataService;
        this.bestSixOverService = bestSixOverService;
        this.rollingRateService = rollingRateService;
        this.strongestChainService = strongestChainService;
        this.reachabilityService = reachabilityService;
    }

    @GetMapping("/state")
    public ResponseEntity<MatchState> getState() {
        return ResponseEntity.ok(matchDataService.getMatchState());
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics() {
        MatchState state = matchDataService.getMatchState();
        int bestSix = bestSixOverService.calculateBestSixOverRuns(state);
        double rolling = rollingRateService.calculateRollingRunRate(state);
        List<Integer> chain = strongestChainService.findStrongestChain(state, 0, 5);
        int chainStrength = strongestChainService.calculateChainStrength(state, chain);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bestSixOverRuns", bestSix);
        response.put("rollingRunRate", rolling);
        response.put("chainStrength", chainStrength);
        response.put("chain", chain);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/reachable/{playerId}")
    public ResponseEntity<Map<String, Object>> getReachable(@PathVariable int playerId) {
        MatchState state = matchDataService.getMatchState();
        List<Integer> reachable = reachabilityService.getReachablePlayers(state, playerId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("player_id", playerId);
        response.put("reachable", reachable);

        return ResponseEntity.ok(response);
    }
}
