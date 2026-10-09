package com.cricpulse;

import com.cricpulse.model.MatchState;
import com.cricpulse.service.BestSixOverService;
import com.cricpulse.service.MatchDataService;
import com.cricpulse.service.PartnershipReachabilityService;
import com.cricpulse.service.PlayerAccessService;
import com.cricpulse.service.PollLimiterService;
import com.cricpulse.service.RollingRateService;
import com.cricpulse.service.StrongestChainService;

import java.util.ArrayList;
import java.util.List;

public final class RunTests {

    @FunctionalInterface
    private interface TestCase {
        void execute() throws Exception;
    }

    private static final class TestResult {
        String name;
        String status;
        String error;
        long durationMs;
    }

    private static final List<TestResult> RESULTS = new ArrayList<>();

    public static void main(String[] args) {
        // Test 1: Partnership Reachability
        run("test_recursive_partnership_scan_visits_all_connected_players", () -> {
            PartnershipReachabilityService reachability = new PartnershipReachabilityService();
            MatchState state = new MatchState(161, 3, 211, 12);
            for (int i = 0; i < 4; i++) {
                state.addPlayer(i, "P" + i, "Batter");
            }
            state.addPartnership(0, 1, 10);
            state.addPartnership(0, 2, 9);
            state.addPartnership(2, 3, 8);
            List<Integer> reachable = reachability.getReachablePlayers(state, 0);
            require(reachable.contains(3), "Partnership scan missed the second branch");
        });

        // Test 2: Best Six-Over Stretch
        run("test_best_six_over_stretch_includes_overlapping_windows", () -> {
            BestSixOverService bestSixOver = new BestSixOverService();
            MatchState state = MatchDataService.createSampleMatch();
            int runs = bestSixOver.calculateBestSixOverRuns(state);
            require(runs == 108, "Expected best six-over stretch of 108 runs, got " + runs);
        });

        // Test 3: Rolling Run Rate
        run("test_rolling_run_rate_uses_recent_overs", () -> {
            RollingRateService rollingRate = new RollingRateService();
            MatchState state = MatchDataService.createSampleMatch();
            double rate = rollingRate.calculateRollingRunRate(state);
            require(rate > 11.3 && rate < 11.4, "Expected recent three-over rate near 11.33, got " + rate);
        });

        // Test 4: Strongest Partnership Chain
        run("test_partnership_chain_maximizes_minimum_link", () -> {
            StrongestChainService strongestChain = new StrongestChainService();
            MatchState state = MatchDataService.createSampleMatch();
            List<Integer> chain = strongestChain.findStrongestChain(state, 0, 5);
            int strength = strongestChain.calculateChainStrength(state, chain);
            require(strength == 30, "Expected strongest chain bottleneck of 30 runs, got " + strength);
        });

        // Test 5: Player Focus Note Access Control
        run("test_fan_cannot_edit_player_focus_note", () -> {
            PlayerAccessService playerAccess = new PlayerAccessService();
            require(!playerAccess.canSavePlayerNote("fan"), "Fan role must not edit a player-only focus note");
        });

        // Test 6: Fan Poll Rate Limiter
        run("test_poll_rate_limit_is_per_fan", () -> {
            PollLimiterService pollLimiter = new PollLimiterService();
            pollLimiter.reset();
            boolean first = true;
            for (int i = 0; i < 3; i++) {
                first = pollLimiter.allowFanPoll("fan-a", 50_000) && first;
            }
            boolean second = pollLimiter.allowFanPoll("fan-b", 50_000);
            require(first && second, "A second fan should have an independent poll allowance");
        });

        writeTelemetryAndExit();
    }

    private static void run(String name, TestCase testCase) {
        long start = System.nanoTime();
        TestResult result = new TestResult();
        result.name = name;
        try {
            testCase.execute();
            result.status = "passed";
        } catch (Throwable t) {
            result.status = "failed";
            result.error = (t.getMessage() != null && !t.getMessage().isBlank())
                    ? t.getMessage()
                    : t.getClass().getSimpleName();
        }
        result.durationMs = (System.nanoTime() - start) / 1_000_000;
        RESULTS.add(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void writeTelemetryAndExit() {
        int passed = 0;
        int failed = 0;
        long totalTimeMs = 0;

        StringBuilder sb = new StringBuilder("{");
        for (TestResult r : RESULTS) {
            if (sb.length() > 1) {
                sb.append(',');
            }
            sb.append('"').append(r.name).append("\":{");
            sb.append("\"Status\":\"").append(r.status).append("\",");
            sb.append("\"Execution time\":\"").append(r.durationMs).append("ms\"");
            if (r.error != null) {
                sb.append(",\"Error\":\"").append(escapeJson(r.error)).append('"');
            }
            sb.append('}');

            totalTimeMs += r.durationMs;
            if ("passed".equals(r.status)) {
                passed++;
            } else {
                failed++;
            }
        }

        sb.append(",\"Passed\":").append(passed);
        sb.append(",\"Failed\":").append(failed);
        sb.append(",\"Total bugs\":").append(failed);
        sb.append(",\"Total Execution time\":\"").append(totalTimeMs).append("ms\"}");

        System.out.println(sb.toString());

        if (failed > 0) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        for (char c : input.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        out.append(String.format("\\u%04x", (int) c));
                    } else {
                        out.append(c);
                    }
                }
            }
        }
        return out.toString();
    }
}
