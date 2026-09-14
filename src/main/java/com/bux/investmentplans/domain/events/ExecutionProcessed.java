package com.bux.investmentplans.domain.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Appended once every Order of this Execution reached a terminal state.
 * {@code rejectedCount > 0} does not make this a failure — partial success
 * is valid and terminal (2.7). Counts let the Notification Domain compose a
 * message without calling back into our API (2.11).
 */
public record ExecutionProcessed(
        UUID eventId,
        String aggregateId,
        Instant occurredAt,
        int filledCount,
        int rejectedCount
) implements DomainEvent {
}
