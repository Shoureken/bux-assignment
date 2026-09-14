package com.bux.investmentplans.domain.events;

import com.bux.investmentplans.domain.OrderStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * {@code aggregateId} is the owning Execution's id. {@code reason} is populated
 * only for {@link OrderStatus#REJECTED} and drives the permanent-vs-transient
 * branch in {@code OrderStatusCommandHandler} (1.7, 2.8, 2.10).
 */
public record OrderStatusUpdated(
        UUID eventId,
        String aggregateId,
        Instant occurredAt,
        String assetId,
        OrderStatus status,
        Optional<RejectionReason> reason
) implements DomainEvent {

    public record RejectionReason(String code, boolean permanent) {
    }
}
