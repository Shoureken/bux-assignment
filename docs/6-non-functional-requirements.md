# 6. Non-Functional Requirements

## 6.1 Scale assumptions (grounding numbers)

The brief gives tens of thousands of users with active plans, with executions monthly per plan but daily in aggregate (roughly uniform across ~28-31 possible recurrence days). **Assumption:** a "typical" day sees `active_plans / ~28` due plans, but common recurrence days (1st, 15th, month-end) likely see a multi-x spike over that average — the Dispatch/Initiate fan-out (2.4) is sized for peak-day volume, not the average, specifically so an assumption error here degrades to "slower," not "broken." API read/write traffic (plan CRUD, status polling) is assumed low-to-moderate and not the scaling bottleneck relative to the execution pipeline.

## 6.2 Scalability

- **Execution triggering** scales horizontally by construction: the Dispatcher does a bounded paginated scan (no full-table load regardless of due-plan count), and the Execution Initiator consumer group scales by adding instances up to `execution-triggers`' partition count (2.4).
- **Read path** scales independently of the write path via CQRS — Projection Workers and the Read Model can be scaled without touching the command/event-store side.
- **Assumption:** Kafka and the relational database's capacity/provisioning are owned by platform teams and scaled to whatever throughput this service needs — this document owns application-level partitioning and consumer-group design, not cluster sizing.

## 6.3 Reliability

- **Durability:** zero event loss by construction — Outbox + Debezium CDC (2.2) means a committed write always eventually reaches Kafka, independent of Kafka's availability at write time.
- **Processing guarantee:** at-least-once everywhere, not exactly-once — made safe by idempotency (2.5) and a status-check-before-transition discipline required of every command handler (4.3), not by avoiding duplicates outright (3.1).
- **Availability target (assumption, needs confirmation — `5-assumptions-and-open-questions.md` 5.2 #8):** propose 99.9% for the HTTP API. This is a planning/convenience surface, not a trading-critical path — a brief API outage doesn't lose money or cause a missed execution, since execution triggering (1.5–1.6) is fully decoupled from API availability.
- **RPO:** effectively zero for committed writes; the relational database's own backup/replication strategy (platform-owned, out of scope) bounds the true worst case.
- **RTO:** the execution pipeline self-heals from transient failures without manual intervention (crash-safe dispatch, redelivery, idempotency). The one case requiring manual intervention is the Dispatcher poison-pill scenario (4.1), bounded by whatever alerting cutoff is set — **assumption:** within the same execution day.

## 6.4 Observability

Consolidating what `4-risks-and-mitigations.md` already calls for into formal requirements, plus tracing:

- **Consumer lag** on every Kafka consumer (Projection Workers, Order-Status Consumer, Execution Dispatcher, Execution Initiator).
- **Outbox backlog** size and oldest-unpublished-row age (4.5).
- **Dispatch-completion alerting** — today's tick fully dispatched by a defined cutoff (4.1).
- **Order-command age / round-trip latency** to Trading/Orders, to catch an extended downstream outage early rather than discovering it via user complaints (4.2).
- **Correlation ids threaded end-to-end** — `plan_id`, `execution_id`, `(execution_id, asset_id)` for orders — through every event, command, and log line, so one execution's full lifecycle is traceable across every component without log-correlation guesswork.
- **Dashboards:** daily dispatched-vs-processed count (surfaces silent partial failures), and an Execution status funnel (Initiated → Processed, with rejection rate broken out by reason code) to make partial-failure rate (2.7) visible over time, not just per-incident.

## 6.5 Security (brief note)

Not designed in depth here — this service is assumed to reuse the App Backend's existing authentication/authorization for the Mobile App → HTTP boundary (out of scope per the brief's given system context) rather than introduce a new auth surface. No new NFR beyond "don't weaken what already exists" is claimed.
