package com.bux.investmentplans.projection;

import com.bux.investmentplans.domain.events.*;
import com.bux.investmentplans.readmodel.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pure CQRS read side (docs/detailed/1-architecture-overview.md 1.3-1.4):
 * consumes our own domain events and upserts the tables GET /plans and
 * GET /plans/{id}/executions query directly. Never talks to the Event
 * Store or Outbox — those belong to the write side only.
 *
 * <p>Idempotent by construction: every upsert here is keyed by the
 * aggregate id, so redelivery of the same event (Kafka is at-least-once)
 * just re-applies the same write.
 */
@Component
@RequiredArgsConstructor
class PlanProjectionWorker {

    private final PlanReadRepository planReads;
    private final ExecutionReadRepository executionReads;
    private final OrderReadRepository orderReads;
    private final ProjectionMapper mapper;
    private final ProjectionCodec codec;

    @KafkaListener(topics = "${investment-plans.topics.domain-events}", groupId = "investment-plans.projection-worker")
    @Transactional
    void onDomainEvent(String rawPayload) {
        DomainEvent event = codec.decode(rawPayload);
        switch (event) {
            case PlanCreated e -> planReads.save(PlanProjection.builder()
                    .id(e.aggregateId())
                    .name(e.name())
                    .status("ENTERED")
                    .recurrenceDay(e.recurrenceDay())
                    .investments(mapper.toProjections(e.investments()))
                    .build());
            case PlanEdited e -> planReads.findById(e.aggregateId()).ifPresent(plan -> {
                plan.setName(e.name());
                plan.setRecurrenceDay(e.recurrenceDay());
                plan.setInvestments(mapper.toProjections(e.investments()));
                // status untouched — edits are status-preserving (1.10)
            });
            case PlanActivated e -> planReads.findById(e.aggregateId()).ifPresent(plan -> plan.setStatus("ACTIVE"));
            case PlanDeactivated e -> planReads.findById(e.aggregateId()).ifPresent(plan -> plan.setStatus("INACTIVE"));
            case PlanCanceled e -> planReads.findById(e.aggregateId()).ifPresent(plan -> plan.setStatus("CANCELED"));

            case ExecutionInitiated e -> executionReads.save(ExecutionProjection.builder()
                    .id(e.aggregateId())
                    .planId(e.planId())
                    .executionDay(e.executionDay())
                    .build());
            case OrderRequested e -> orderReads.save(OrderProjection.builder()
                    .id(new OrderProjectionId(e.aggregateId(), e.assetId()))
                    .quantity(e.quantity())
                    .status("INITIATED")
                    .build());
            case OrderStatusUpdated e -> orderReads.findById(new OrderProjectionId(e.aggregateId(), e.assetId()))
                    .ifPresent(order -> order.setStatus(e.status().name()));
            case ExecutionProcessed e -> executionReads.findById(e.aggregateId())
                    .ifPresent(execution -> execution.markProcessed(e.filledCount(), e.rejectedCount()));

            default -> { /* no-op */ }
        }
    }
}
