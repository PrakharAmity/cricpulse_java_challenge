# CricPulse — Java Edition (`cricpulse-java-challenge`)

A 100% faithful Java edition of the **CricPulse** live match center and cricket analytics challenge. Built with **Java 17+** and **Spring Boot 3.2.5**, running on port `5000` (`server.port=5000`).

The application mirrors the Python/Flask and C++ CricPulse applications in data structures, REST API endpoints, web dashboard interface, strict JSON test telemetry, and contains the **exact same 6 intentional behavioral bugs**.

---

## Architecture & Technology Stack

- **Language**: Java 17+
- **Build System**: Apache Maven (`pom.xml`)
- **Framework**: Spring Boot 3.2.5 (`spring-boot-starter-web`)
- **Port**: `http://localhost:5000`
- **Storage**: In-memory state (no external database or network dependencies)
- **Frontend Assets**: Served from `src/main/resources/static/` (`index.html`, `style.css`, `app.js`)

---

## Repository Structure

```text
cricpulse-java-challenge/
├── pom.xml                                   # Maven project definition (Java 17, Spring Boot 3.2.5)
├── challenge.json                            # Environment runtime, port, and command configuration
├── README.md                                 # Project overview and run guide
├── AI.md                                     # Bug catalog, architecture, and observation guide
├── start.sh                                  # Service startup script
├── src/
│   ├── main/
│   │   ├── java/com/cricpulse/
│   │   │   ├── Application.java              # Spring Boot application entry point
│   │   │   ├── controller/
│   │   │   │   ├── MatchController.java      # /api/state, /api/analytics, /api/reachable/{id}
│   │   │   │   ├── AuthController.java       # /api/login, /api/poll
│   │   │   │   └── NoteController.java       # /api/player-note
│   │   │   ├── model/
│   │   │   │   ├── MatchState.java           # In-memory match state and partnership graph
│   │   │   │   ├── Player.java               # Player model record
│   │   │   │   ├── Over.java                 # Over runs record
│   │   │   │   └── PartnershipLink.java      # Graph partnership link record
│   │   │   └── service/
│   │   │       ├── MatchDataService.java     # Seed sample match and focus note state
│   │   │       ├── BestSixOverService.java   # [Bug 2] Highest-scoring 6-over stretch
│   │   │       ├── RollingRateService.java   # [Bug 3] Rolling recent run rate
│   │   │       ├── StrongestChainService.java# [Bug 4] Max-bottleneck partnership path
│   │   │       ├── PartnershipReachabilityService.java # [Bug 1] Reachable partner traversal
│   │   │       ├── PlayerAccessService.java  # [Bug 5] Role access control for strategy notes
│   │   │       └── PollLimiterService.java   # [Bug 6] Sliding window per-fan rate limiter
│   │   └── resources/
│   │       ├── application.properties        # server.port=5000
│   │       └── static/
│   │           ├── index.html                # Match center frontend dashboard
│   │           ├── style.css                 # Dark sports analytics theme & graph styling
│   │           └── app.js                    # Live polling, SVG graph rendering & bug hooks
│   └── test/
│       └── java/com/cricpulse/
│           └── RunTests.java                 # Standalone test runner outputting strict JSON
└── tests/
    └── run_tests.sh                          # Compilation and test execution script
```

---

## How to Build and Run

### 1. Compile the Project
```bash
mvn clean compile
```

### 2. Start the Spring Boot Web Server
```bash
mvn spring-boot:run
```
Or run the provided startup script:
```bash
./start.sh
```

The web dashboard is accessible in your browser at:
```text
http://localhost:5000
```

### 3. Run the Evaluation Tests
Execute the standalone test suite returning single-line strict JSON telemetry:
```bash
./tests/run_tests.sh
```

Before fixing the bugs, all 6 tests will fail with exit code `1`:
```json
{"test_recursive_partnership_scan_visits_all_connected_players":{"Status":"failed","Execution time":"...ms","Error":"Partnership scan missed the second branch"},"test_best_six_over_stretch_includes_overlapping_windows":{"Status":"failed","Execution time":"...ms","Error":"Expected best six-over stretch of 108 runs, got 86"},"test_rolling_run_rate_uses_recent_overs":{"Status":"failed","Execution time":"...ms","Error":"Expected recent three-over rate near 11.33, got 13.416666666666666"},"test_partnership_chain_maximizes_minimum_link":{"Status":"failed","Execution time":"...ms","Error":"Expected strongest chain bottleneck of 30 runs, got 20"},"test_fan_cannot_edit_player_focus_note":{"Status":"failed","Execution time":"...ms","Error":"Fan role must not edit a player-only focus note"},"test_poll_rate_limit_is_per_fan":{"Status":"failed","Execution time":"...ms","Error":"A second fan should have an independent poll allowance"},"Passed":0,"Failed":6,"Total bugs":6,"Total Execution time":"...ms"}
```

When all 6 bugs are fixed, all tests pass with exit code `0`:
```json
{"test_recursive_partnership_scan_visits_all_connected_players":{"Status":"passed","Execution time":"...ms"},"test_best_six_over_stretch_includes_overlapping_windows":{"Status":"passed","Execution time":"...ms"},"test_rolling_run_rate_uses_recent_overs":{"Status":"passed","Execution time":"...ms"},"test_partnership_chain_maximizes_minimum_link":{"Status":"passed","Execution time":"...ms"},"test_fan_cannot_edit_player_focus_note":{"Status":"passed","Execution time":"...ms"},"test_poll_rate_limit_is_per_fan":{"Status":"passed","Execution time":"...ms"},"Passed":6,"Failed":0,"Total bugs":0,"Total Execution time":"...ms"}
```

---

## REST API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Serves static match dashboard (`index.html`) |
| `GET` | `/api/state` | Returns JSON of match score, overs, players, and partnership links |
| `GET` | `/api/analytics` | Returns `{ bestSixOverRuns, rollingRunRate, chainStrength, chain }` |
| `GET` | `/api/reachable/{playerId}` | Returns `{ player_id, reachable: [int...] }` |
| `POST` | `/api/login` | Authenticates user credentials and issues session token |
| `POST` | `/api/poll` | Rate-limited fan poll voting endpoint (requires `Authorization: Bearer <token>`) |
| `GET` | `/api/player-note` | Retrieves the current player tactical focus note |
| `POST` | `/api/player-note` | Updates player focus note (role restricted to `"player"`) |

### Demo Accounts
- **Player**: `user="rohit"`, `password="coverdrive"` (Role: `"player"`, Name: Rohit Sharma)
- **Fan 1**: `user="fan"`, `password="fanpass"` (Role: `"fan"`, Name: Aarav Mehta)
- **Fan 2**: `user="fan2"`, `password="fanpass"` (Role: `"fan"`, Name: Riya Sen)

---

## The 6 Intentional Bugs Summary

1. **Partnership Reachability Scan** (`PartnershipReachabilityService.java`): DFS returns on the first neighbor, missing sibling branches.
2. **Best Six-Over Window** (`BestSixOverService.java`): Stride of 6 evaluates non-overlapping blocks (86 runs) rather than overlapping windows (108 runs).
3. **Rolling Run Rate** (`RollingRateService.java`): Computes entire innings average (13.42) instead of the recent 3-over average (11.33).
4. **Strongest Partnership Chain** (`StrongestChainService.java`): Unweighted BFS picks the path with fewest hops (bottleneck 20) instead of maximizing minimum bottleneck capacity (bottleneck 30).
5. **Player Focus Note Access Control** (`PlayerAccessService.java`): Erroneously allows `"fan"` role to modify player-only strategy notes.
6. **Fan Poll Rate Limiter** (`PollLimiterService.java`): Tracks a single global counter instead of rate-limiting per `userId`.

For full diagnosis, reproduction steps, and expected behaviors, see [`AI.md`](file:///c:/Users/prakh/Documents/Codex/2026-10-07/you-are-an-expert-application-engineer/outputs/cricpulse-java-challenge/AI.md).
#   c r i c p u l s e _ j a v a _ c h a l l e n g e  
 