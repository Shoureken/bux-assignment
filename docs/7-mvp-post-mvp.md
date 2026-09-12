# 7. MVP / Post-MVP

## 7.1 Delivery sequencing

**Phase 1 — Plan CRUD (no execution yet).** Entity model, Event Store + Outbox schema, Command Handlers for `PlanCreated`/`PlanEdited`, Projection Workers + Read Model, and the read/write Plan APIs: `POST /plans`, `PUT /plans/{id}`, `GET /plans`, `GET /plans/{id}`. This is fully self-contained (create → outbox → Debezium → `investment-plan-data` → projection → read) and testable end-to-end without the Scheduler, Trading/Orders, or any other domain involved. Ships first because nothing else in the system has anything to act on until plans exist.

**Phase 2 — Scheduled execution pipeline.** Scheduler, Execution Dispatcher, `execution-triggers`, Execution Initiator, `InitiateExecution` with its idempotency key (2.5), and `order-commands` publishing. `GET /plans/{id}/executions` and `GET /plans/{id}/executions/{id}` ship alongside, since there's nothing to view before this phase exists. This phase has a hard external dependency: it can't be integration-tested end-to-end until Trading/Orders confirms the `order-commands`-vs-`order-data` topic question and the `OrderRejected` reason-code shape (5.3 #1–2) — worth starting that conversation before Phase 2 development, not after.

**Phase 3 — Closing the loop.** Order-Status Consumer, `UpdateOrderStatus`, the `ExecutionProcessed`/`PlanDeactivated` reactive branch (2.10), and the observability signals from `6-non-functional-requirements.md` 6.4 (consumer lag, outbox backlog, dispatch-completion alerting). This is still MVP, not a nice-to-have follow-on: without it, executions never resolve past `Initiated`, and without the alerting specifically, a silent full-day dispatch failure (4.1) would be undetectable in production — not an acceptable gap for a feature that moves money on a schedule.

Idempotency (2.5) and the reactive `PlanDeactivated` path are treated as core correctness properties bundled into the phases above, not separable "features" that could be cut to shrink MVP further.

## 7.2 MVP API surface

| Endpoint | Phase | Notes |
|---|---|---|
| `POST /plans` | 1 | |
| `PUT /plans/{id}` | 1 | |
| `GET /plans` | 1 | |
| `GET /plans/{id}` | 1 | |
| `GET /plans/{id}/executions` | 2 | Empty until Phase 2 ships |
| `GET /plans/{id}/executions/{id}` | 2 | |

All six ship as part of MVP — the brief's full API list is small enough that there's no real case for holding any of it back to post-MVP; the sequencing above is about build order and testability, not about permanently excluding an endpoint.

## 7.3 Explicitly post-MVP (deferred, with rationale)

| Item | Why deferred |
|---|---|
| Proactive asset-availability reconciliation (5.2 #7) | Reactive detection (Phase 3) covers the same open problem at a fraction of the cost; proactive reconciliation is real, undesigned scope with its own open questions (frequency, reference data source). |
| Manual re-execution / idempotency-override tooling (4.4, 5.2 #6) | No confirmed support workflow needs it yet (open question). A direct, audited manual intervention by an engineer is an acceptable stopgap until it's a proven recurring need. |
| Read/write service split (2.1) | The internal CQRS separation already gives the seams to split later; doing it now is speculative scaling for a load level we haven't hit. |
| Dispatch checkpoint/cursor (vs. full re-scan on crash, 3.5) | Full re-scan is correct and cheap at current stated scale; only worth the added complexity if re-scan cost becomes measurably real. |
| Order-Status Consumer horizontal scale-out (4.6) | Blocked on confirming `order-data`'s partitioning with the Trading/Orders team (5.3 #3) anyway; premature to build against an unconfirmed constraint. |
| Optimistic concurrency on Plan edits (5.3 #4) | No confirmed requirement yet; last-write-wins is an acceptable MVP default pending a Product/Engineering answer. |
| Dedicated `plan-notifications` topic (2.11) | Direct subscription to `investment-plan-data` is strictly less work and was the recommended choice; revisit only if event-schema coupling with Notification Domain becomes a real maintenance problem. |

Actual push-notification *delivery* to the user depends on whether a Notification Domain capable of consuming our events already exists (5.2 #5) — that's a dependency on another team's system, not something this project's timeline can unilaterally guarantee. Our MVP scope is limited to publishing events rich enough for it to work (2.11), which is already covered in Phase 3.
