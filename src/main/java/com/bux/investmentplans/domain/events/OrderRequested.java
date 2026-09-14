package com.bux.investmentplans.domain.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * {@code aggregateId} is the owning Execution's id. {@code (aggregateId, assetId)}
 * is this Order's identity and doubles as the correlation id sent in the order
 * command (2.9) — no separate generated order id.
 */
public record OrderRequested(
        UUID eventId,
        String aggregateId,
        Instant occurredAt,
        String assetId,
        BigDecimal quantity
) implements DomainEvent {
}
