package com.bux.investmentplans.domain;

import com.bux.investmentplans.domain.events.*;

import java.time.Instant;
import java.util.*;

/**
 * Event-sourced aggregate for one Execution and its Orders
 * (docs/detailed/1-architecture-overview.md 1.9-1.10). Loaded by the
 * Order-Status Consumer's command handler to decide whether an incoming
 * fill/reject completes the Execution.
 */
public final class Execution {

    private final String id;
    private String planId;
    private ExecutionStatus status;
    private final Map<String, OrderStatus> ordersByAssetId = new LinkedHashMap<>();

    private Execution(String id) {
        this.id = id;
    }

    public static Execution replay(String executionId, List<DomainEvent> events) {
        Execution execution = new Execution(executionId);
        events.forEach(execution::apply);
        return execution;
    }

    /**
     * Starts a new Execution: one {@link ExecutionInitiated} plus one
     * {@link OrderRequested} per snapshotted Investment (2.6), created and
     * published together so there's no separately-observable "Planned"
     * state (1.10). Investments are passed in already snapshotted by the
     * caller from the Plan aggregate — this method does not read the Plan.
     */
    public static List<DomainEvent> decideInitiate(String executionId, String planId,
                                                      java.time.LocalDate executionDay,
                                                      List<Investment> investments) {
        var initiated = new ExecutionInitiated(UUID.randomUUID(), executionId, Instant.now(), planId, executionDay);
        List<DomainEvent> events = new ArrayList<>();
        events.add(initiated);
        for (Investment investment : investments) {
            events.add(new OrderRequested(UUID.randomUUID(), executionId, Instant.now(),
                    investment.assetId(), investment.quantity()));
        }
        return events;
    }

    private void apply(DomainEvent event) {
        switch (event) {
            case ExecutionInitiated e -> {
                this.planId = e.planId();
                this.status = ExecutionStatus.INITIATED;
            }
            case OrderRequested e -> ordersByAssetId.put(e.assetId(), OrderStatus.INITIATED);
            case OrderStatusUpdated e -> ordersByAssetId.put(e.assetId(), e.status());
            case ExecutionProcessed e -> this.status = ExecutionStatus.PROCESSED;
            default -> { /* Plan events don't affect Execution state */ }
        }
    }

    /**
     * Applies one order's terminal outcome and, if every Order has now
     * reached a terminal state, also produces {@link ExecutionProcessed}
     * (1.7). Returns just the {@link OrderStatusUpdated} otherwise.
     */
    public List<DomainEvent> decideOrderStatusUpdate(String assetId, OrderStatus newStatus,
                                                       Optional<OrderStatusUpdated.RejectionReason> reason) {
        if (!ordersByAssetId.containsKey(assetId)) {
            throw new IllegalStateException("No Order for asset " + assetId + " on execution " + id);
        }
        var updated = new OrderStatusUpdated(UUID.randomUUID(), id, Instant.now(), assetId, newStatus, reason);

        // Project the pending update onto a local copy to decide completion
        // without mutating this instance — apply() runs only once the
        // command handler actually persists the event.
        Map<String, OrderStatus> projected = new LinkedHashMap<>(ordersByAssetId);
        projected.put(assetId, newStatus);
        boolean allTerminal = projected.values().stream()
                .allMatch(s -> s == OrderStatus.FILLED || s == OrderStatus.REJECTED);

        if (!allTerminal) {
            return List.of(updated);
        }
        long filled = projected.values().stream().filter(s -> s == OrderStatus.FILLED).count();
        long rejected = projected.values().stream().filter(s -> s == OrderStatus.REJECTED).count();
        var processed = new ExecutionProcessed(UUID.randomUUID(), id, Instant.now(), (int) filled, (int) rejected);
        return List.of(updated, processed);
    }

    public String id() {
        return id;
    }

    public String planId() {
        return planId;
    }

    public ExecutionStatus status() {
        return status;
    }
}
