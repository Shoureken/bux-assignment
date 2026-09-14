package com.bux.investmentplans.eventstore;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Written in the same transaction as the {@link EventRow}(s) it corresponds
 * to (docs/detailed/1-architecture-overview.md 1.1, 2.2). Debezium tails
 * this table's WAL and publishes each row to {@code topic}, keyed by {@code
 * messageKey} — it is shared infra, not code in this module. Holds both
 * domain-event rows (topic {@code investment-plan-data}) and order-command
 * rows (topic {@code order-commands}), since both need the same
 * write+publish atomicity guarantee.
 */
@Entity
@Table(name = "outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA
@Builder
public class OutboxRow {

    @Id
    @GeneratedValue
    private java.util.UUID id;

    @Column(nullable = false)
    private String topic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
