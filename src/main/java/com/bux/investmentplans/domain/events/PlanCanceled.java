package com.bux.investmentplans.domain.events;

import java.time.Instant;
import java.util.UUID;

/** Terminal — a canceled Plan is never reactivated (1.10). */
public record PlanCanceled(
        UUID eventId,
        String aggregateId,
        Instant occurredAt
) implements DomainEvent {
}
