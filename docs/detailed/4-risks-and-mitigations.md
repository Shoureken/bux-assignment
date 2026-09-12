# 4. Risks & Mitigations

### 4.1 Execution Dispatcher poison-pill: a day's dispatch never completes

**How crash recovery normally works:** the `scheduler-tick` consumer offset is committed only after the entire day's scan-and-fan-out completes, never mid-scan (1.5). So a transient Dispatcher crash (e.g. after publishing `ExecutionDue` for 40 of 100 pages) leaves the tick offset uncommitted, and Kafka redelivers the same tick — either to a restarted instance or, with multiple replicas for HA, to another instance via consumer-group rebalance. Redelivery triggers a full re-scan from page 1, no separate checkpoint/cursor needed: duplicate `ExecutionDue` messages for plans already dispatched before the crash are absorbed downstream by `InitiateExecution`'s idempotency key, so redoing the whole scan is safe, just occasionally redundant. Messages already published to `execution-triggers` before the crash are unaffected — they're durable in Kafka regardless of the Dispatcher's fate.
**Risk:** this only covers *transient* crashes. If something about *today's specific data* deterministically crashes the Dispatcher, this same recovery loop repeats forever without ever finishing that day's dispatch (a poison pill) — no plans get executed that day, silently.
**Mitigation:** alert if today's tick isn't fully dispatched by a defined cutoff (e.g. "still undispatched past 06:00 UTC → page on-call"); this is a monitoring/runbook gap, not something the architecture alone can close.

### 4.2 Trading/Orders Domain extended outage: executions stuck silently

**Risk:** the brief explicitly calls out trading engine unavailability. Our design tolerates it structurally — `order-commands` durably queues in Kafka and Trading/Orders consumes the backlog whenever it recovers, no data loss — but there's no timeout or user-facing signal if that outage lasts hours or days. Executions just sit `Initiated` indefinitely with no indication anything is wrong.
**Mitigation:** alert on order-command age / consumer lag on the Trading/Orders side (their responsibility, but worth surfacing in our own monitoring since it directly affects our users); consider a "still processing" status message surfaced to the user past some threshold, rather than leaving `Initiated` to silently mean both "seconds old" and "days old."

### 4.3 Consumers other than InitiateExecution aren't explicitly idempotent

**Risk:** 2.5 designs an explicit idempotency key for `InitiateExecution`, but Kafka delivery is at-least-once everywhere, not just there. If `UpdateOrderStatus` (Order-Status Consumer, 1.7) isn't written to check the Order's *current* status before transitioning it, a redelivered `OrderFilled`/`OrderRejected` could double-apply — e.g. double-counting toward the `ExecutionProcessed` filled/rejected summary, or re-triggering the `PlanDeactivated` branch a second time.
**Mitigation:** every command handler that reacts to an at-least-once topic needs the same discipline: transition only from the expected prior status, treat a repeat of an already-applied transition as a no-op. This should be stated as a general invariant for the Command Handler, not just a one-off property of `InitiateExecution`.

### 4.4 No operational override for a legitimately-stuck idempotency key

**Risk:** the `(plan_id, year-month)` uniqueness constraint (2.5) that prevents double-execution also prevents a *legitimate* manual re-run — e.g. support needs to re-trigger a plan's execution after fixing a bug that caused it to be missed for that month. As designed, there's no path to do that.
**Mitigation:** needs an explicit operational escape hatch (admin tooling / support runbook) to reset or bypass the key for a specific plan+month, with appropriate audit logging. Currently undesigned — flagged as a gap for Product/Engineering rather than guessed at here.

### 4.5 Outbox backlog growth if Debezium falls behind or fails

**Risk:** the Outbox + Debezium CDC pattern (2.2) guarantees no event is lost, but if the shared Debezium infrastructure lags or goes down, outbox rows accumulate unpublished — Plan/Execution/Order state changes stop reaching `investment-plan-data`, silently stalling projections *and* the Notification Domain, for however long the outage lasts.
**Mitigation:** monitor outbox backlog size and oldest-unpublished-row age directly (a strong, simple signal independent of Debezium's own health metrics); since Debezium is shared infra we don't own, this needs coordination with whoever operates it on alerting thresholds and recovery SLAs.

### 4.6 Order-Status Consumer isn't designed for horizontal scale the way Execution Initiator is

**Risk:** 2.4 deliberately built the Dispatcher/Initiator split to scale past a serial-loop bottleneck, but the Order-Status Consumer (1.7) has no equivalent explicit parallelism story. If `order-data` volume grows, this consumer could become a throughput bottleneck with no designed mitigation.
**Mitigation:** running it as a consumer group naturally parallelizes across whatever partitions `order-data` already has — but that depends on partitioning we don't control (it's Trading/Orders' topic). Needs confirmation of `order-data`'s partition count/key with the Trading/Orders team; if it's not partitioned in a way that spreads our load, this needs to be raised as a cross-domain dependency, not something we can fix unilaterally.

### 4.7 Rejection reasons reaching the user unclearly (insufficient cash, etc.)

**Risk:** the brief calls out insufficient cash balance explicitly. Because we don't pre-check cash (2.8) by design, the only signal a user gets is an `OrderRejected` reason code passed through from Trading/Orders. If that code is generic or Trading/Orders doesn't distinguish "insufficient cash" from other rejection causes, the user (and the Notification Domain, 2.11) can't give a specific, actionable message.
**Mitigation:** confirm with the Trading/Orders team that `OrderRejected` carries a structured, specific reason code (not free text) that we can map to user-facing messaging — already flagged as an assumption in `5-assumptions-and-open-questions.md` 5.1 #2 for the permanent/transient distinction; the same gap applies here.

### 4.8 Event-schema coupling now that Notification Domain depends on our events

**Risk:** enriching `ExecutionProcessed`/`PlanDeactivated` for the Notification Domain's benefit (2.11) means our internal domain events now carry externally-consumed, presentation-shaped fields. A future internal refactor that seems harmless to us (renaming a field, changing a payload shape) could silently break a consumer we don't own or control.
**Mitigation:** treat `investment-plan-data`'s published event shapes as a versioned contract once external consumers exist — additive changes only, explicit deprecation window for anything breaking. Worth a lightweight schema registry or at least a documented compatibility policy before more consumers show up.
