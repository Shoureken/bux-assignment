package com.bux.investmentplans.orderstatus;

import com.bux.investmentplans.domain.OrderStatus;
import com.bux.investmentplans.domain.events.OrderStatusUpdated;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Reacts purely to {@code order-data} — independent of the Dispatcher,
 * Initiator, or the state of any daily batch (1.7). Fills/rejects arrive
 * asynchronously per order, on no particular schedule.
 */
@Component
@RequiredArgsConstructor
class OrderStatusConsumer {

    private final OrderStatusCommandHandler commandHandler;

    @KafkaListener(topics = "${investment-plans.topics.order-data}", groupId = "investment-plans.order-status-consumer")
    void onOrderResult(OrderResultMessage message) {
        String[] parts = message.correlationId().split(":", 2); // "executionId:assetId" (2.9)
        String executionId = parts[0];
        String assetId = parts[1];

        switch (message) {
            case OrderResultMessage.OrderFilled filled ->
                    commandHandler.updateOrderStatus(executionId, assetId, OrderStatus.FILLED, Optional.empty());
            case OrderResultMessage.OrderRejected rejected -> {
                var reason = new OrderStatusUpdated.RejectionReason(rejected.reasonCode(), rejected.permanent());
                commandHandler.updateOrderStatus(executionId, assetId, OrderStatus.REJECTED, Optional.of(reason));
            }
        }
    }
}
