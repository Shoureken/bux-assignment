package com.bux.investmentplans.domain.events;

import com.bux.investmentplans.domain.Investment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Status-preserving edit (docs/detailed/1-architecture-overview.md 1.10) — recorded for audit only. */
public record PlanEdited(
        UUID eventId,
        String aggregateId,
        Instant occurredAt,
        String name,
        int recurrenceDay,
        List<Investment> investments
) implements DomainEvent {
}
