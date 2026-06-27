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
  server.java   # Task 1 + Task 2 (multi-threaded; PUBLISHER/SUBSCRIBER broadcast).  Untested.
  client.java   # Task 1 + Task 2 (3rd arg = role; daemon receiver thread).          Untested.
screenshots/
  Task_1.png    # Task 1 evidence
MA-Assignment-1-2026 (1).pdf
PROJECT_GUIDE.md
```

Ashen has already delivered **Task 1** and a first cut of **Task 2** (committed as "Task 2 Complete Untested" and merged into `master`). It still needs verification with multiple terminals, and Task 3 (topic filtering) + Task 4 (write-up) + polish are open.

We are extending Ashen's single-file design — *not* throwing it away and rewriting it from scratch.

---

## 3. What Ashen's Task 2 already does

### `src/server.java`
- `public class server` with an inner `static class ClientHandler implements Runnable`.
- `main` opens a `ServerSocket`, runs an infinite accept loop, and starts a new `Thread` per connection.
- A static `CopyOnWriteArrayList<ClientHandler> clients` tracks every connected handler.
- Each handler reads a first line `REGISTER|<ROLE>` to learn its role.
- When a `PUBLISHER` sends a line, `broadcastToSubscribers(line)` iterates the list and calls `client.send(message)` on every `SUBSCRIBER`.
- On `terminate` or disconnect, the handler removes itself from the list and closes the socket.

### `src/client.java`
- Args: `<server-ip> <port> <PUBLISHER|SUBSCRIBER>`.
- Sends `REGISTER|<ROLE>` as the first line.
- Spawns a **daemon listener thread** that reads from the socket and prints `[BROADCAST]<line>` for every server message.
- Main thread reads stdin and writes each line to the server.
- `terminate` exits the loop and closes the socket.

### What's missing for full marks

| Task | Status | Gap |
|---|---|---|
| Task 1 (20) | ✅ done | — |
| Task 2 (30) | 🟡 untested | Needs an end-to-end test + screencast. A few small issues to clean up (see §5 Hesanda + Isum). |
| Task 3 (40) | ❌ not started | Need 4th arg = topic. Filter broadcast by topic. |
| Task 4 (10) | ❌ not started | Distributed architecture write-up. |

---

## 4. Wire protocol (extended for Task 3)

Ashen used `|` as the separator. We keep that to avoid breaking his code.

| Direction | Format | Notes |
|---|---|---|
| Client → Server, first line | `REGISTER\|<ROLE>\|<TOPIC>` | `ROLE` = `PUBLISHER` or `SUBSCRIBER`. `TOPIC` defaults to `GLOBAL` if 4th arg missing — keeps Task 2 demos still working. |
| Client → Server | raw text (publisher) | Server reads any non-`REGISTER` line and treats it as a message. |
| Client → Server | `terminate` | Case-insensitive. Disconnects. |
| Server → Subscriber | `[<topic>] <payload>` | Subscribers see which topic the message came from. |

### Command-line interface (after Task 3)

```
# Server
java -cp out server <port>
java -cp out server 5000

# Client
java -cp out client <server-ip> <port> <ROLE> [TOPIC]
java -cp out client 127.0.0.1 5000 PUBLISHER  TOPIC_A
java -cp out client 127.0.0.1 5000 SUBSCRIBER TOPIC_A
java -cp out client 127.0.0.1 5000 PUBLISHER             # defaults to TOPIC=GLOBAL
```

---

## 5. Member assignments

Six members, six parts. Ashen has Task 1 + Task 2 (untested). The other five share the remaining work. Everyone codes. Submission packaging (zip + VLE upload) is a **group responsibility** — see §7.

| # | Member | Files | What |
|---|---|---|---|
| 1 | **Ashen** ✅ | `src/server.java`, `src/client.java`, Task 1 screencast | Task 1 + Task 2 base implementation. Needs verification. |
| 2 | **Hesanda** | `src/server.java` (extend) | Task 3 server-side: topic in handshake, per-handler topic, topic-filtered broadcast. Fix any Task 2 bugs found in testing. |
| 3 | **Dana** | `src/client.java` (extend) | Task 3 client-side: 4th arg = topic, send `REGISTER\|ROLE\|TOPIC`, improve incoming-message formatting. |
| 4 | **Isum** | `src/server.java` + `src/client.java` (hardening) | Robustness pass: graceful disconnect on broken sockets, NPE protection in `send()`, proper finally-blocks, server stays up on handler exceptions. |
| 5 | **Ushitha** | `README.md`, `run-server.sh`, `run-client.sh`, Task 2 + Task 3 screencasts | Build + run instructions, integration testing, the two demo videos. |
| 6 | **Umaya** | `docs/Task_4.pdf` | Distributed Pub/Sub architecture write-up (diagram + description). Plus help with cross-task testing. |

> No "size" labels — everyone has a real, scoped part. Don't expand your scope without telling the rest of the group.

---

### Member 1 — Ashen ✅ (Task 1 + Task 2 base)

Already merged into `master`. Stays available for fixes if Task 2 bugs surface during testing.

---

### Member 2 — Hesanda — Task 3 server-side

**File**: `src/server.java`

**Changes**:
1. Update the `REGISTER` parse: split on `|`, expect `REGISTER|ROLE|TOPIC`. If the 3rd field is missing, default `topic = "GLOBAL"`.
2. Add a `private String topic;` field to `ClientHandler` and a `getTopic()` accessor.
3. In `broadcastToSubscribers(line)`:
   - Change the signature to `broadcastToSubscribers(String line, String topic)`.
   - Only call `client.send(...)` when the subscriber's topic equals the publisher's topic.
   - Format the broadcast as `[<topic>] <line>` so subscribers can see which topic it came from.
4. When a publisher sends a message, pass `this.topic` to the broadcast.
5. Log on the server: which client subscribed to which topic, and which topic each message was routed to.

**Acceptance**:
- Publisher on `TOPIC_A` → only `TOPIC_A` subscribers receive. `TOPIC_B` subscribers see nothing.
- Two publishers on the same topic both reach all that topic's subscribers.
- Subscribers on a topic with no publishers stay connected and silent.
- Old Task 2 demo (no 4th arg) still works because everyone defaults to `GLOBAL`.

---

### Member 3 — Dana — Task 3 client-side

**File**: `src/client.java`

**Changes**:
1. Accept an optional 4th CLI arg `<TOPIC>`. If missing, default `topic = "GLOBAL"`.
2. Send `REGISTER|<ROLE>|<TOPIC>` instead of `REGISTER|<ROLE>`.
3. In the receiver thread, print incoming lines clearly:
   - Currently: `[BROADCAST]<line>`
   - New: keep `[BROADCAST]` prefix but make sure newlines/spacing read well in a CLI demo.
4. Print the connection summary including the topic: `Connected to server X:Y as PUBLISHER on TOPIC_A`.
5. Add a usage line update: `Usage: java client <SERVER_IP> <PORT> <PUBLISHER|SUBSCRIBER> [TOPIC]`.

**Acceptance**:
- Bad args (missing role, unknown role) → clear usage message and non-zero exit.
- Running without a topic still connects on `GLOBAL`.
- Subscriber CLI shows publisher messages without the user needing to type anything (Ashen's daemon thread already does this — just verify after your change).

---

### Member 4 — Isum — Robustness / hardening

**Files**: `src/server.java`, `src/client.java`

Ashen's code is functional but has rough edges. Tighten it.

**Server-side**:
1. `ClientHandler.send(String message)` currently dereferences `writer` directly. If a subscriber's socket dies mid-broadcast, the publisher's thread will throw an exception that kills the whole publisher — wrap `writer.println(message)` in a try/catch and remove the dead handler from `clients` instead of propagating.
2. In `run()`, the `try-with-resources` only covers the `BufferedReader`. The `PrintWriter` and `Socket` aren't closed on every exception path — restructure so `finally` reliably closes the socket and writer.
3. If the first line isn't a valid `REGISTER`, send back an error and close the socket instead of leaving `role` as `null` and accepting messages.
4. Print disconnect events with the role (`PUBLISHER disconnected`, `SUBSCRIBER disconnected`).

**Client-side**:
1. The `Socket`, `PrintWriter`, `BufferedReader`, and `Scanner` are not in a try-with-resources block — if anything throws, sockets leak. Wrap them.
2. The daemon listener prints `"Connection closed."` on `IOException` even for normal terminations — make that quieter (only print if we didn't request `terminate`).
3. Handle `NumberFormatException` on the port arg with a clear message.

**Acceptance**:
- Killing a subscriber mid-stream (Ctrl-C) does not bring the server or other publishers down.
- A client passed bad args exits cleanly with a clear message.
- No `ResourceLeak`/`unclosed socket` warnings from the IDE on either file.

---

### Member 5 — Ushitha — README + run scripts + demo videos

**Files**: `README.md`, `run-server.sh`, `run-client.sh`, `videos/Task_2.mp4`, `videos/Task_3.mp4`, `screenshots/Task_2.png`, `screenshots/Task_3.png`.

**Deliverables**:

1. **`README.md`** at the repo root covering:
   - One-paragraph project description.
   - Build: `mkdir -p out && javac -d out src/*.java`.
   - Run examples for Task 1, Task 2, Task 3 scenarios (copy the commands from §4 of this guide).
   - Wire-protocol summary (link to §4 here).
   - "How to record the demo videos" section so future-you can re-record if needed.

2. **Helper scripts** (optional but nice):
   - `run-server.sh` — wraps `java -cp out server "$1"`.
   - `run-client.sh` — wraps `java -cp out client "$@"`.
   - `chmod +x` both.

3. **Integration testing** (manual, documented in `README.md`):
   - Task 2 demo: 1 server, 2 publishers + 3 subscribers (no topic / all on `GLOBAL`). All subscribers receive, publishers don't echo themselves.
   - Task 3 demo: 1 server, pub on `TOPIC_A`, pub on `TOPIC_B`, sub on `TOPIC_A`, sub on `TOPIC_B`, sub on `TOPIC_A`. Verify routing isolation.
   - Disconnect test: kill a subscriber mid-stream; server keeps running; publisher can still publish.

4. **Screencast videos** for Task 2 and Task 3 (≤2 minutes each). Show:
   - Each terminal labelled with its role + topic (use `iTerm` or `Terminal` profile names so they're readable).
   - Multiple message rounds across at least 3 clients.
   - A `terminate` to demonstrate clean shutdown.
   - Save as `videos/Task_2.mp4` and `videos/Task_3.mp4`. Drop a still frame into `screenshots/` too.

**Acceptance**: anyone in the group can `git clone`, follow `README.md`, and reach a working demo within 5 minutes.

---

### Member 6 — Umaya — Task 4 doc + cross-task testing

**Files**: `docs/Task_4.pdf` (or `.docx` then export to PDF).

**Deliverables**:
1. **Task 4 document** (~1–2 pages) proposing a distributed Pub/Sub architecture that removes the single-server SPOF. Must include:
   - A diagram (draw.io, Excalidraw, or similar — embed PNG).
   - Description of:
     - **Broker cluster** (multiple broker nodes, each holding subscription state).
     - **Topic partitioning / replication** (each topic primary + N replicas; quorum writes for durability).
     - **Failover** (leader election via ZooKeeper / Raft, or gossip-based).
     - **Client discovery** (clients use a bootstrap list, learn the cluster, reconnect on broker death).
     - **Trade-offs** (CAP — explicitly call out what we give up; latency vs durability).
   - 2–3 sentences explicitly mapping each improvement back to *availability* AND *reliability* (the PDF asks for both).

2. **Cross-task testing assist**: once Hesanda + Dana + Isum have merged, pair with Ushitha and try to break the system before recording the final videos. Edge cases worth checking:
   - Subscriber connects *before* any publisher exists on its topic.
   - Publisher connects *first*, no subscribers — does anything blow up?
   - Two clients connect simultaneously (race in the accept loop).
   - Very long messages.
   - Unicode messages.

**Acceptance**:
- Task 4 doc explicitly addresses both availability *and* reliability.
- At least one bug found (or "no bugs found" reported with the matrix you ran).

---

## 6. Sequencing

Roughly two weeks. Today's footing assumes Ashen has just merged Task 2 (untested).

**Week 1**
- Day 1: everyone pulls `master` and reads this guide.
- Days 2–3: Isum runs the hardening pass on Ashen's Task 2 code. **Block** Hesanda + Dana on this — they should pull after Isum's PR lands so they're patching clean code.
- Days 4–5: Hesanda (server topic) + Dana (client topic) work in parallel. Coordinate on the exact `REGISTER|ROLE|TOPIC` wire format so neither side drifts.
- Days 6–7: integration — Ushitha runs the test matrix, files issues back to whoever wrote the bug.

**Week 2**
- Days 8–9: Ushitha records Task 2 + Task 3 videos. Umaya drafts Task 4.
- Days 10–11: Umaya finalises Task 4, helps Ushitha re-test if any of the videos surfaced a bug.
- Day 12: **whole-group dry run** — fresh clone → build → run all 3 demos → review videos → review Task 4 doc.
- Day 13: **group submission session** — assemble the zip together, every member verifies their files are present, one person uploads to VLE on a screen-share with the rest watching.

---

## 7. Coordination notes

- **Branch per member** is recommended: `feat/task3-server` (Hesanda), `feat/task3-client` (Dana), `feat/hardening` (Isum), `docs/readme-and-videos` (Ushitha), `docs/task4` (Umaya). Each merges to `master` via PR. Lets a viva point at individual commits.
- Keep commit messages descriptive — useful evidence of contribution if a viva is called.
- **Don't change the wire protocol** (`REGISTER|ROLE|TOPIC`, plain text messages, `terminate`) without telling the rest of the group in the chat.
- For viva readiness: each member should be able to explain not just their file, but how it fits into the data-flow:
  ```
  PUBLISHER client --(REGISTER|PUBLISHER|TOPIC_A)--> server
                   --(hello)----------------------> server
                                                       |
                                                       v
                                              broadcastToSubscribers("hello", "TOPIC_A")
                                                       |
                                                       v
  for each handler in clients:
      if handler.role == SUBSCRIBER && handler.topic == "TOPIC_A":
          handler.send("[TOPIC_A] hello")
  ```

### Submission packaging — group responsibility

The final zip is **not assigned to a single person**. On submission day everyone helps:

1. Together, agree the repo is feature-complete (Task 3 merged, all 3 demo runs pass, Task 4 doc reviewed).
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

> I'm `<NAME>` in a 6-person group building a Java Pub/Sub middleware for IS3208 Assignment 01. Read `PROJECT_GUIDE.md` (in the repo root) and the assignment PDF. My part is the section in §5 titled with my name. The team is building on Ashen's Task 2 code (already merged on `master`) — do NOT rewrite that from scratch. Confirm you understand the wire protocol in §4, then implement the changes listed in my section.

Per-member quick reference:

- **Hesanda** → §5 Member 2 — Task 3 server-side topic filtering (`src/server.java`)
- **Dana** → §5 Member 3 — Task 3 client-side topic arg (`src/client.java`)
- **Isum** → §5 Member 4 — Robustness pass on both files
- **Ushitha** → §5 Member 5 — README + run scripts + Task 2/3 videos
- **Umaya** → §5 Member 6 — Task 4 architecture write-up + cross-task testing
