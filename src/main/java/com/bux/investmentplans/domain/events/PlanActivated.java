package com.bux.investmentplans.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * First activation, or recovery from Inactive after a user edit resolves the
 * blocker (docs/detailed/1-architecture-overview.md 1.10) — same event either way.
 */
public record PlanActivated(
        UUID eventId,
        String aggregateId,
        Instant occurredAt
) implements DomainEvent {
}
