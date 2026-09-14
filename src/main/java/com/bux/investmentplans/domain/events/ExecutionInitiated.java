package com.bux.investmentplans.domain.events;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code aggregateId} is the new Execution's id. Always accompanied, in the
 * same command-handler transaction, by one {@link OrderRequested} per
 * Investment snapshotted from the Plan at this instant (2.6) — there is no
 * separately-observable "Planned" state (1.10).
 */
public record ExecutionInitiated(
        UUID eventId,
        String aggregateId,
        Instant occurredAt,
        String planId,
        LocalDate executionDay
) implements DomainEvent {
}
