# CricPulse — Java Challenge Developer & AI Guide

This document provides a technical overview of the `cricpulse-java-challenge` project, including architecture, repository layout, match data specifications, and behavioral descriptions of the **6 intentional challenge bugs**, reproduction steps, and expected behaviors.

---

## 1. Project Overview

**CricPulse** is a live cricket analytics dashboard and match center service built with Java 17 and Spring Boot 3.2.5. It tracks a live T20 World Cup match between India (chasing 211) and Australia (210/7), offering real-time ball-by-ball momentum, rolling run rates, fan polling, player strategy notes, and an interactive partnership network graph.

The application runs as a lightweight Spring Boot service with an in-memory match state store and a responsive web dashboard. The match center modules are separated into standalone service units so each analytical and security requirement can be evaluated independently without external database dependencies.

---

## 2. Repository Structure

```text
cricpulse-java-challenge/
├── pom.xml                                   # Maven POM configuring Java 17 and Spring Boot 3.2.5
├── challenge.json                            # Evaluation environment runtime configuration
├── README.md                                 # Developer build, run, and API documentation
├── AI.md                                     # AI and bug diagnosis reference (this document)
├── start.sh                                  # Service startup script (mvn spring-boot:run)
├── src/
│   ├── main/
│   │   ├── java/com/cricpulse/
│   │   │   ├── Application.java              # Spring Boot main application entry point
│   │   │   ├── controller/
│   │   │   │   ├── MatchController.java      # REST endpoints for match state, analytics, reachable nodes
│   │   │   │   ├── AuthController.java       # User authentication and fan poll submission
│   │   │   │   └── NoteController.java       # Player focus note retrieval and updates
│   │   │   ├── model/
│   │   │   │   ├── MatchState.java           # Complete match state, overs, players, graph links
│   │   │   │   ├── Player.java               # Player model (id, name, role)
│   │   │   │   ├── Over.java                 # Over model (number, runs)
│   │   │   │   └── PartnershipLink.java      # Undirected partnership edge (from, to, runs)
│   │   │   └── service/
│   │   │       ├── MatchDataService.java     # Seed match data and player strategy note state
│   │   │       ├── BestSixOverService.java   # [Bug 2] Computes highest-scoring 6-over stretch
│   │   │       ├── RollingRateService.java   # [Bug 3] Computes recent 3-over rolling run rate
│   │   │       ├── StrongestChainService.java# [Bug 4] Finds strongest partnership chain
│   │   │       ├── PartnershipReachabilityService.java # [Bug 1] Graph reachability traversal
│   │   │       ├── PlayerAccessService.java  # [Bug 5] Role access control for player note
│   │   │       └── PollLimiterService.java   # [Bug 6] Rate limiter for fan polls
│   │   └── resources/
│   │       ├── application.properties        # Configures server.port=5000
│   │       └── static/
│   │           ├── index.html                # Match center frontend dashboard
│   │           ├── style.css                 # Dark sports analytics theme & graph styles
│   │           └── app.js                    # Live polling, SVG graph rendering & UI bindings
│   └── test/
│       └── java/com/cricpulse/
│           └── RunTests.java                 # Standalone test runner outputting strict JSON
└── tests/
    └── run_tests.sh                          # Recompiles and executes RunTests with exit status
```

---

## 3. Match Data & Graph Specifications

- **Score**: 161/3 in 12.0 overs (Target: 211). Opponent: Australia (210/7).
- **Over Runs Array (12 overs)**: `[7, 7, 18, 18, 18, 18, 18, 18, 5, 5, 5, 24]`
- **Players (6 players)**:
  - `0`: Rohit Sharma (Opener)
  - `1`: Virat Kohli (Batter)
  - `2`: Shubman Gill (Batter)
  - `3`: Suryakumar Yadav (Batter)
  - `4`: Hardik Pandya (All-rounder)
  - `5`: Ravindra Jadeja (All-rounder)
- **Undirected Partnership Links**:
  - `(0, 1)`: 38 runs
  - `(0, 2)`: 30 runs
  - `(0, 3)`: 14 runs
  - `(1, 5)`: 20 runs
  - `(2, 4)`: 45 runs
  - `(3, 4)`: 22 runs
  - `(4, 5)`: 34 runs
- **Initial Focus Note**: `"Play straight early; accelerate after the powerplay."`

---

## 4. The 6 Intentional Bugs Breakdown

These are the six behavioral bug surfaces covered by the challenge. Each entry describes where the issue lives, how to reproduce it, what symptoms occur, and what the expected system behavior is.

### Bug 1: Partnership reachability scan terminates prematurely

- **Bug Location**: `src/main/java/com/cricpulse/service/PartnershipReachabilityService.java`
- **How to Observe It**: Click Rohit Sharma (node 0) in the "Partnership chain" section of the web dashboard, or execute `./tests/run_tests.sh`.
- **Failure**: The traversal terminates prematurely after visiting only the first neighbor, missing players along other branches and returning only 1 connected teammate instead of all connected teammates. In the web dashboard, the UI warns that sibling branches were missed.
- **Expected**: The traversal should explore all reachable branches in the partnership network and return all connected teammates in the player's network component (all 5 teammates for player 0).

---

### Bug 2: Best six-over stretch misses overlapping windows

- **Bug Location**: `src/main/java/com/cricpulse/service/BestSixOverService.java`
- **How to Observe It**: Inspect the "BEST 6 OVERS" badge on the "Run rate, over by over" momentum card in the web dashboard, or execute `./tests/run_tests.sh`.
- **Failure**: The evaluation only inspects non-overlapping blocks rather than contiguous sliding windows across the innings, yielding 86 runs instead of the true peak scoring window.
- **Expected**: The calculation should evaluate every contiguous 6-over window across the match to discover the true maximum scoring stretch (108 runs).

---

### Bug 3: Rolling run rate averages full innings instead of recent overs

- **Bug Location**: `src/main/java/com/cricpulse/service/RollingRateService.java`
- **How to Observe It**: Check the "LIVE FORM / Rolling run rate" card in the web dashboard sidebar, or execute `./tests/run_tests.sh`.
- **Failure**: The calculation averages the overall match run rate across all completed overs (displaying 13.42 / over) rather than measuring recent batting momentum.
- **Expected**: The metric should reflect recent batting form by calculating the average run rate strictly over the last 3 completed overs (~11.33 runs/over).

---

### Bug 4: Partnership chain selects shortest path rather than maximum bottleneck

- **Bug Location**: `src/main/java/com/cricpulse/service/StrongestChainService.java`
- **How to Observe It**: Check the "Strongest chain" header and highlighted path in the "Partnership chain" card in the web dashboard, or execute `./tests/run_tests.sh`.
- **Failure**: The path discovery selects the route with the fewest hops rather than the strongest connection strength, resulting in a low bottleneck link of 20 runs.
- **Expected**: The algorithm should find the path between the selected players that maximizes the strength of the weakest partnership link along the chain (achieving a bottleneck strength of 30 runs).

---

### Bug 5: Fan role is permitted to modify player focus notes

- **Bug Location**: `src/main/java/com/cricpulse/service/PlayerAccessService.java`
- **How to Observe It**: Switch to a fan account (e.g. "Fan 1 (Aarav)") in the Fan Zone, enter text into the "PLAYER WORKSPACE" note, and click "Save note"; or execute `./tests/run_tests.sh`.
- **Failure**: Unprivileged fan accounts are permitted to update internal team strategy notes. In the UI, the save operation succeeds for fan accounts instead of being denied.
- **Expected**: Access control must restrict editing permissions strictly to users with the player role. Attempts by fans to modify the note must be rejected with an authorization error.

---

### Bug 6: Poll rate limiter is shared globally rather than enforced per fan

- **Bug Location**: `src/main/java/com/cricpulse/service/PollLimiterService.java`
- **How to Observe It**: Vote 3 times as "Fan 1 (Aarav)", switch to "Fan 2 (Riya)", and attempt to vote; or execute `./tests/run_tests.sh`.
- **Failure**: Rate limiting is applied globally across all requests rather than on a per-user basis. When one fan exhausts their voting quota, other fans are immediately blocked as well.
- **Expected**: Rate limiting should be tracked independently per user, granting each fan their own distinct allowance of 3 poll submissions per time window.

---

## 5. Expected Behaviour After Fixing All Bugs

- Clicking any player in the partnership network scans and highlights all connected teammates across all network branches.
- The momentum stretch display shows the true maximum 6-over scoring window across all overlapping intervals (108 runs).
- The rolling run rate reflects the current batting momentum over the last 3 overs (~11.33 runs/over).
- The strongest partnership chain identifies the route that maximizes the weakest shared run partnership (30 runs bottleneck).
- Player workspace strategy notes are protected, allowing only authenticated player accounts to update them.
- Fan poll voting enforces a fair 3-vote limit per fan independently, without cross-user interference.

---

## 6. Test Suite & Verification

Run the test suite from the repository root:
```bash
./tests/run_tests.sh
```

- When the 6 intentional bugs are present, the test runner outputs JSON telemetry with failed statuses for all 6 tests and terminates with exit code `1`.
- When all 6 bugs are fixed, the test runner outputs JSON telemetry with passed statuses for all 6 tests and terminates with exit code `0`.
