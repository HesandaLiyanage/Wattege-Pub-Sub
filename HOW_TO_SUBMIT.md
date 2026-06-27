# How to Complete & Submit the Assignment

**IS3208 — Middleware Architectures — Assignment 01**

This is the runbook to get from "code is done" → "zip uploaded to VLE". Follow it in order.

---

## 0. Prerequisites

- A machine with **JDK 11+** installed. Check with `java -version` and `javac -version`.
- A screen recorder. On macOS: **QuickTime Player → File → New Screen Recording**, or press `Cmd+Shift+5`. On Windows: **Game Bar (Win+G)** or **OBS**.
- A way to view markdown and export to PDF (any markdown editor → Print → Save as PDF, or paste into Google Docs).

---

## 1. Build the code

From the repo root:

```bash
mkdir -p out
javac -d out src/*.java
```

You should get three class files in `out/`:
- `server.class`
- `client.class`
- `server$ClientHandler.class`

If `javac` complains, fix the error before going further. No demo will record well if the code doesn't compile.

---

## 2. Smoke test the system

Open **three** terminal windows, all in the repo root.

**Terminal 1 — server:**
```bash
java -cp out server 5000
```
You should see:
```
Listening on port 5000
Waiting for clients on port 5000
```

**Terminal 2 — subscriber on TOPIC_A:**
```bash
java -cp out client 127.0.0.1 5000 SUBSCRIBER TOPIC_A
```

**Terminal 3 — publisher on TOPIC_A:**
```bash
java -cp out client 127.0.0.1 5000 PUBLISHER TOPIC_A
```

In Terminal 3, type `hello` and press Enter. Terminal 2 should print:
```
[BROADCAST][TOPIC_A] hello
```

If that works, the system works. Type `terminate` in each client to disconnect them, then `Ctrl-C` the server.

---

## 3. Record the three screencast videos

The assignment requires **one video per task** (Tasks 1, 2, 3). Each video should be ≤ 2 minutes, no narration required (text on screen is enough), and must show the CLI clearly. Keep the windows large and the font readable.

Create a `videos/` folder in the repo root before recording, so you can drop the files in:

```bash
mkdir -p videos
```

### 3.1 Task 1 video — basic client/server

Required by the spec: server + one client, client sends typed text → server displays it, `terminate` disconnects the client.

> **Note:** our current `server.java` and `client.java` are the Task 2/3 version. The Task 1 demo still works because Task 2/3 is a superset — just use the simplest possible invocation.

**Recording steps:**
1. Start recording.
2. **Terminal 1:** run `java -cp out server 5000`. Show the "Listening" line.
3. **Terminal 2:** run `java -cp out client 127.0.0.1 5000 PUBLISHER` (no topic — defaults to GLOBAL). Show the "Connected" line.
4. In Terminal 2, type 2–3 messages (`hello world`, `this is task 1`, `testing`). Each one should appear on Terminal 1 as `PUBLISHER says: <text>`.
5. In Terminal 2, type `terminate`. Show that the client exits and the server logs `PUBLISHER disconnected.`.
6. Stop recording.

Save as `videos/Task_1.mp4`.

### 3.2 Task 2 video — multiple publishers and subscribers (no topic filter)

Required: multiple concurrent clients; publisher messages reach all subscribers but NOT other publishers.

**You need 6 terminals open at once** (1 server + 2 publishers + 3 subscribers). Use a tiled layout so all are visible at once.

**Recording steps:**
1. Start recording.
2. **Terminal 1 (server):** `java -cp out server 5000`.
3. **Terminals 2–3 (publishers, GLOBAL topic):**
   ```bash
   java -cp out client 127.0.0.1 5000 PUBLISHER
   ```
4. **Terminals 4–6 (subscribers, GLOBAL topic):**
   ```bash
   java -cp out client 127.0.0.1 5000 SUBSCRIBER
   ```
5. In Terminal 2 type `message from pub 1`. **Point out (on-screen text or cursor)** that:
   - Terminal 1 (server) shows `PUBLISHER says: message from pub 1`.
   - Terminals 4, 5, 6 (subscribers) each show `[BROADCAST][GLOBAL] message from pub 1`.
   - Terminal 3 (the other publisher) shows **nothing** — publishers don't get publisher messages.
6. In Terminal 3 type `hi from pub 2`. Same verification.
7. Type `terminate` in each client; show the server staying up between disconnects.
8. Stop recording.

Save as `videos/Task_2.mp4`.

### 3.3 Task 3 video — topic-filtered pub/sub

Required: 4th CLI argument = topic; publisher messages only reach subscribers on the same topic.

**Layout (6 terminals):**

| Terminal | Command |
|---|---|
| 1 | `java -cp out server 5000` |
| 2 | `java -cp out client 127.0.0.1 5000 PUBLISHER  TOPIC_A` |
| 3 | `java -cp out client 127.0.0.1 5000 PUBLISHER  TOPIC_B` |
| 4 | `java -cp out client 127.0.0.1 5000 SUBSCRIBER TOPIC_A` |
| 5 | `java -cp out client 127.0.0.1 5000 SUBSCRIBER TOPIC_A` |
| 6 | `java -cp out client 127.0.0.1 5000 SUBSCRIBER TOPIC_B` |

**Recording steps:**
1. Start recording. Show the server starting and all 5 clients connecting (server prints `Client registered as: PUBLISHER on topic TOPIC_A` etc.).
2. In Terminal 2 (`PUBLISHER TOPIC_A`) type `news for topic A`. Verify:
   - Terminals 4 and 5 (`SUBSCRIBER TOPIC_A`) print `[BROADCAST][TOPIC_A] news for topic A`.
   - Terminal 6 (`SUBSCRIBER TOPIC_B`) shows **nothing**.
   - Terminal 3 (`PUBLISHER TOPIC_B`) shows **nothing**.
3. In Terminal 3 (`PUBLISHER TOPIC_B`) type `news for topic B`. Verify:
   - Terminal 6 (`SUBSCRIBER TOPIC_B`) prints it.
   - Terminals 4 and 5 (`SUBSCRIBER TOPIC_A`) show nothing.
4. Optionally send one more round to make routing obvious.
5. `terminate` everyone, stop recording.

Save as `videos/Task_3.mp4`.

---

## 4. Screenshots (optional but recommended)

For each task, grab one still frame from the running demo and save into `screenshots/`:

- `screenshots/Task_1.png` — already exists (Ashen committed it).
- `screenshots/Task_2.png` — six-terminal layout from §3.2.
- `screenshots/Task_3.png` — six-terminal layout from §3.3, with the routing isolation visible.

On macOS: `Cmd+Shift+4` then space to capture a window, or just the whole screen.

---

## 5. Task 4 — write-up

The Task 4 draft is at `docs/Task_4.md`. Convert it to PDF:

1. Open `docs/Task_4.md` in any markdown editor (Typora, VS Code with markdown preview, etc.) or paste into Google Docs.
2. Print → Save as PDF.
3. Save as `docs/Task_4.pdf`.

Optionally redraw the ASCII diagram in §2 of that doc with a real diagramming tool (draw.io, Excalidraw) and embed the PNG. The ASCII version is acceptable but a clean diagram looks better.

---

## 6. Assemble the submission

Create the final folder structure:

```
GroupX-Pubsub-IS3208/
├── src/
│   ├── server.java
│   └── client.java
├── videos/
│   ├── Task_1.mp4
│   ├── Task_2.mp4
│   └── Task_3.mp4
├── screenshots/
│   ├── Task_1.png
│   ├── Task_2.png
│   └── Task_3.png
├── docs/
│   ├── Task_4.pdf            ← required
│   └── Task_4.md             ← optional (source for the PDF)
├── PROJECT_GUIDE.md          ← optional (group design notes)
├── HOW_TO_SUBMIT.md          ← this file (optional, can leave out)
└── README.md                 ← short build/run instructions (write one if missing)
```

Replace `GroupX` with your actual group name/number.

Quick `README.md` template if you don't have one:

```markdown
# Pub/Sub Middleware — IS3208 Assignment 01

## Build
mkdir -p out && javac -d out src/*.java

## Run server
java -cp out server 5000

## Run client
java -cp out client 127.0.0.1 5000 PUBLISHER  TOPIC_A
java -cp out client 127.0.0.1 5000 SUBSCRIBER TOPIC_A

## Notes
- 4th arg (TOPIC) is optional. Defaults to GLOBAL.
- Type `terminate` in a client to disconnect.
```

Then zip:

```bash
cd ..
zip -r GroupX-Pubsub-IS3208.zip GroupX-Pubsub-IS3208/
```

**Do not include** `out/`, `.git/`, `.idea/`, `*.iml`, or `MA-Assignment-1-2026 (1).pdf` (the original brief) — strip those before zipping.

---

## 7. Final checklist (run before uploading)

- [ ] `javac -d out src/*.java` compiles cleanly on a fresh clone.
- [ ] All three demo videos play and show their tasks clearly.
- [ ] `docs/Task_4.pdf` exists and is readable.
- [ ] Zip opens to the folder structure in §6.
- [ ] No build artefacts (`out/`, `.idea/`, `.git/`) inside the zip.
- [ ] One person uploads to the VLE while the rest watch on a screen-share to confirm the upload succeeded.

---

## 8. If something is broken at the last minute

| Symptom | Most likely cause | Fix |
|---|---|---|
| Client hangs after `terminate` | Listener thread blocked on `readLine()` | It's a daemon thread, the JVM should exit anyway. If not, force-quit the terminal. |
| Subscriber never gets messages | Topics don't match (case-sensitive) | Re-check the 4th arg on both sides. `TOPIC_A` ≠ `topic_a`. |
| Server crashes when a client disconnects | A `send()` on a closed socket throws inside the publisher's thread | Known rough edge — restart the server and avoid Ctrl-C'ing a subscriber mid-broadcast during recording. |
| "Address already in use" | Previous server still running | `lsof -i :5000` (macOS/Linux) and kill the PID, or pick another port. |
| Video file too large to upload | High-resolution screen recording | Re-record at a lower resolution or compress with HandBrake / `ffmpeg -crf 28`. |
