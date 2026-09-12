# 2. Key Design Decisions

This section summarizes the decisions behind the design in `1-architecture-overview.md` (referenced by section number below), organized loosely around the open problems the brief calls out: execution triggering & state tracking, partial-failure handling, concurrent edits, no-double-execution, trading engine unavailability, notification, and insufficient cash.

### 2.1 Single deployable service with internal CQRS split

**Chosen:** One "Investment Plans Service" deployable, internally separated into command-side (Event Store + Outbox) and query-side (Projections + Read Model), mirroring how User/Cash/Orders are each a single domain.
**Rejected:** Separate read-service and write-service deployables.
**Why:** Fewer moving parts to operate for an MVP; the internal split still gives full CQRS benefits (independent read/write scaling *within* the service later, if needed) without the deployment/ops overhead of two services on day one.

### 2.2 Transactional Outbox + Debezium CDC for reliable publishing

**Chosen:** Every Kafka publish that must stay atomic with a local DB write goes through an Outbox table in the same transaction, tailed by Debezium (shared infra, not a service sub-component) — see 1.3, 1.6–1.7, 1.8.
**Rejected:** Publishing to Kafka directly from the command handler after the DB commit (dual-write); a distributed transaction/2PC across the relational database and Kafka.
**Why:** Avoids the dual-write problem without the complexity of 2PC. One exception: the Execution Dispatcher (2.4) publishes directly, since it does no DB write — outbox only matters where a write and a publish must stay atomic (see 1.5's "Why no Outbox here").

### 2.3 Stateless, plan-agnostic Scheduler

**Chosen:** A tiny standalone Scheduler that knows nothing about plans, publishing one daily tick; the Investment Plans Service resolves which plans are due (1.5).
**Rejected:** A scheduler that owns per-plan jobs (schedule/reschedule/deregister on every create/edit/cancel).
**Why:** Editing or canceling a plan never has to touch scheduling infrastructure — the next tick simply won't find it due. Keeps the scheduler itself trivial and replaceable.

### 2.4 Two-stage fan-out for execution triggering at scale

**Chosen:** Split "find who's due" (Dispatcher: paginated keyset scan, no writes) from "process each one" (Initiator: Kafka-partitioned consumer group) via an intermediate `execution-triggers` topic (1.5–1.6).
**Rejected:** A single consumer looping serially over every due plan; a sharded consumer group filtering the DB directly with no intermediate topic; spreading execution across time-of-day buckets instead of parallelizing.
**Why:** At tens of thousands of active plans (daily-in-aggregate due to per-user execution days), a serial loop over a single day's due set doesn't scale and has a bad failure mode (crash = unclear resume point). The two-stage split scales horizontally just by adding Initiator instances and isolates a crash to one partition instead of the whole day's run.

### 2.5 Idempotency key prevents double-execution

**Chosen:** `InitiateExecution` is keyed by `(plan_id, year-month-of(execution_day))`, enforced with a unique constraint in the event store (1.6, 1.9).
**Rejected:** Relying on exactly-once delivery from Kafka or from the Dispatcher's scan (neither is guaranteed — both are at-least-once).
**Why:** Makes every redelivery path safe by construction rather than by careful ordering: a redelivered scheduler tick, a re-published `ExecutionDue`, or consumer-group rebalance can all safely retry — duplicates collide on the constraint and become no-ops.

### 2.6 Executions snapshot the Plan's investments at initiation

**Chosen:** `InitiateExecution` reads the Plan's investments once and persists them into that Execution's own `OrderRequested` events — the Execution is thereafter immutable regardless of later Plan edits.
**Rejected:** Having an in-flight Execution read the Plan's investments live/repeatedly during processing.
**Why:** This is the answer to "concurrent plan edits during execution": a user editing a Plan while today's Execution is still in flight can't corrupt or race with it — the edit only affects *future* executions. No locking needed; it falls out of event sourcing for free.

### 2.7 Partial-failure handling: `Processed` tolerates partial rejection

**Chosen:** Execution status is a 2-state pipeline (`Initiated` → `Processed`); `Processed` means every Order reached a terminal state, not that every Order succeeded. Per-instrument success/failure is visible at the Order level (1.7, 1.10).
**Rejected:** A distinct Execution-level `Failed`/`PartiallyProcessed` status.
**Why:** A plan with 5 instruments where 1 gets rejected (e.g. insufficient cash for just that leg) isn't a failed execution — it's a partially successful one. Modeling failure only at the Order level avoids an extra status with fuzzy semantics ("failed" compared to what threshold?) and keeps the truth in one place.
**Assumption underlying this:** a partial outcome is treated as valid and terminal, with no remediation flow — flagged for confirmation in `5-assumptions-and-open-questions.md` 5.1 #4.

### 2.8 Downstream failures handled uniformly via async rejection, not pre-validated

**Chosen:** Neither Trading/Orders availability nor Cash sufficiency is checked before placing an order; both failure modes surface the same way — an `OrderRejected` event on `order-data`, processed by the same Order-Status Consumer (1.1, 1.7).
**Rejected:** Synchronously checking Cash balance or Trading/Orders health before issuing a command.
**Why:** Cash allocation is explicitly the Cash Domain's responsibility (out of scope per the brief) — duplicating that check here would be redundant business logic we'd have to keep in sync. For Trading/Orders unavailability specifically, Kafka's durability already buffers `order-commands` until Trading recovers — no special-casing needed beyond what 2.2–2.4 already provide.

### 2.9 Order identity via composite key, not a generated ID

**Chosen:** `(execution_id, asset_id)` is Order's identity and doubles as the correlation id sent in the order command (1.9).
**Rejected:** A separate generated `order_id`.
**Why:** An Execution has at most one Order per asset by construction, so the pair is already unique — one fewer generated field to manage.

### 2.10 Plan `Inactive`: dual trigger, user-edit-only recovery, reactive detection for MVP

**Chosen:** A Plan can be parked either by the system (detecting a blocking condition, e.g. an asset becomes untradeable) or by the user manually pausing it; the only way out is a user edit — no automatic recovery. Detection is reactive: discovered via a permanently-rejected Order during an actual execution attempt (1.7, 1.10).
**Rejected:** Automatic recovery once a blocker resolves; a proactive reconciliation process checking Active plans against live asset reference data before their next execution day.
**Why:** Automatic recovery would need a reconciliation loop we haven't designed and could silently resume executing a plan the user never re-confirmed. Reactive-only detection is simpler for MVP but has a real cost — one wasted execution attempt before the plan is parked — deferred with that trade-off stated explicitly (open item, see `5-assumptions-and-open-questions.md` 5.2 #7).

### 2.11 Execution outcome notification: publish enriched events, don't own delivery

**Chosen:** We don't build or own a notification mechanism. An external Notification Domain (1.1, 1.8) subscribes directly to our existing `investment-plan-data` topic; `ExecutionProcessed` and `PlanDeactivated` are enriched with compact summary context (fill/reject counts; triggering `asset_id`/`reason`) so it can compose a message without calling back into our API. The mobile app can also poll `GET /plans/{id}/executions[/{id}]` directly for pull-based visibility.
**Rejected:** Building/owning push notification delivery ourselves; publishing a separate curated topic just for notifications (rejected for now — direct subscription to `investment-plan-data` matches the existing "domains subscribe to what they need" convention with zero extra publishing work); keeping events thin and requiring a synchronous API callback for detail (would violate the brief's Kafka-only backend-to-backend convention).
**Why:** Channel/timing/delivery-guarantee logic (push vs. email, user preferences, retries) isn't this domain's concern, and duplicating it here would mean maintaining notification infrastructure alongside investment-plan logic. Publishing rich-enough events is a small, one-time cost that fully decouples us from however notification actually gets implemented — consistent with 2.8's general pattern of not special-casing downstream concerns we don't own.
