package com.bux.investmentplans.domain;

import com.bux.investmentplans.domain.events.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Event-sourced aggregate: current state is a fold over this Plan's event
 * stream (docs/detailed/1-architecture-overview.md 1.9-1.10). Command
 * handlers load it via {@link #replay}, call a {@code decideX} method to get
 * the next event, apply it locally, then persist + append in one transaction.
 */
public final class Plan {

    private final String id;
    private PlanStatus status;
    private String name;
    private int recurrenceDay;
    private List<Investment> investments;

    private Plan(String id) {
        this.id = id;
    }

    public static Plan replay(String planId, List<DomainEvent> events) {
        Plan plan = new Plan(planId);
        events.forEach(plan::apply);
        return plan;
    }

    private void apply(DomainEvent event) {
        switch (event) {
            case PlanCreated e -> {
                this.status = PlanStatus.ENTERED;
                this.name = e.name();
                this.recurrenceDay = e.recurrenceDay();
                this.investments = e.investments();
            }
            case PlanEdited e -> {
                this.name = e.name();
                this.recurrenceDay = e.recurrenceDay();
                this.investments = e.investments();
                // status unchanged by construction (1.10)
            }
            case PlanActivated e -> this.status = PlanStatus.ACTIVE;
            case PlanDeactivated e -> this.status = PlanStatus.INACTIVE;
            case PlanCanceled e -> this.status = PlanStatus.CANCELED;
            default -> { /* Execution/Order events don't affect Plan state */ }
        }
    }

    /** Validates a create request and produces the event to append. No prior state exists yet. */
    public static PlanCreated decideCreate(String planId, String name, int recurrenceDay, List<Investment> investments) {
        requireValidRecurrenceDay(recurrenceDay);
        requireNonEmptyInvestments(investments);
        return new PlanCreated(UUID.randomUUID(), planId, Instant.now(), name, recurrenceDay, investments);
    }

    public PlanEdited decideEdit(String name, int recurrenceDay, List<Investment> investments) {
        if (status == PlanStatus.CANCELED) {
            throw new IllegalStateException("Cannot edit a canceled plan: " + id);
        }
        requireValidRecurrenceDay(recurrenceDay);
        requireNonEmptyInvestments(investments);
        return new PlanEdited(UUID.randomUUID(), id, Instant.now(), name, recurrenceDay, investments);
    }

    /** Covers both first activation and Inactive -> Active recovery (1.10) — same event. */
    public PlanActivated decideActivate() {
        if (status == PlanStatus.CANCELED) {
            throw new IllegalStateException("Cannot activate a canceled plan: " + id);
        }
        return new PlanActivated(UUID.randomUUID(), id, Instant.now());
    }

    /** System-triggered parking after a permanently-rejected Order (1.7, 2.10). */
    public PlanDeactivated decideDeactivate(String assetId, String reason) {
        return new PlanDeactivated(UUID.randomUUID(), id, Instant.now(), assetId, reason);
    }

    public PlanCanceled decideCancel() {
        return new PlanCanceled(UUID.randomUUID(), id, Instant.now());
    }

    public String id() {
        return id;
    }

    public PlanStatus status() {
        return status;
    }

    public int recurrenceDay() {
        return recurrenceDay;
    }

    /** Snapshot used by InitiateExecution (2.6) — callers must not mutate the returned list. */
    public List<Investment> investments() {
        return investments;
    }

    private static void requireValidRecurrenceDay(int day) {
        if (day < 1 || day > 28) { // avoids months without day 29-31
            throw new IllegalArgumentException("recurrenceDay must be 1-28: " + day);
        }
    }

    private static void requireNonEmptyInvestments(List<Investment> investments) {
        if (investments == null || investments.isEmpty()) {
            throw new IllegalArgumentException("A plan must contain at least one investment");
        }
    }
}
