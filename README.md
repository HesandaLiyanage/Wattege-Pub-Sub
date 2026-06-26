# Pub/Sub Middleware — IS3208 Assignment 01

A lightweight **Publish/Subscribe (Pub/Sub) middleware** built in Java using TCP sockets.  
The system follows a classic broker pattern: a central **server** (broker) routes messages published on a **topic** to every **subscriber** currently registered on that topic.

---

## Table of Contents

1. [Project Description](#1-project-description)
2. [Repository Layout](#2-repository-layout)
3. [Build Instructions](#3-build-instructions)
4. [Running the Application](#4-running-the-application)
   - [Task 1 — Single client/server echo](#task-1--single-clientserver-echo)
   - [Task 2 — Multi-client broadcast (GLOBAL topic)](#task-2--multi-client-broadcast-global-topic)
   - [Task 3 — Topic-isolated routing](#task-3--topic-isolated-routing)
5. [Wire-Protocol Reference](#5-wire-protocol-reference)
6. [Admin Commands](#6-admin-commands)
7. [Group Members & Files](#7-group-members--files)

---

## 1. Project Description

This project implements a **multi-client Pub/Sub broker** in plain Java (no external libraries) for the IS3208 — Middleware Systems module.

Key characteristics:

| Feature | Detail |
|---|---|
| Transport | TCP sockets (line-based UTF-8, `\n` terminated) |
| Concurrency | One thread per client via a cached thread pool |
| Routing | Topic-based; messages reach only same-topic subscribers |
| Admin interface | In-band slash commands (`/list`, `/who`, `/stats`) |
| Build tool | Plain `javac`; no Maven/Gradle needed |

---

## 2. Repository Layout

```
Wattege-Pub-Sub/
├── src/
│   ├── server.java          # Task 1 — original single-client server (Ashen)
│   ├── client.java          # Task 1 — original single-client client (Ashen)
│   ├── Server.java          # Task 2/3 — multi-client broker entry point (Hesanda)
│   ├── ClientHandler.java   # Task 2/3 — per-client socket handler (Hesanda)
│   ├── Broker.java          # Task 2/3 — topic registry & message routing (Dana)
│   ├── Client.java          # Task 2/3 — interactive client application (Isum)
│   ├── Protocol.java        # Wire-protocol constants & helpers (Ushitha)
│   ├── Logger.java          # Timestamped, thread-safe logger (Ushitha)
│   ├── AdminCommands.java   # /list /who /stats handlers (Umaya)
│   └── TestRunner.java      # Programmatic load-test harness (Umaya)
├── screenshots/
│   └── Task_1.png
├── videos/
│   ├── Task_2.mp4
│   └── Task_3.mp4
├── docs/
│   └── Task_4.pdf
├── run-server.sh
├── run-client.sh
├── PROJECT_GUIDE.md
└── README.md
```

---

## 3. Build Instructions

No IDE or build tool is required — just a JDK (Java 11 or later).

```bash
# 1. Create the output directory (first time only)
mkdir -p out

# 2. Compile all source files in one shot
javac -d out src/*.java
```

If the compile succeeds you will see the `.class` files under `out/`.  
Any compile error will reference the exact file and line number.

> **Windows users**: replace `/` with `\` in paths if you are using Command Prompt.  
> PowerShell and Git Bash both handle forward slashes correctly.

---

## 4. Running the Application

> Make the shell scripts executable once (Linux / macOS / Git Bash on Windows):
> ```bash
> chmod +x run-server.sh run-client.sh
> ```

---

### Task 1 — Single client/server echo

The original Task 1 code lives in `src/server.java` and `src/client.java`.  
Run each in its own terminal:

```bash
# Terminal 1 — server (listens on port 5000)
java -cp out server 5000

# Terminal 2 — client
java -cp out client 127.0.0.1 5000
```

Type any text in Terminal 2 and press Enter; the server echoes it back.  
Type `terminate` to disconnect cleanly.

---

### Task 2 — Multi-client broadcast (GLOBAL topic)

All clients default to the `GLOBAL` topic, so every subscriber receives every publisher's messages.  
Open **six terminals** (or use tmux/tabs):

```bash
# Terminal 1 — broker
./run-server.sh 5000

# Terminal 2 — Publisher A  (defaults to TOPIC=GLOBAL)
./run-client.sh 127.0.0.1 5000 PUBLISHER

# Terminal 3 — Publisher B  (defaults to TOPIC=GLOBAL)
./run-client.sh 127.0.0.1 5000 PUBLISHER

# Terminal 4 — Subscriber 1
./run-client.sh 127.0.0.1 5000 SUBSCRIBER

# Terminal 5 — Subscriber 2
./run-client.sh 127.0.0.1 5000 SUBSCRIBER

# Terminal 6 — Subscriber 3
./run-client.sh 127.0.0.1 5000 SUBSCRIBER
```

In Terminal 2 type a message and press Enter — all three subscriber terminals display it.  
Repeat from Terminal 3 to see both publishers routing to all subscribers.

To disconnect any client gracefully:
```
terminate
```

---

### Task 3 — Topic-isolated routing

Two topics (`TOPIC_A` and `TOPIC_B`) run independently on the same broker.  
Open **six terminals**:

```bash
# Terminal 1 — broker
./run-server.sh 5000

# Terminal 2 — Publisher on TOPIC_A
./run-client.sh 127.0.0.1 5000 PUBLISHER TOPIC_A

# Terminal 3 — Publisher on TOPIC_B
./run-client.sh 127.0.0.1 5000 PUBLISHER TOPIC_B

# Terminal 4 — Subscriber on TOPIC_A  (receives only TOPIC_A messages)
./run-client.sh 127.0.0.1 5000 SUBSCRIBER TOPIC_A

# Terminal 5 — Subscriber on TOPIC_B  (receives only TOPIC_B messages)
./run-client.sh 127.0.0.1 5000 SUBSCRIBER TOPIC_B

# Terminal 6 — Second subscriber on TOPIC_A
./run-client.sh 127.0.0.1 5000 SUBSCRIBER TOPIC_A
```

Publish from Terminal 2 — only Terminals 4 and 6 display the message; Terminal 5 stays silent.  
Publish from Terminal 3 — only Terminal 5 displays the message.

#### Disconnect-resilience test

Kill Terminal 4 (Ctrl-C or `terminate`).  
Continue publishing from Terminal 2 — Terminal 6 keeps receiving without interruption and the server logs the disconnect cleanly.

---

## 5. Wire-Protocol Reference

All messages are **line-based UTF-8** with a single `\n` at the end.  
The very first line a client sends must be a `REGISTER` handshake.

### Message flow

```
CLIENT                                SERVER (broker)
  |                                       |
  |---  REGISTER PUBLISHER TOPIC_A  ----->|   Handshake
  |<--  ACK Welcome pub-1            -----|
  |                                       |
  |---  MSG hello world              ----->|   Publish
  |                                       |---> [pub-1@TOPIC_A] hello world ---> each TOPIC_A subscriber
  |                                       |
  |---  TERMINATE                    ----->|   Clean disconnect
  |                                       |   (server removes from broker, closes socket)
  |                                       |


SUBSCRIBER CLIENT                    SERVER (broker)
  |                                       |
  |---  REGISTER SUBSCRIBER TOPIC_A  --->|   Handshake
  |<--  ACK Welcome sub-2            ----|
  |                                       |
  |<--  [pub-1@TOPIC_A] hello world  ----|   Routed broadcast (arrives asynchronously)
  |                                       |
  |---  TERMINATE                    ---->|   Clean disconnect
```

### Message type reference

| Direction | Format | Notes |
|---|---|---|
| Client → Server (1st line) | `REGISTER <ROLE> <TOPIC>` | `ROLE` is `PUBLISHER` or `SUBSCRIBER`. Defaults to `GLOBAL` if `TOPIC` is omitted. |
| Publisher → Server | `MSG <payload>` | Payload may contain spaces; everything after `MSG ` is the body. |
| Any client → Server | `TERMINATE` | Signals a clean disconnect; server closes the socket. |
| Server → Subscriber | `[<pubId>@<TOPIC>] <payload>` | `pubId` is a short id like `pub-3` assigned by the server. |
| Server → Client (success) | `ACK <message>` | Used for handshake confirmation. |
| Server → Client (failure) | `ERR <message>` | Sent when the handshake is malformed or a command fails. |

### Protocol constants (`Protocol.java`)

| Constant | Value |
|---|---|
| `Protocol.REGISTER` | `"REGISTER"` |
| `Protocol.MSG` | `"MSG"` |
| `Protocol.TERMINATE` | `"TERMINATE"` |
| `Protocol.ACK` | `"ACK"` |
| `Protocol.ERR` | `"ERR"` |
| `Protocol.DEFAULT_TOPIC` | `"GLOBAL"` |

---

## 6. Admin Commands

Admin commands can be typed by **any connected client** (publisher or subscriber).  
They are sent as regular lines beginning with `/` and the server replies inline.

| Command | Description | Example Response |
|---|---|---|
| `/list` | Lists all active topics and their subscriber counts. | `topics: TOPIC_A=2, TOPIC_B=1` |
| `/who <topic>` | Shows the subscriber count for a specific topic. | `TOPIC_A has 2 subscriber(s)` |
| `/stats` | Shows the total number of messages routed since the server started. | `messages routed: 142` |

### Usage example

```bash
# In any connected client terminal, type:
/list
/who TOPIC_A
/stats
```

The server's response is printed immediately in the same terminal.

---

## 7. Group Members & Files

| # | Member | Assigned Files |
|---|---|---|
| 1 | Ashen | `src/server.java`, `src/client.java`, Task 1 screencast |
| 2 | Hesanda | `src/Server.java`, `src/ClientHandler.java` |
| 3 | Dana | `src/Broker.java` |
| 4 | Isum | `src/Client.java` |
| 5 | **Ushitha** | `src/Protocol.java`, `src/Logger.java`, `README.md`, `run-server.sh`, `run-client.sh`, Task 2 & 3 screencasts |
| 6 | Umaya | `src/AdminCommands.java`, `src/TestRunner.java`, `docs/Task_4.pdf` |

---

*IS3208 — Middleware Systems, Assignment 01.*
