# 5. Assumptions & Open Questions

## 5.1 Assumptions made in this design

| # | Assumption | If wrong |
|---|---|---|
| 1 | `order-commands` is a new topic, separate from `order-data` (1.2). | Collapses into a single bidirectional topic — a naming/wiring change, not a structural one. |
| 2 | `OrderRejected` carries a structured reason code distinguishing *permanent* (asset delisted/untradeable) from *transient* (momentary halt, insufficient liquidity) rejections (1.7, 1.10). | The reactive `PlanDeactivated` trigger (2.10) can't tell the two apart — either every rejection parks the plan (too aggressive) or none do (the open problem goes unsolved). |
| 3 | Investment.quantity is a monetary amount in the user's account currency (converted to shares/units at execution time), not a literal share count. | Order semantics, rounding, and partial-fill handling all change if it's actually a unit count — affects the entity model (1.9) directly. |
| 4 | A partially-executed Execution (some Orders Filled, some Rejected) is a valid, complete, terminal outcome — `Processed` — with no remediation required. The user simply sees which orders succeeded and which didn't; nothing retries automatically, nothing escalates (2.7, 1.10). | 2.7's decision not to have a separate Execution-level failure status would need revisiting, and a remediation flow (retry the failed leg next cycle? partial refund/adjustment? require the user to act?) would need designing — none of which exists today. |

## 5.2 Open questions for Product

| # | Question | Why it matters |
|---|---|---|
| 1 | How does a Plan actually move from `Entered` to `Active`? Is there a distinct activation step (funding confirmation, KYC check, explicit user confirmation) we haven't designed — or does creation activate it immediately, making `Entered` effectively vestigial the way `Edited`/`Planned` turned out to be (1.9, 1.10)? There's currently no activation endpoint in the API. | Determines whether `Entered` is a real, reachable state worth keeping in the model at all. |
| 2 | Confirms #3 above: does `Investment.quantity` mean a monetary amount or a unit/share count? | Same as 5.1 #3 — needs a Product-side answer since it's a business concept, not just an implementation detail. |
| 3 | Plan constraints: minimum/maximum investment amount per asset, maximum number of assets per plan, and what happens when `recurrence_day` doesn't exist in a given month (e.g. the 31st in a 30-day month) — run on the last day, skip that month, or shift to the 1st of the next? | Directly affects the Execution Dispatcher's due-plan query (1.5) and needs a concrete rule, not a default we guess at. |
| 4 | Any data retention/archival requirement for Executions and Orders once a Plan is `Canceled`? | Affects whether the read model needs an archival/purge story, or whether history must remain queryable indefinitely. |
| 5 | Does a Notification domain/service capable of consuming `investment-plan-data` already exist at BUX, or is that a separate initiative this design is assuming into existence (2.11)? | If it doesn't exist yet, "notify the user" isn't actually solved by this design — it's a dependency on unstarted work. |
| 6 | Is manually re-triggering a missed/failed monthly execution a supported support workflow? If so, who can do it and what's the audit requirement (4.4)? | The idempotency key that prevents double-execution (2.5) has no designed override — needs a decision before it becomes a real support ticket. |
| 7 | Is proactive detection of a blocked plan (reconciling Active plans against live asset reference data *before* their next execution day, rather than discovering it reactively via a wasted execution attempt) in scope for MVP, or explicitly deferred? | The reactive-only path (2.10) is a stated trade-off (3.3), not a permanent design decision — needs an explicit owner/call rather than staying an open gap indefinitely. |
| 8 | Is 99.9% API availability the right target, or does this surface need a stricter/looser SLO? | `6-non-functional-requirements.md` 6.3 proposes 99.9% as a reasoned default (convenience surface, not trading-critical) — stated as an assumption pending confirmation, not a negotiated target. |

## 5.3 Open questions for Engineering (mostly the Trading/Orders team)

| # | Question | Why it matters |
|---|---|---|
| 1 | Can order placement commands go on a new `order-commands` topic, or must they go through the existing `order-data` topic (5.1 #1)? | Changes the topic wiring in 1.2/1.8; needs to be settled before build, not discovered during integration. |
| 2 | Does `OrderRejected` already carry a structured reason code (5.1 #2), or would exposing one be new scope for the Trading/Orders team? | Blocks the reactive `PlanDeactivated` path and clear rejection messaging (2.10, 4.7) either way — worth knowing which. |
| 3 | What's `order-data`'s partition count/key? Can the Order-Status Consumer scale horizontally just by adding consumer-group instances, or is the topic effectively single-partition today (4.6)? | We don't own this topic's partitioning, so we can't unilaterally fix a throughput bottleneck there if it becomes one. |
| 4 | Is optimistic concurrency (e.g. a version field) expected on the Plan aggregate, to handle two concurrent `PUT /plans/{id}` edits racing each other — or is last-write-wins acceptable for MVP? | Not designed anywhere in this document; needs an explicit answer rather than an implicit default. |
