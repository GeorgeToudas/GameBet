# GameBet — Distributed Betting System

A distributed betting platform with a **Java backend** (Master-Worker architecture) and an **Android frontend** (mobile client). The backend uses raw TCP sockets, MapReduce aggregation, consistent-hash routing, and a cryptographically verified Secure Random Generator (SRG).

---

## Repository Structure

```
GameBet/
├── backend/                   ← Java distributed server system
│   ├── src/GameBet/
│   │   ├── Master.java
│   │   ├── Worker.java
│   │   ├── Reducer.java
│   │   ├── SRGServer.java
│   │   ├── PlayerApp.java     ← CLI client (alternative to Android)
│   │   ├── Manager.java       ← Admin CLI
│   │   ├── Game.java
│   │   ├── Player.java
│   │   ├── AppConfig.java
│   │   ├── GameJson.java
│   │   ├── KeyValue.java
│   │   ├── SearchRequest.java
│   │   └── SRGResponse.java
│   └── config/
│       ├── config.properties
│       ├── worker1.properties
│       └── worker2.properties
│
├── frontend/                  ← Android Studio project (mobile client)
│   └── app/src/main/
│       ├── java/
│       │   ├── com/example/GameBetApp/
│       │   │   ├── MainActivity.java
│       │   │   ├── GamesActivity.java
│       │   │   ├── GameDetailsActivity.java
│       │   │   ├── SearchActivity.java
│       │   │   ├── GameAdapter.java
│       │   │   └── MasterConnection.java
│       │   └── GameBet/           ← shared serializable model classes
│       │       ├── Game.java
│       │       ├── Player.java
│       │       ├── SearchRequest.java
│       │       └── SRGResponse.java
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_games.xml
│           │   ├── activity_game_details.xml
│           │   ├── activity_search.xml
│           │   └── item_game.xml
│           └── drawable/
│               ├── lucky7.png
│               └── test1.png
│
├── .gitignore
└── README.md
```

---

## System Architecture

```
  ┌─────────────────────┐      ┌───────────────┐
  │   Android App        │      │    Manager    │
  │  MainActivity        │      │  (Admin CLI)  │
  │  GamesActivity       │      └──────┬────────┘
  │  GameDetailsActivity │             │
  │  SearchActivity      │             │ TCP / Java Serialization
  └──────────┬───────────┘             │
             │  10.0.2.2:4445          │
             └──────────┬──────────────┘
                        ▼
            ┌───────────────────────┐
            │         MASTER        │
            │       Port: 4445      │
            │  • Worker registry    │
            │  • Consistent-hash    │
            │    routing            │
            │  • MapReduce dispatch │
            └────────┬──────────────┘
                     │
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
            │    REDUCER     │
            │  Port: 6000    │
            │ SEARCH reduce  │
            │ SUM_DOUBLE     │
            └────────────────┘

            ┌────────────────┐
            │   SRG Server   │
            │  Port: 3000    │
            │ Producer-      │
            │ Consumer buf   │
            │ SHA-256 verify │
            └────────────────┘
```

---

## Android Frontend — Screens

| Screen | File | Description |
|--------|------|-------------|
| Main Menu | `MainActivity` | Add balance, navigate to games/search |
| Available Games | `GamesActivity` | ListView with all active games + logo |
| Game Details | `GameDetailsActivity` | Full game info, Play button, Rate button |
| Search | `SearchActivity` | Filter by min stars, risk (Spinner), bet category (Spinner) |

### Key implementation detail
The Android app and Java backend share the **same serialized model classes** (`Game`, `SearchRequest`, `SRGResponse`) under the `GameBet` package. This allows direct Java Object Serialization over TCP — no REST API or JSON conversion needed.

Communication is handled by `MasterConnection.java`, which opens a TCP socket, writes the command + payload with `ObjectOutputStream`, and reads the response with `ObjectInputStream` — identical to the CLI `PlayerApp`.

> **Emulator note:** `10.0.2.2` is Android's special alias for the host machine's `localhost`. For a real device on the same LAN, replace with the Master's actual IP.

---

## Backend Design Patterns

### 1. MapReduce
Operations spanning all workers (search, stats) follow a two-phase pattern:

**Map phase** — Master broadcasts to every Worker with a shared `jobId`. Each Worker filters/aggregates its local data and sends a partial result to the Reducer via `STORE_PARTIAL`.

**Reduce phase** — Master requests `GET_RESULT` from the Reducer, which waits for all partial results then merges:
- `SEARCH` → deduplicates `Game` objects by name
- `SUM_DOUBLE` → sums `KeyValue<String, Double>` entries per key

### 2. Consistent Hash Routing
For single-worker commands (`play`, `addGame`, `addBalance`, etc.):
```java
int workerIndex = Math.floorMod(routingKey.hashCode(), workers.size());
```
The routing key is the first element of the payload (e.g. `gameName`, `playerId`), ensuring the same entity always reaches the same Worker.

### 3. Producer-Consumer (SRG Server)
`SRGServer` maintains a `RandomBuffer` (bounded blocking queue) per game secret. A background `RandomProducer` thread fills it with `SecureRandom` integers. Each `play` request consumes one number, and the Worker verifies it by recomputing `SHA-256(randomNumber + secret)`.

### 4. Multi-threaded TCP Server
Every server accepts connections in a `while(true)` loop and dispatches each to a new `Thread`. Communication uses Java's `ObjectInputStream` / `ObjectOutputStream`.

---

## Technologies

| What | Details |
|------|---------|
| Backend language | Java (JDK 11+) |
| Frontend | Android (Java, minSdk 8+) |
| Networking | Raw TCP Sockets (`java.net`) |
| Serialization | Java Object Serialization |
| Cryptography | `MessageDigest` SHA-256, `SecureRandom` |
| Concurrency | `synchronized`, `wait/notifyAll`, `Thread` |
| Configuration | `java.util.Properties` via `AppConfig` |
| JSON parsing | Custom regex-based `GameJson` parser |
| Build (backend) | IntelliJ IDEA (no Maven/Gradle) |
| Build (frontend) | Android Studio |
| External libraries | **None** — pure Java SE + Android SDK |

---

## Configuration

### `config.properties`
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
worker.port=7001        # 7002 for worker 2
master.host=localhost
master.port=4445
```

> For multi-machine setup, replace `localhost` with the actual LAN IP. See [Multi-Machine Setup](#multi-machine-setup).

---

## How to Run

### Backend — Start in this order

| Step | Class | Config |
|------|-------|--------|
| 1 | `SRGServer` | `config/config.properties` |
| 2 | `Reducer` | `config/config.properties` |
| 3 | `Master` | `config/config.properties` |
| 4 | `Worker` ×2 | `worker1.properties`, `worker2.properties` |
| 5 | `PlayerApp` or Android app | `config/config.properties` |

Each class uses `args[0]` as the config file path. Set this in IntelliJ via `Run → Edit Configurations → Program arguments`.

### Frontend — Android App
1. Open the `frontend/` folder in **Android Studio**
2. Make sure the backend Master is running
3. Run on emulator — it connects to `10.0.2.2:4445` (host machine localhost)
4. For a real device, change `MASTER_HOST` in each Activity to the Master's LAN IP

---

## Multi-Machine Setup

1. Run `ipconfig` (Windows) on each machine to find its IPv4 address
2. Update all `.properties` files with the correct IPs
3. Open firewall ports:

| Machine | Ports |
|---------|-------|
| Master | 4445 |
| Reducer | 6000 |
| SRG | 3000 |
| Worker 1 | 7001 |
| Worker 2 | 7002 |

4. For Android on a real device, update `MASTER_HOST` in `MainActivity`, `GamesActivity`, `GameDetailsActivity`, `SearchActivity`

---

## Game Mechanics

### Risk Multipliers
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

**Jackpot:** triggered when `rnd % 100 == 0` → 10×, 20×, 40× for low/medium/high.

### Bet Categories
- `$` — minBet < 1.0
- `$$` — minBet 1.0–4.99
- `$$$` — minBet ≥ 5.0

---

## Authors

Developed as a university assignment  
AUEB