# Pub/Sub Middleware — Project Guide

Reference doc for the IS3208 Assignment 01 group. Hand this to your AI agent along with the assignment PDF and it will have full context for your part.

---

## 1. Goal

Build a simple Publish/Subscribe middleware in **Java** using TCP sockets.

- One **server** (the broker) listens on a port.
- Many **clients** connect. Each client is either a `PUBLISHER` or a `SUBSCRIBER`, on a given `TOPIC`.
- Publisher messages are routed by the server to all subscribers of the same topic.
- Task 4 is a written distributed-architecture proposal (no code).

Assignment marks: Task 1 = 20, Task 2 = 30, Task 3 = 40, Task 4 = 10.

---

## 2. Current state of the repo

```
src/
  server.java     # Task 1: single-client, single-thread
  client.java     # Task 1: sends stdin to server
screenshots/
  Task_1.png      # Task 1 evidence
MA-Assignment-1-2026 (1).pdf
```

Task 1 is done and committed. Everything below replaces / extends the Task 1 code.

---

## 3. Target architecture

All new code lives in `src/`. We are renaming the entry classes to start with capitals (Java convention) — `Server` and `Client`.

```
src/
  Server.java          # main(); parses port; accept loop; spawns ClientHandler per connection         [Hesanda]
  ClientHandler.java   # Runnable; reads REGISTER handshake; reads subsequent lines; forwards to Broker [Hesanda]
  Broker.java          # Thread-safe registry: topic -> Set<ClientHandler subscribers>; broadcast      [Dana]
  Client.java          # main(); parses args; sends REGISTER; subscriber receiver thread; stdin loop    [Isum]
  Protocol.java        # Wire protocol constants + parse/format helpers                                 [Ushitha]
  Logger.java          # Timestamped, level-tagged logger used by Server / ClientHandler / Broker      [Ushitha]
  AdminCommands.java   # Server-side LIST / WHO / STATS commands wired into ClientHandler              [Umaya]
  TestRunner.java      # Spawns N Client instances programmatically for the demo + load checks         [Umaya]
```

### Data flow

```
  PUBLISHER client  --(REGISTER PUBLISHER TOPIC_A)-->  Server  --(accept)--> ClientHandler
                    --(MSG hello)----------------->  ClientHandler --> Broker.publish("TOPIC_A","hello",pubId)

  SUBSCRIBER client --(REGISTER SUBSCRIBER TOPIC_A)--> Server --> ClientHandler
                                                                       |
                                                                       v
                                                              Broker.subscribe("TOPIC_A", handler)

  Broker.publish iterates subscribers of TOPIC_A and writes
  "[pubId@TOPIC_A] hello" to each subscriber's socket.
```

### Wire protocol

Line-based UTF-8, one message per line, `\n` terminated.

| Direction | Format | Notes |
|---|---|---|
| Client → Server (first line) | `REGISTER <ROLE> <TOPIC>` | `ROLE` is `PUBLISHER` or `SUBSCRIBER`. `TOPIC` defaults to `GLOBAL` when not provided. |
| Publisher → Server | `MSG <payload>` | Payload may contain spaces; everything after `MSG ` is the body. |
| Client → Server | `TERMINATE` | Clean disconnect. |
| Server → Subscriber | `[<pubId>@<TOPIC>] <payload>` | `pubId` is a short id like `pub-3` assigned by the server. |
| Server → Client | `ACK <message>` / `ERR <message>` | Optional, for handshake feedback. |

Using a single protocol for Task 2 and Task 3 keeps the codebase simple: Task 2's "broadcast to all subscribers" is just Task 3 with every client on the same topic (`GLOBAL`).

### Command-line interface

```
# Server
java -cp out Server <port>
java -cp out Server 5000

# Client
java -cp out Client <server-ip> <port> <ROLE> [TOPIC]
java -cp out Client 127.0.0.1 5000 PUBLISHER  TOPIC_A
java -cp out Client 127.0.0.1 5000 SUBSCRIBER TOPIC_A
java -cp out Client 127.0.0.1 5000 PUBLISHER          # defaults to TOPIC=GLOBAL
```

The client should keep running until the user types `terminate` (case-insensitive). On `terminate` it sends `TERMINATE` to the server, then exits.

### Concurrency model

- `Server` has one accept loop on the main thread, hands each socket to a `ClientHandler` running on a thread pool (`Executors.newCachedThreadPool()` is fine).
- `Broker` keeps `ConcurrentHashMap<String, Set<ClientHandler>>` for subscribers. Use `ConcurrentHashMap.newKeySet()` or `Collections.synchronizedSet(new HashSet<>())` for the inner set.
- Each `ClientHandler` writes to its own socket — synchronize on the handler's `PrintWriter` if multiple threads might write to it (the broadcast loop iterates subscribers serially in one thread, so this is usually fine, but the handler's own reads/writes coexist with broker writes — guard with a `synchronized` on the writer to be safe).
- On disconnect (`TERMINATE`, EOF, or `IOException`), the handler must remove itself from the Broker.

---

## 4. Build and run

We're not adding Maven/Gradle — just `javac`. Ushitha will add a small run script as part of integration, but the manual flow is:

```bash
# Compile
mkdir -p out
javac -d out src/*.java

# Terminal 1 — server
java -cp out Server 5000

# Terminal 2 — subscriber on TOPIC_A
java -cp out Client 127.0.0.1 5000 SUBSCRIBER TOPIC_A

# Terminal 3 — publisher on TOPIC_A
java -cp out Client 127.0.0.1 5000 PUBLISHER TOPIC_A
> hello world
> terminate
```

---

## 5. Member assignments

Six members, six coding parts. Everyone writes Java. Submission packaging (zip + VLE upload) is a **group responsibility** — see §7.

| # | Member | Files |
|---|---|---|
| 1 | **Ashen** ✅ | `src/server.java`, `src/client.java`, Task 1 screencast (done) |
| 2 | **Hesanda** | `src/Server.java`, `src/ClientHandler.java` |
| 3 | **Dana** | `src/Broker.java` |
| 4 | **Isum** | `src/Client.java` |
| 5 | **Ushitha** | `src/Protocol.java`, `src/Logger.java`, `README.md`, `run-*.sh`, Task 2 + 3 screencasts |
| 6 | **Umaya** | `src/AdminCommands.java`, `src/TestRunner.java`, `docs/Task_4.pdf` |

> **Coordination rule:** Hesanda, Dana, Isum, Ushitha and Umaya should agree on the exact method signatures listed under "Interfaces" below *before* writing implementations. Once those are fixed, each member can work in isolation.

### Interfaces everyone depends on (lock these in first)

```java
// Protocol.java  (Ushitha — write this first; everyone imports it)
public class Protocol {
    public static final String REGISTER  = "REGISTER";
    public static final String MSG       = "MSG";
    public static final String TERMINATE = "TERMINATE";
    public static final String ACK       = "ACK";
    public static final String ERR       = "ERR";
    public static final String DEFAULT_TOPIC = "GLOBAL";

    // Helpers
    public static String formatRegister(String role, String topic);  // "REGISTER PUBLISHER TOPIC_A"
    public static String formatMsg(String payload);                  // "MSG hello world"
    public static String formatBroadcast(String pubId, String topic, String payload);
                                                                     // "[pub-3@TOPIC_A] hello world"
    public static String[] parseRegister(String line);               // returns {role, topic} or throws
    public static String parseMsgPayload(String line);               // returns payload after "MSG "
}

// Logger.java  (Ushitha — also written first)
public class Logger {
    public static void info(String tag, String msg);   // "2026-06-25 10:21:03 [server] Listening on 5000"
    public static void warn(String tag, String msg);
    public static void error(String tag, String msg, Throwable t);
}

// Broker.java  (Dana)
public class Broker {
    public static Broker getInstance();
    public void subscribe(String topic, ClientHandler handler);
    public void unsubscribe(ClientHandler handler);                  // removes from all topics
    public void publish(String topic, String payload, String pubId); // writes to all subscribers of topic
    public java.util.Map<String, Integer> topicSubscriberCounts();   // for AdminCommands
    public long totalMessagesPublished();                            // for AdminCommands
}

// ClientHandler.java  (Hesanda)
public class ClientHandler implements Runnable {
    public ClientHandler(java.net.Socket socket);
    public String getId();              // e.g. "pub-3" or "sub-7"
    public String getRole();            // "PUBLISHER" | "SUBSCRIBER"
    public String getTopic();
    public void send(String line);      // thread-safe write to this client's socket
    public void run();
}

// AdminCommands.java  (Umaya)
public class AdminCommands {
    public static boolean isAdminCommand(String line);               // line starts with "/"
    public static String handle(String line, ClientHandler caller);  // returns response text
    // Supported: /list (all topics + sub counts), /who <topic>, /stats
}
```

---

### Member 1 — Ashen ✅ (done)

**Task 1**: single client/server socket app + Task 1 screencast.

Already on `master`.

---

### Member 2 — Hesanda — Server core

**Files**: `src/Server.java`, `src/ClientHandler.java`

**Deliverables**:
1. `Server.java` with a `main(String[] args)` that:
   - Validates `args[0]` as the port; prints usage and exits otherwise.
   - Opens a `ServerSocket`, calls `Logger.info("server", "Listening on port " + port)`.
   - Loops: `accept()` → wrap in `ClientHandler` → submit to an `ExecutorService` (cached thread pool).
   - Logs each accept + disconnect with the client's address and assigned id.
   - Handles `IOException` cleanly; shuts the pool down on JVM exit (`Runtime.addShutdownHook`).
2. `ClientHandler.java`:
   - Reads the first line; uses `Protocol.parseRegister(line)` to extract `<ROLE> <TOPIC>`; assigns an id (`pub-N` / `sub-N` using an `AtomicInteger`).
   - On `SUBSCRIBER`, calls `Broker.getInstance().subscribe(topic, this)`.
   - Loops reading subsequent lines:
     - If `AdminCommands.isAdminCommand(line)` → reply with `AdminCommands.handle(line, this)`.
     - `MSG <payload>` from a `PUBLISHER` → `Broker.publish(topic, Protocol.parseMsgPayload(line), getId())`.
     - `MSG ...` from a `SUBSCRIBER` → reply `ERR subscribers cannot publish`.
     - `TERMINATE` → break.
   - In `finally`: unsubscribe from broker, close socket, log disconnect.
   - Provide a thread-safe `send(String line)` that `synchronized`-wraps the `PrintWriter.println` + `flush`.

**Depends on**: `Protocol`, `Logger` (Ushitha), `Broker` (Dana), `AdminCommands` (Umaya). Stub these locally first if they aren't merged yet, then swap to the real classes during integration.

**Acceptance**:
- Server stays up when a client disconnects.
- 10 clients can connect simultaneously (test via shell loop).
- No deadlock when broker writes to subscribers concurrently with their own handler reads.

---

### Member 3 — Dana — Broker / routing logic

**File**: `src/Broker.java`

**Deliverables**:
1. Singleton `Broker` (static `getInstance()`).
2. Internal data: `ConcurrentHashMap<String, Set<ClientHandler>> subscribersByTopic`.
3. `subscribe(topic, handler)`:
   - `computeIfAbsent(topic, k -> ConcurrentHashMap.newKeySet()).add(handler)`.
   - Log `[broker] sub-7 subscribed to TOPIC_A (3 subscribers)`.
4. `unsubscribe(handler)`:
   - Remove from every topic; log.
5. `publish(topic, payload, pubId)`:
   - Look up subscribers for `topic`. If none, log `[broker] no subscribers for TOPIC_A; dropped`.
   - For each subscriber, call `handler.send("[" + pubId + "@" + topic + "] " + payload)`.
   - Catch per-subscriber `IOException`/`RuntimeException` so one dead socket doesn't break the loop. Schedule that handler for cleanup.
6. Maintain a stats counter (`messagesPublishedByTopic`) and expose `topicSubscriberCounts()` + `totalMessagesPublished()` for Umaya's `AdminCommands` to read.

**Depends on**: `Logger` (Ushitha) for log lines. Stub it (`System.out.println`) locally if not merged yet.

**Acceptance**:
- Publisher on TOPIC_A → only TOPIC_A subscribers receive.
- Publisher's own terminal does **not** echo the broadcast.
- Two publishers on the same topic both reach all topic subscribers in arrival order.
- Subscriber on a topic with no publishers stays connected and silent.

---

### Member 4 — Isum — Client app

**File**: `src/Client.java`

**Deliverables**:
1. Parse args: `<ip> <port> <ROLE> [TOPIC]`. Validate `ROLE ∈ {PUBLISHER, SUBSCRIBER}`. `TOPIC` defaults to `Protocol.DEFAULT_TOPIC`.
2. Open socket, send `Protocol.formatRegister(role, topic)` as the first line.
3. **Receiver thread** (daemon): reads lines from the server and prints them. Prefix incoming lines visibly (e.g. `<< [pub-3@TOPIC_A] hello`) so the demo videos read clearly.
4. **Main thread**:
   - If `PUBLISHER`: read stdin lines. If line equals `terminate` (case-insensitive) → send `Protocol.TERMINATE` and exit. Lines starting with `/` (e.g. `/list`, `/stats`) are sent as-is so the server's `AdminCommands` can handle them. Otherwise send `Protocol.formatMsg(line)`.
   - If `SUBSCRIBER`: still read stdin so the user can type `terminate` to disconnect; `/list` / `/stats` admin commands also work; any other typed text prints `(subscribers cannot publish)`.
5. On exit: close socket, join receiver thread with a short timeout.

**Depends on**: `Protocol` (Ushitha). Stub the helper methods locally if not merged yet.

**Acceptance**:
- `terminate` cleanly closes both ends.
- Subscriber sees publisher messages in real time (set `PrintWriter(..., true)` for autoflush on the server send path).
- Bad args print a clear usage message and exit non-zero.
- `/list` and `/stats` typed in a client print the admin response.

---

### Member 5 — Ushitha — Protocol + Logger + integration + videos

**Files**: `src/Protocol.java`, `src/Logger.java`, `README.md`, `run-server.sh`, `run-client.sh`, `screenshots/Task_2.png`, `screenshots/Task_3.png`, video files.

**Deliverables**:

**(a) Code — write these first; everyone else imports them**
1. `Protocol.java`:
   - Constants from §5 (`REGISTER`, `MSG`, `TERMINATE`, `ACK`, `ERR`, `DEFAULT_TOPIC`).
   - Helpers: `formatRegister`, `formatMsg`, `formatBroadcast`, `parseRegister`, `parseMsgPayload`.
   - `parseRegister` should throw `IllegalArgumentException` on a malformed line so the server can reply `ERR bad handshake`.
2. `Logger.java`:
   - `info(tag, msg)`, `warn(tag, msg)`, `error(tag, msg, throwable)`.
   - Format: `2026-06-25 10:21:03 [tag] msg`. Use `java.time.LocalDateTime` + a `DateTimeFormatter`.
   - `synchronized` on `System.out` so concurrent logs don't interleave.

**(b) Build script + README**
3. `README.md` covering: project description, build (`javac -d out src/*.java`), run examples for Task 1/2/3 scenarios, wire-protocol summary, admin commands (`/list`, `/who`, `/stats`).
4. `run-server.sh` and `run-client.sh` wrapping the `java -cp out ...` invocations.

**(c) Integration testing + videos**
5. Manual end-to-end test scenarios (documented in README):
   - **Task 2 demo**: 1 server, 2 publishers + 3 subscribers all on `GLOBAL` → all 3 subscribers receive, publishers don't echo themselves.
   - **Task 3 demo**: 1 server, pub on `TOPIC_A`, pub on `TOPIC_B`, sub on `TOPIC_A`, sub on `TOPIC_B`, sub on `TOPIC_A`. Verify routing isolation.
   - **Disconnect test**: subscriber dies mid-stream; server keeps running; publisher can still publish.
6. **Screencast videos** for Task 2 and Task 3 (≤2 min each). Show:
   - Each terminal labelled with its role + topic.
   - Multiple message rounds.
   - A `terminate` to demonstrate clean shutdown.
   - Save as `videos/Task_2.mp4` and `videos/Task_3.mp4`.

**Acceptance**:
- `Protocol` + `Logger` merged before B/C/D start (or B/C/D can stub locally and swap during integration).
- Anyone in the group can follow `README.md` from a clean clone to a working demo.

---

### Member 6 — Umaya — Admin commands + Test harness + Task 4 doc

**Files**: `src/AdminCommands.java`, `src/TestRunner.java`, `docs/Task_4.pdf` (or `.docx`).

**Deliverables**:

**(a) Code**
1. `AdminCommands.java`:
   - `isAdminCommand(line)` → `true` if `line` starts with `/`.
   - `handle(line, caller)` supports:
     - `/list` → "topics: TOPIC_A=3, TOPIC_B=1" (uses `Broker.topicSubscriberCounts()`).
     - `/who <topic>` → subscriber count for that topic.
     - `/stats` → "messages routed: 142" (uses `Broker.totalMessagesPublished()`).
     - Unknown → `ERR unknown admin command`.
   - All output goes back to the calling `ClientHandler` via `caller.send(...)`.
2. `TestRunner.java`:
   - `main(String[] args)` that takes `<host> <port> <numPublishers> <numSubscribers> <topic>`.
   - Spawns N+M `Client` instances in threads (re-use the existing `Client` class — don't duplicate it). Publishers send a few canned messages with a small sleep between each, then terminate. Subscribers stay up for a few seconds, log what they received, then terminate.
   - Used by the group for load-checking and for one of the demo video segments.

**(b) Documentation**
3. **Task 4 document** (~1–2 pages) proposing a distributed Pub/Sub architecture that removes the single-server SPOF. Must include:
   - A diagram (draw.io, Excalidraw, or similar — PNG embedded).
   - A description of:
     - **Broker cluster** (multiple broker nodes, each holding subscription state).
     - **Topic partitioning / replication** (e.g. each topic primary + N replicas; quorum writes for durability).
     - **Failover** (leader election via ZooKeeper / Raft, or gossip-based).
     - **Client discovery** (clients use a bootstrap list and learn the cluster; reconnect on broker death).
     - **Trade-offs** (CAP — explicitly call out what we give up; latency vs durability).
   - 2–3 sentences mapping each improvement back to *availability* and *reliability* specifically (the PDF asks for both).

**Depends on**: `Broker.topicSubscriberCounts()` / `totalMessagesPublished()` (Dana), `Client` class (Isum). Stub them while waiting.

**Acceptance**:
- `/list`, `/who <topic>`, `/stats` typed in any connected client return correct values.
- `TestRunner 127.0.0.1 5000 2 3 TOPIC_A` runs to completion with subscribers receiving publisher messages.
- Task 4 doc explicitly addresses both availability *and* reliability.

---

## 6. Sequencing

Roughly two phases of one week each.

**Week 1**
- Day 1: everyone reads this guide. The whole group agrees on the interfaces in §5.
- Day 2: **Ushitha** lands `Protocol.java` + `Logger.java` first so the others can import them. **Umaya** drafts the `AdminCommands` skeleton so Hesanda has something to wire into `ClientHandler`.
- Days 3–5: Hesanda (server), Dana (broker), Isum (client) code in parallel against the locked interfaces.
- Days 6–7: integration — everyone pulls master, fixes glue issues together. Umaya finishes `TestRunner` once `Client` is in.

**Week 2**
- Days 8–9: Ushitha records Task 2 + Task 3 videos. Umaya drafts Task 4.
- Days 10–11: Umaya finalises Task 4 doc.
- Day 12: **whole-group dry run** — fresh clone → build → run all 3 demos → review videos → review Task 4 doc.
- Day 13: **group submission session** — assemble the zip together, every member verifies their files are present, one person uploads to VLE on a screen-share with the rest watching. Buffer / submit.

---

## 7. Coordination notes

- **Branch per member** if possible (`feat/server-core`, `feat/broker`, `feat/client`, `feat/protocol-logger`, `feat/admin-testrunner`) → PR into `master`.
- Keep commit messages descriptive — useful evidence of individual contribution if a viva is called.
- Don't change the wire protocol or the public method signatures in §5 without telling the rest of the group.
- For viva readiness: each member should be able to explain not just their file, but how it fits into the data-flow diagram in §3.

### Submission packaging — group responsibility

The final zip is **not assigned to a single person**. On submission day everyone helps:

1. Together, agree the repo is feature-complete (all 6 parts merged, all 3 demo runs pass).
2. Together, create the submission folder:
   ```
   GroupX-Pubsub-IS3208/
     src/                # all .java files
     videos/             # Task_1.mp4, Task_2.mp4, Task_3.mp4
     screenshots/        # supporting images
     docs/Task_4.pdf
     README.md
     PROJECT_GUIDE.md
   ```
3. Each member verifies their own files are present and correct.
4. Zip the folder. One person uploads to the VLE while the rest watch on a screen-share so everyone confirms it went through.

---

## 8. Working with your AI agent

Recommended kickoff prompt for each member (replace `<NAME>` with your own):

> I'm `<NAME>` in a 6-person group building a Java Pub/Sub middleware for IS3208 Assignment 01. Read `PROJECT_GUIDE.md` (in the repo root) and the assignment PDF. My part is the section in §5 titled with my name. Confirm you understand the interfaces in §5, then implement the files listed in my section. Don't modify files outside my section unless I tell you to.

Per-member quick reference:

- **Hesanda** → §5 Member 2 — `Server.java` + `ClientHandler.java`
- **Dana** → §5 Member 3 — `Broker.java`
- **Isum** → §5 Member 4 — `Client.java`
- **Ushitha** → §5 Member 5 — `Protocol.java` + `Logger.java` + README + videos
- **Umaya** → §5 Member 6 — `AdminCommands.java` + `TestRunner.java` + Task 4 doc
