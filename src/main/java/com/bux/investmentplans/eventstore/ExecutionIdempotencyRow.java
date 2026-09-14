package com.bux.investmentplans.eventstore;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.YearMonth;

/**
 * The unique constraint that makes {@code InitiateExecution} safe to retry
 * (docs/detailed/1-architecture-overview.md 1.6, 1.9, 2.5): one row per
 * {@code (planId, executionMonth)}. A duplicate insert — redelivered
 * scheduler tick, re-published {@code ExecutionDue}, consumer-group
 * rebalance — hits the constraint and the command handler treats that as a
 * no-op rather than an error.
 */
@Entity
@Table(name = "execution_idempotency", uniqueConstraints =
        @UniqueConstraint(columnNames = {"plan_id", "execution_month"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA
@Builder
public class ExecutionIdempotencyRow {

    @Id
    @GeneratedValue
    private java.util.UUID id;

    @Column(name = "plan_id", nullable = false)
    private String planId;

    @Column(name = "execution_month", nullable = false)
    private YearMonth executionMonth;

    @Column(name = "execution_id", nullable = false)
    private String executionId;
}
