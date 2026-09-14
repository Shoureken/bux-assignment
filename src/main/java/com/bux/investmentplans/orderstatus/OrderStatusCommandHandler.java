package com.bux.investmentplans.orderstatus;

import com.bux.investmentplans.domain.Execution;
import com.bux.investmentplans.domain.OrderStatus;
import com.bux.investmentplans.domain.Plan;
import com.bux.investmentplans.domain.events.OrderStatusUpdated;
import com.bux.investmentplans.eventstore.EventStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Handles {@code UpdateOrderStatus} (1.7, 2.7-2.8, 2.10). Two aggregate
 * streams can be appended to in one call — Execution always, Plan only on a
 * permanent rejection — but it's still one DB transaction, so either both
 * commit or neither does.
 */
@Service
@RequiredArgsConstructor
class OrderStatusCommandHandler {

    private final EventStore eventStore;

    @Transactional
    void updateOrderStatus(String executionId, String assetId, OrderStatus newStatus,
                            Optional<OrderStatusUpdated.RejectionReason> reason) {
        Execution execution = Execution.replay(executionId, eventStore.loadEvents(executionId));
        var executionEvents = execution.decideOrderStatusUpdate(assetId, newStatus, reason);
        eventStore.append(executionId, executionEvents);

        // A permanent rejection also parks the Plan so future Dispatch
        // scans stop picking it up (1.7, 1.10) — reactive detection, one
        // wasted attempt before the plan is parked (see open item 5.2 #7).
        boolean permanentRejection = newStatus == OrderStatus.REJECTED
                && reason.map(OrderStatusUpdated.RejectionReason::permanent).orElse(false);
        if (permanentRejection) {
            Plan plan = Plan.replay(execution.planId(), eventStore.loadEvents(execution.planId()));
            var planEvent = plan.decideDeactivate(assetId, reason.get().code());
            eventStore.append(execution.planId(), List.of(planEvent));
        }
    }
}
