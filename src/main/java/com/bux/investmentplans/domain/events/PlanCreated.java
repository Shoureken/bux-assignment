package com.bux.investmentplans.domain.events;

import com.bux.investmentplans.domain.Investment;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code aggregateId} is the new Plan's id. */
public record PlanCreated(
        UUID eventId,
        String aggregateId,
        Instant occurredAt,
        String name,
        int recurrenceDay,
        List<Investment> investments
) implements DomainEvent {
}
