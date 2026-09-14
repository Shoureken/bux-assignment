package com.bux.investmentplans.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Common envelope for every event appended to the Event Store / Outbox
 * (docs/detailed/1-architecture-overview.md 1.1). {@code aggregateId} is the
 * Plan or Execution id — the event-store partition key.
 */
public sealed interface DomainEvent
        permits PlanCreated, PlanEdited, PlanActivated, PlanDeactivated, PlanCanceled,
                ExecutionInitiated, OrderRequested, OrderStatusUpdated, ExecutionProcessed {

    UUID eventId();

    String aggregateId();

    Instant occurredAt();
}
