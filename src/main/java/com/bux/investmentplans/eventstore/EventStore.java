package com.bux.investmentplans.eventstore;

import com.bux.investmentplans.domain.events.DomainEvent;

import java.time.YearMonth;
import java.util.List;

/**
 * Append-only log of domain events, one aggregate (Plan or Execution) per
 * stream (docs/detailed/1-architecture-overview.md 1.1). {@link #append}
 * also writes the corresponding Outbox row(s) in the same call — callers
 * only need a surrounding {@code @Transactional} to get the atomicity
 * described in 2.2; they never touch the Outbox table directly.
 */
public interface EventStore {

    /**
     * Persists {@code events} to the aggregate's stream and writes one
     * Outbox row per event onto the {@code investment-plan-data} topic, in
     * whatever transaction is already open on the calling thread.
     */
    void append(String aggregateId, List<DomainEvent> events);

    /** Loads the full stream for an aggregate, in append order. */
    List<DomainEvent> loadEvents(String aggregateId);

    /**
     * Writes one Outbox row directly, for messages that aren't domain
     * events — e.g. the {@code OrderCommand}s the Execution Command Handler
     * sends to Trading/Orders on {@code order-commands} (1.6). Still just
     * an insert on the calling thread's open transaction, so it stays
     * atomic with whatever {@link #append} calls happen alongside it.
     */
    void appendOutboxMessage(String topic, String messageKey, Object payload);

    /**
     * Tries to claim the one-execution-per-plan-per-month slot (2.5).
     * Returns {@code true} if this call claimed it (caller should proceed
     * with {@code InitiateExecution}); {@code false} if it was already
     * claimed — a unique-constraint violation on {@code
     * (planId, executionMonth)}, caught here and turned into a plain no-op
     * rather than propagated as an error.
     */
    boolean claimExecutionSlot(String planId, YearMonth executionMonth, String executionId);
}
