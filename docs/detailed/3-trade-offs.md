# 3. Trade-offs

`2-key-design-decisions.md` covers individual choices; this section steps back and names the handful of overarching tensions those choices share, and why they were resolved the way they were for this system.

### 3.1 Availability & throughput over strong consistency

**Optimizing for:** every write path (plan create/edit, execution initiation, order status updates) is async end-to-end — Outbox + Debezium CDC decouples the DB write from the Kafka publish, and Kafka itself is at-least-once, not exactly-once (2.2, 2.5). No component blocks on another component being available.
**Sacrificing:** read-after-write consistency. A `GET /plans` right after `POST /plans` can briefly return stale data until the projection catches up (1.3); duplicate messages/re-processing are expected and normal, not exceptional, under crash-and-retry (1.5's full re-scan, 1.6's idempotency key).
**Why it's the right call:** at tens of thousands of active plans with daily-in-aggregate executions, blocking on cross-component consistency would mean the whole pipeline's throughput is capped by its slowest, least available part. Idempotency (2.5) makes "at-least-once + eventual consistency" safe rather than merely convenient.

### 3.2 Horizontal scalability & failure isolation over operational simplicity

**Optimizing for:** the Dispatch/Initiation split (2.4) and the Order-Status Consumer being fully independent of the daily batch (1.7) mean no single component's failure or slowness can stall the whole pipeline, and throughput scales by adding consumer instances.
**Sacrificing:** component count. A naive "one service, one loop" design would be far simpler to reason about, deploy, and debug than five Kafka topics, two consumer groups, and a separate Scheduler service. More moving parts means more failure modes, more monitoring surface, more onboarding cost for whoever operates this.
**Why it's the right call:** the brief's own scale context (executions happen daily in aggregate, not just monthly per plan) makes the naive version a real bottleneck, not a hypothetical one — this is the one place we chose to spend complexity budget deliberately rather than defer it.

### 3.3 Reuse & simplicity over optimal user experience (for failure detection)

**Optimizing for:** `PlanDeactivated`'s reactive trigger reuses the existing Order-Status Consumer flow (2.10) instead of building a new reconciliation component; no cash/asset-availability pre-check before execution (2.8) avoids duplicating Cash Domain's logic.
**Sacrificing:** a Plan can waste one full execution attempt — and the user gets no signal until *after* that attempt fails — before the system parks it. There's no early warning that a plan is about to fail before its execution day arrives.
**Why it's the right call:** proactive reconciliation is real, undesigned scope (flagged explicitly as deferred, 2.10) — building it now would mean guessing at requirements (how often to reconcile, against what reference data) we don't have yet. Shipping the reactive path first, with the gap named, beats blocking MVP on a bigger unknown.

### 3.4 Domain decoupling over short-term efficiency

**Optimizing for:** zero synchronous cross-domain calls anywhere in this design — order placement, cash sufficiency, and notification delivery are all discovered/handled via async Kafka events rather than direct queries to Cash, Trading, or a Notification service (2.8, 2.11).
**Sacrificing:** efficiency and latency in the unhappy path. An order that will obviously fail (e.g. known-insufficient cash) still goes through the full publish → Trading Domain → reject → consume round-trip instead of being caught early with one synchronous check. Every failure costs a full async round trip before the user (or the system) learns about it.
**Why it's the right call:** the brief mandates Kafka-only backend-to-backend communication and Cash allocation as explicitly out of scope — a synchronous shortcut here would mean quietly re-owning logic that belongs to another domain, which is a worse long-term bet than a slower failure path.

### 3.5 MVP-appropriate simplicity over completeness

**Optimizing for:** a single deployable service instead of split read/write services (2.1); full re-scan-on-crash instead of a dispatch checkpoint/cursor table (1.5); direct-produce instead of Outbox for the one component that has no DB write to protect (2.2's exception).
**Sacrificing:** some efficiency headroom and architectural purity. A crash-and-retry under the current design redoes more work than a checkpointed version would; a single deployable means read and write load scale together until it's explicitly split later.
**Why it's the right call:** each of these has a clear, named escape hatch (documented in `7-mvp-post-mvp.md` 7.3 as a post-MVP item) rather than being a dead end — the simplification was chosen because the *current* stated scale doesn't yet justify the extra complexity, not because the more scalable version is unknown or infeasible.
