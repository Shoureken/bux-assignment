package com.bux.investmentplans.eventstore;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One row per appended domain event — the actual source of truth
 * (docs/detailed/1-architecture-overview.md 1.1, "Event Store"). {@code
 * payload} is the event serialized as JSON; {@code sequence} is the
 * per-aggregate append order, used to reconstruct an aggregate via
 * {@code Plan.replay}/{@code Execution.replay}.
 */
@Entity
@Table(name = "event_store", uniqueConstraints = @UniqueConstraint(columnNames = {"aggregate_id", "sequence"}))
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA
@Builder
public class EventRow {

    @Id
    @GeneratedValue
    private java.util.UUID id;

    @Column(name = "aggregate_id", nullable = false)
    private String aggregateId;

    @Column(name = "sequence", nullable = false)
    private long sequence;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
