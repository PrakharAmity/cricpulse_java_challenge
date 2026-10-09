# CricPulse — Live Match Center & Cricket Analytics

CricPulse is an interactive live cricket match center and telemetry analytics application built for sports analysts, team strategists, and fans. It tracks a live T20 World Cup match between India (chasing 211) and Australia (210/7), providing real-time ball-by-ball momentum visualization, sliding window scoring metrics, recent rolling run rates, authenticated player tactical workspaces, rate-limited fan polling, and an interactive partnership network graph.

---

## 1. Application Overview

### Core Functionality
- **Live Match Center & Telemetry**: Monitor real-time score updates (161/3 in 12.0 overs chasing 211), required run rates, and recent ball-by-ball scoring logs.
- **Over-by-Over Momentum Analysis**: Visualize run progression across all 12 overs with dynamic bar charts and automated detection of the highest-scoring 6-over scoring stretch.
- **Rolling Form Run Rate**: Track immediate batting momentum across recent overs to compare active acceleration against the overall match run rate.
- **Partnership Network Graph**: Interactive SVG network graph displaying undirected batting partnerships between players, supporting reachability traversal and bottleneck strength path analysis.
- **Player Tactical Workspace**: Secure, authenticated tactical focus note editor restricted strictly to active team players.
- **Fan Zone Live Polling**: Community engagement poll with token-based session management and per-fan sliding window rate limiting.

### Technology Stack
- **Language**: Java 17+
- **Framework**: Spring Boot 3.2.5 (`spring-boot-starter-web`)
- **Port**: `http://localhost:5000`
- **Frontend / Styling**: Vanilla JavaScript, semantic HTML5, custom responsive CSS (dark sports analytics theme, native SVG canvas graph)
- **Testing**: Standalone Java test suite (`RunTests.java`) executed via `./tests/run_tests.sh` outputting strict single-line JSON telemetry
- **Data Storage**: In-memory match state store (no external database or network dependencies)

### Demo Accounts
- **Player**: `user="rohit"`, `password="coverdrive"` (Role: `"player"`, Name: Rohit Sharma)
- **Fan 1**: `user="fan"`, `password="fanpass"` (Role: `"fan"`, Name: Aarav Mehta)
- **Fan 2**: `user="fan2"`, `password="fanpass"` (Role: `"fan"`, Name: Riya Sen)

---

## 2. Debugging Challenge

QA engineers and early users have flagged several issues in the CricPulse match center platform. Your goal is to investigate the codebase, reproduce each bug, and implement the necessary fixes so that all automated test suites pass.

### Reported Issues & Tasks:

#### Issue 1: Partnership Reachability Scan Misses Teammate Branches
- **User Symptom**: When an analyst clicks on Rohit Sharma (node 0) in the partnership network graph, the dashboard displays an orange warning: *"Reachable from Rohit: only Kohli (1/5 teammates). Sibling branches missed!"*. The graph traversal terminates prematurely after visiting only the first neighbor, missing teammates across other connected branches.
- **Task**: Fix the graph traversal so that clicking any player explores all reachable branches and returns all connected teammates in the player's partnership network component.

#### Issue 2: Best Six-Over Stretch Calculation Skips Overlapping Windows
- **User Symptom**: On the "Run rate, over by over" momentum card, the *"BEST 6 OVERS"* badge displays only 86 runs (evaluating discrete blocks: overs 1–6). Analysts point out that the team experienced a major scoring surge between overs 3 and 8 totaling 108 runs, which the system fails to detect.
- **Task**: Update the sliding window calculation so that it evaluates every contiguous 6-over window across the match to discover the true maximum scoring stretch (108 runs).

#### Issue 3: Rolling Run Rate Displays Full Match Average Instead of Recent Form
- **User Symptom**: The "LIVE FORM / Rolling run rate" sidebar card displays `13.42 / over`, which reflects the total innings run rate across all 12 overs rather than active batting momentum. Analysts expect the card to reflect the team's immediate form over recent overs.
- **Task**: Modify the rolling run rate calculation to compute the average run rate strictly over the most recent 3 completed overs (~11.33 runs/over).

#### Issue 4: Strongest Partnership Chain Favors Fewest Hops Over Highest Strength
- **User Symptom**: The partnership chain analyzer highlights a route from Rohit to Jadeja through Kohli (`0 → 1 → 5`) reporting a bottleneck of 20 runs. However, a stronger link path exists through Gill and Hardik (`0 → 2 → 4 → 5`) with a bottleneck strength of 30 runs. The algorithm erroneously selects the shortest path by hop count rather than the path with the strongest minimum link.
- **Task**: Implement a maximum-bottleneck path discovery algorithm to identify the partnership chain that maximizes the strength of the weakest link between the selected players (30 runs bottleneck).

#### Issue 5: Fan Accounts Are Permitted to Edit Player Strategy Notes
- **User Symptom**: When a user logs in with a fan account (e.g., "Fan 1 (Aarav)") and clicks "Save note" in the Player Workspace, the system allows the submission and displays: *"Saved! (Bug: Fan was allowed to edit player note)"*. Internal team strategy notes should be protected from unauthorized edits by non-players.
- **Task**: Enforce role-based access control so that only users with the `"player"` role can modify the player focus note, rejecting fan submissions with an authorization error (HTTP 403 Forbidden).

#### Issue 6: Fan Poll Rate Limiter Locks Out All Users When One Fan Hits Quota
- **User Symptom**: When Fan 1 votes 3 times and reaches their voting limit, switching to Fan 2 and attempting to vote immediately fails with HTTP 429: *"Poll limit reached: Rate limit blocked Riya Sen!"*. The rate limiter shares a global counter across all requests instead of tracking allowances independently per user.
- **Task**: Update the rate limiter to enforce submission limits independently per `userId`, ensuring every fan receives their own full voting quota within the active time window.

---

## 3. Expected Behavior After Fixing Bugs

After resolving the issues:
1. Clicking any player in the partnership network scans and highlights all connected teammates across all network branches (all 5 teammates for player 0).
2. The momentum stretch badge and chart highlight the true maximum 6-over scoring interval across all overlapping windows (108 runs).
3. The rolling run rate accurately reflects recent batting momentum over the last 3 overs (~11.33 runs/over).
4. The strongest partnership chain identifies the path that maximizes the weakest shared partnership link (30 runs bottleneck).
5. Player workspace strategy notes are protected, allowing only authenticated player accounts to update them while rejecting fan submissions with HTTP 403 Forbidden.
6. Fan poll voting enforces an independent 3-vote limit per fan without cross-user interference.
7. All automated tests in `tests/run_tests.sh` pass with exit code `0`.

---

## 4. How to Build, Run & Test

### Compile the Application
```bash
mvn clean compile
```

### Start the Live Server
```bash
mvn spring-boot:run
```
Or execute:
```bash
./start.sh
```
Access the dashboard at `http://localhost:5000`.

### Run Automated Tests
```bash
./tests/run_tests.sh
```
- Initial state (with 6 bugs): Fails with exit code `1` and outputs strict single-line JSON test telemetry.
- Resolved state (all bugs fixed): Passes with exit code `0` and reports `{"Passed": 6, "Failed": 0, ...}`.
