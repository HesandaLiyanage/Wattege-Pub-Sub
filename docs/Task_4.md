# Task 4 — Distributed Pub/Sub Architecture

**IS3208 Middleware Architectures — Assignment 01**

> Convert this file to PDF before submission (`docs/Task_4.pdf`). On macOS: open in any markdown viewer and "Print → Save as PDF", or paste into a Google Doc and export.

---

## 1. The problem with our current design

The implementation submitted for Tasks 1–3 uses **a single server process** as the broker. Every publisher and every subscriber connects to the same JVM. The server holds the entire list of connected clients in memory (`CopyOnWriteArrayList<ClientHandler>`) and routes every message itself.

This is a textbook **single point of failure (SPOF)**:

- If the server process crashes, the JVM dies, the host reboots, or the network link to it drops, **every** publisher and subscriber loses service at once.
- Subscription state is held only in the running JVM. There is no persistence. A restart loses every subscription; clients must reconnect from scratch.
- The server is also a single point of **scale** — every message in the system passes through one CPU.

For a real Pub/Sub middleware this is unacceptable. We want **availability** (clients can keep publishing and receiving even when individual nodes fail) and **reliability** (messages aren't lost during a node failure).

---

## 2. Proposed distributed architecture

We replace the single broker with a **broker cluster** of N nodes (typically 3 or 5 for quorum-based protocols).

```
                 ┌──────────────────────────────────────────────┐
                 │           Broker Cluster (N=3)               │
                 │                                              │
                 │   ┌────────┐    ┌────────┐    ┌────────┐     │
   Publishers ──►│   │Broker 1│◄──►│Broker 2│◄──►│Broker 3│     │──► Subscribers
                 │   │ leader │    │follower│    │follower│     │
                 │   └────────┘    └────────┘    └────────┘     │
                 │        ▲             ▲             ▲         │
                 │        │             │             │         │
                 │        └─── Raft consensus ────────┘         │
                 │       (subscription state + log)             │
                 └──────────────────────────────────────────────┘
                                       ▲
                                       │
                          ┌────────────┴────────────┐
                          │ Cluster Bootstrap List  │
                          │  broker1:5000           │
                          │  broker2:5000           │
                          │  broker3:5000           │
                          └─────────────────────────┘
```

### 2.1 Broker cluster

- N broker nodes, each running the same Pub/Sub server code we already wrote, but with an extra **replication module**.
- Every broker holds the **complete subscription table** (which subscriber is registered on which topic). The table is kept consistent across nodes by a consensus protocol — we'll use **Raft** because it is well-documented and there are mature Java libraries (e.g. Apache Ratis).
- Writes to the table (a client connects / disconnects / changes topic) go through the Raft leader, which appends to a replicated log; followers apply it locally. Once a majority (e.g. 2 of 3) has the entry, the operation is considered committed.

### 2.2 Topic partitioning + replication of messages

To scale beyond one CPU, **topics are partitioned across brokers**. Each topic has:

- A **primary broker** that owns all live publisher connections for that topic.
- N − 1 **replica brokers** that hold the same subscriber list and receive each message via the replication log.

When a publisher sends a message:
1. The primary broker for that topic receives the message.
2. It writes the message to the Raft log (committed when a majority acks).
3. After commit, every replica (including the primary) forwards the message to its locally connected subscribers of that topic.

Result: a message survives the loss of any minority of brokers.

### 2.3 Failover

- Brokers run a **heartbeat** between each other (part of Raft).
- If the primary for a topic stops heartbeating, a new primary is elected automatically using Raft leader election.
- Subscribers connected to the dead broker get a TCP disconnect and **reconnect** to another broker from the bootstrap list. Because subscription state was replicated, the new broker already knows them.

### 2.4 Client discovery

- Clients are configured with a **bootstrap list** of broker addresses (`broker1:5000, broker2:5000, broker3:5000`), not a single IP.
- On startup, the client connects to whichever broker answers first.
- Brokers respond to a new `WHO_IS_PRIMARY <topic>` query so the client can be redirected to the right primary for publishing. Subscribers can connect to any broker that holds a replica for their topic.

### 2.5 Trade-offs (CAP)

The CAP theorem says we can have at most two of: Consistency, Availability, Partition tolerance. Networks fail, so P is mandatory. Our design favours **CP** during partitions:

- During a network split that isolates a minority of brokers, those minority brokers refuse writes (they cannot reach quorum). They remain available for **reads** (subscribers still receive messages already replicated to them) but the system stops accepting new publishes on partitions that lost the majority.
- An alternative would be AP — accept writes everywhere and reconcile later (like a gossip-based design or Cassandra-style eventual consistency). We don't choose that here because pub/sub semantics expect each subscriber to see a single ordered stream per topic, which is much harder under eventual consistency.

Latency trade-off: every publish now waits for a majority ack instead of being a single in-memory list iteration. For a 3-node cluster on the same LAN this adds ~1 ms. Acceptable for the typical chat / event-stream workload.

---

## 3. How this improves availability and reliability

| Requirement | Single-server design (current) | Distributed design (proposed) |
|---|---|---|
| **Availability** — clients keep working during failures | Server crash → 100 % outage | Single broker crash → only the clients on that broker disconnect and reconnect to another node. Quorum (e.g. 2 of 3 brokers) remains available. |
| **Reliability** — messages not lost during failures | A message in-flight when the server dies is lost. Subscription state is lost on restart. | Each message is replicated to a majority before being delivered, so it survives the loss of any minority. Subscription state lives in the Raft log on disk, so it survives a full cluster restart. |
| **Scalability** (bonus) | Single CPU, single network card | Topics partitioned across brokers — more brokers = more throughput. |

In short: the design replaces a single fragile broker with a small replicated group that **stays available through node failures** (availability) and **does not drop messages or forget subscriptions** when those failures happen (reliability).

---

## 4. What we did NOT change

To stay focused, the proposal keeps the *client-facing protocol* identical to what we built in Tasks 1–3:

- `REGISTER|<ROLE>|<TOPIC>` handshake.
- Plain-text messages from publishers.
- `terminate` to disconnect.

The only client change is the bootstrap list (multiple IP:port pairs instead of one) and automatic reconnect on disconnect. Existing client code would need only a small wrapper around the connection logic.

---

## 5. Summary

Replacing the single broker with a 3-node Raft-replicated broker cluster, partitioning topics across nodes, and giving clients a bootstrap list with automatic failover gives us **continuous availability under any minority failure** and **zero message loss** for committed publishes, while keeping the client protocol unchanged. The cost is a small per-message latency penalty for replication consensus, which is acceptable for typical pub/sub workloads.
