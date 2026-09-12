# 8. Delivery Plan

## 8.1 Development

Follows the three phases from `7-mvp-post-mvp.md` 7.1, with one adjustment to avoid a hard sequential block: Phase 2 (scheduled execution) has a real external dependency on Trading/Orders confirming the `order-commands` topic and `OrderRejected` reason-code shape (5.3 #1–2). Rather than waiting on that confirmation before starting, Phase 2 development begins against a stubbed Trading/Orders consumer/producer (a local fake that accepts commands and emits configurable fill/reject events), and swaps to the real integration once confirmed — this keeps Phase 2 moving in parallel with that cross-team conversation instead of blocking on it.

## 8.2 Testing

- **Unit tests** on the Plan/Execution/Order state machines (1.9–1.10) — every transition in the lifecycle diagrams should have a corresponding test, including the ones explicitly *not* allowed (e.g. `Canceled` never re-entering `Active`).
- **Integration tests** for the full Outbox → Debezium → Kafka → Projection round trip, and for the Dispatch → `execution-triggers` → Initiate fan-out (1.5–1.6) — these cross real infrastructure boundaries that unit tests can't cover.
- **Contract tests** against Trading/Orders' published event/command shapes (`order-commands` out, `order-data` in) — this is the message-driven equivalent of an API contract test, and matters more here than usual since 5.3 already flags several unconfirmed assumptions about that shape; a contract test turns "assumed" into "verified, and alerts on drift" once confirmed.
- **Load/scale tests** specifically targeting the Dispatch/Initiate fan-out (2.4) at the peak-day volume assumed in `6-non-functional-requirements.md` 6.1, not just the average — this is the one component explicitly built for scale, so its scaling claim should be verified before launch, not trusted from the design alone.
- **Failure-injection tests**: kill the Execution Dispatcher mid-scan and confirm the crash-recovery behavior described in 1.5/4.1 (delayed offset commit, full re-scan, no double-dispatch); redeliver duplicate `OrderFilled`/`OrderRejected` events and confirm `UpdateOrderStatus` is a no-op on an already-terminal Order (4.3's stated invariant). Both of these are strong claims made in the design — they should be demonstrated, not assumed correct by code review alone.
- **Idempotency tests**: duplicate `InitiateExecution` calls for the same `(plan_id, execution_month)` and confirm exactly one Execution results (2.5).

## 8.3 Rollout

This moves users' money on an unattended schedule, so rollout is staged rather than a single cutover:

1. **Internal/employee cohort first** — create and run real plans against the full pipeline (including live Trading/Orders integration) before any external user can.
2. **Gate by recurrence day, not by user percentage.** Since executions are inherently spread across recurrence days, initially only accept plan creation for a small number of near-term recurrence days (e.g. illustratively, the next 3 execution days). This means the team watches a small, bounded, day-by-day set of real executions closely — using the dashboards/alerts from `6-non-functional-requirements.md` 6.4 as the go/no-go signal — before opening up the remaining recurrence days, rather than committing to a broad user rollout with an unpredictable first-execution-day spike.
3. **General availability** once several consecutive execution days have run clean (dispatch completed within the alerting cutoff, no unexpected rejection-rate spike, no consumer-lag incidents).

**Rollback:** because every write path is durable (Outbox, Kafka) and every retry path is idempotent (2.5, 4.3), rollback doesn't mean "undo" — it means *stop starting new work*: disable plan creation via a feature flag, and/or pause the Scheduler so no new `DayTick` is published. Executions already in flight drain naturally through the existing resilient consumers; nothing needs to be manually reconciled or reversed.
