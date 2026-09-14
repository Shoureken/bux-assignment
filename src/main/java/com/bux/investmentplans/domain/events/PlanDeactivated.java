package com.bux.investmentplans.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * System-detected parking of a Plan after a permanently-rejected Order
 * (docs/detailed/1-architecture-overview.md 1.7, 2.10), or a user pause.
 * Carries enough context for the Notification Domain to compose a message
 * without calling back into our API (2.11).
 */
public record PlanDeactivated(
        UUID eventId,
        String aggregateId,
        Instant occurredAt,
        String assetId,
        String reason
) implements DomainEvent {
}
