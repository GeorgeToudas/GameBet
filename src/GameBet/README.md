# GameBet — Distributed Betting System

A distributed betting platform built entirely in **Java** using raw TCP sockets, implementing a **Master-Worker architecture** with MapReduce aggregation, consistent-hash routing, a dedicated Reducer node, and a cryptographically verified **Secure Random Generator (SRG) Server**.

---

## System Architecture

```
  ┌───────────────┐      ┌───────────────┐
  │   PlayerApp   │      │    Manager    │
  │  (CLI Client) │      │ (Admin CLI)   │
  └──────┬────────┘      └──────┬────────┘
         │                      │  TCP / Java Serialization
         └──────────┬───────────┘
                    ▼
        ┌───────────────────────┐
        │         MASTER        │
        │       Port: 4445      │
        │  • Worker registry    │
        │  • Consistent-hash    │
        │    routing            │
        │  • MapReduce dispatch │
        └────────┬──────────────┘
                 │ routes per routing key
        ┌────────┴─────────┐
        ▼                  ▼
  ┌───────────┐     ┌───────────┐
  │  Worker 1 │     │  Worker 2 │
  │ Port:7001 │     │ Port:7002 │
  │  gameMap  │     │  gameMap  │
  │  balances │     │  balances │
  └─────┬─────┘     └─────┬─────┘
        │   MAP phase      │
        └────────┬─────────┘
                 ▼
        ┌────────────────┐
        │    REDUCER      │
        │  Port: 6000    │
        │  SEARCH reduce │
        │  SUM_DOUBLE    │
        │    reduce      │
        └────────────────┘
                 ▲
        ┌────────┴────────┐
        │   SRG Server    │
        │  Port: 3000     │
        │  Producer-      │
        │  Consumer buf   │
        │  SHA-256 verify │
        └─────────────────┘
```

---

## Design Patterns

### 1. MapReduce
Operations that span all workers (search, stats) follow a two-phase pattern:

**Map phase** — Master broadcasts to every Worker with a shared `jobId`. Each Worker filters/aggregates its local data and sends a partial result to the Reducer via `STORE_PARTIAL`.

**Reduce phase** — Master asks the Reducer for `GET_RESULT`. The Reducer waits until all partial results for that `jobId` arrive, then merges them:
- `SEARCH` reduce → deduplicates `Game` objects by name
- `SUM_DOUBLE` reduce → sums `KeyValue<String, Double>` entries per key

### 2. Consistent Hash Routing
For commands that target a single Worker (`play`, `addGame`, `addBalance`, `rateGame`, etc.), the Master applies:

```java
int workerIndex = Math.floorMod(routingKey.hashCode(), workers.size());
```

The routing key is the **first element** of the payload (e.g. `gameName` for `play`/`addGame`, `playerId` for `addBalance`). This ensures the same entity always reaches the same Worker, keeping its local state consistent.

### 3. Producer-Consumer (SRG Server)
The `SRGServer` maintains a `RandomBuffer` (bounded blocking queue) per game secret. A background `RandomProducer` thread continuously fills the buffer with `SecureRandom` integers. Each `play` request consumes one number from the buffer. The Worker verifies the result by recomputing `SHA-256(randomNumber + secret)` and comparing it to the hash the SRG returned — ensuring tamper-proof randomness.

### 4. Multi-threaded TCP Server
Every server component (Master, Worker, Reducer, SRG) accepts connections in a `while(true)` loop and dispatches each one to a new `Thread`, achieving parallel request handling. Communication uses Java's `ObjectInputStream` / `ObjectOutputStream` for serialization.

---

##  Technologies

| What | Details |
|------|---------|
| Language | Java (JDK 11+) |
| Networking | Raw TCP Sockets (`java.net`) |
| Serialization | Java Object Serialization |
| Cryptography | `MessageDigest` SHA-256, `SecureRandom` |
| Concurrency | `synchronized` blocks, `wait/notifyAll` |
| Configuration | `java.util.Properties` via `AppConfig` |
| JSON parsing | Custom regex-based `GameJson` parser |
| Build / IDE | IntelliJ IDEA (no Maven/Gradle) |
| External libs | **None** — pure Java SE |

---

## Project Structure

```
GameBet/
├── src/
│   └── GameBet/
│       ├── Master.java          # Coordinator: routing, MapReduce dispatch
│       ├── Worker.java          # Game logic, player balances, map phase
│       ├── Reducer.java         # Partial result store + reduce phase
│       ├── SRGServer.java       # Secure Random Generator (Producer-Consumer)
│       ├── PlayerApp.java       # Player CLI client
│       ├── Manager.java         # Admin CLI client
│       ├── Game.java            # Game model (risk multipliers, jackpot)
│       ├── Player.java          # Player model
│       ├── AppConfig.java       # .properties config loader
│       ├── GameJson.java        # Regex-based JSON → Game parser
│       ├── KeyValue.java        # Generic pair used in MapReduce
│       ├── SearchRequest.java   # Filter object (stars, risk, betCategory)
│       └── SRGResponse.java     # SRG response (randomNumber + SHA-256 hash)
├── config/
│   ├── config.properties        # Shared config (ports, hosts, worker count)
│   ├── worker1.properties       # Worker 1 — port 7001
│   └── worker2.properties       # Worker 2 — port 7002
├── .gitignore
└── README.md
```

---

##  Configuration

### `config.properties` (used by Master, Reducer, SRG, Manager, PlayerApp)
```properties
master.host=localhost
master.port=4445

reducer.host=localhost
reducer.port=6000

srg.host=localhost
srg.port=3000
srg.buffer.size=10

workers.count=2
```

### `worker1.properties` / `worker2.properties`
```properties
worker.port=7001          # 7002 for worker 2
master.host=localhost
master.port=4445
reducer.host=localhost
reducer.port=6000
srg.host=localhost
srg.port=3000
```

>  For multi-machine setup, replace `localhost` with the actual LAN IP of each machine. See [Multi-Machine Setup](#multi-machine-setup) below.

---

## How to Run

Start components **in this exact order** (each as a separate Run Configuration in IntelliJ):

| Step | Class | Config arg |
|------|-------|------------|
| 1 | `SRGServer` | `config/config.properties` |
| 2 | `Reducer` | `config/config.properties` |
| 3 | `Master` | `config/config.properties` |
| 4 | `Worker` ×2 | `config/worker1.properties`, `config/worker2.properties` |
| 5 | `PlayerApp` | `config/config.properties` |
| 5 | `Manager` | `config/config.properties` (optional, admin only) |

### IntelliJ Run Configuration
For each class: `Run → Edit Configurations → Program arguments` → enter the path to its `.properties` file.

---

##  Multi-Machine Setup

To distribute components across different physical machines on the same LAN:

1. Run `ipconfig` (Windows) on each machine to find its IPv4 address.
2. Update all `.properties` files so each `*.host` points to the correct machine IP.
3. Open the required firewall ports on each machine:

| Machine | Open ports |
|---------|-----------|
| Master machine | 4445 |
| Reducer machine | 6000 |
| SRG machine | 3000 |
| Worker 1 machine | 7001 |
| Worker 2 machine | 7002 |

4. Start components in the order above.
5. Verify connectivity: `ping <target-ip>` from each machine before starting.

---

## Game Mechanics

### Risk Multipliers
Each game has a `riskLevel` (`low` / `medium` / `high`) that determines payout multipliers applied to the bet amount:

| Index (rnd % 10) | Low | Medium | High |
|-----------------|-----|--------|------|
| 0–2 | 0.0× | 0.0× | 0.0× |
| 3 | 0.1× | 0.0× | 0.0× |
| 4 | 0.5× | 0.0× | 0.0× |
| 5 | 1.0× | 0.5× | 0.0× |
| 6 | 1.1× | 1.0× | 0.0× |
| 7 | 1.3× | 1.5× | 1.0× |
| 8 | 2.0× | 2.5× | 2.0× |
| 9 | 2.5× | 3.5× | 6.5× |

**Jackpot**: triggered when `rnd % 100 == 0` → multipliers 10×, 20×, 40× for low/medium/high.

### Bet Categories
Derived automatically from `minBet`:
- `$` — minBet < 1.0
- `$$` — minBet 1.0–4.99
- `$$$` — minBet ≥ 5.0

---

## CLI Menus

### PlayerApp
```
1. Search games       (filter by stars, risk, bet category)
2. View available games
3. Play game
4. Rate game          (1–5 stars)
5. Add balance
6. View last search results
0. Exit
```

### Manager (Admin)
```
1. Add Game           (manual input)
2. Add Game from JSON (load .json file)
3. Remove Game        (soft-deactivate)
4. Update Game Risk
5. Show game/provider stats  (MapReduce aggregate)
6. Show player stats         (MapReduce aggregate)
0. Exit
```

---

##  Authors

Developed as a university assignment  
