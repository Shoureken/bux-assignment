package com.bux.investmentplans.orderstatus;

import com.bux.investmentplans.api.PlanCommandHandler;
import com.bux.investmentplans.domain.Investment;
import com.bux.investmentplans.domain.OrderStatus;
import com.bux.investmentplans.domain.events.ExecutionInitiated;
import com.bux.investmentplans.domain.events.OrderRequested;
import com.bux.investmentplans.eventstore.EventStore;
import com.bux.investmentplans.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the reactive-deactivation branch from
 * docs/detailed/1-architecture-overview.md 1.7 and 2.10: a permanently
 * rejected Order both completes the Execution and parks the Plan, in one
 * transaction. Seeds the Execution/Order state directly via {@link
 * EventStore} rather than through the (out-of-scope-for-this-test)
 * Execution Initiator flow — see ExecutionTriggerFlowIT for that path.
 */
class OrderStatusConsumerIT extends AbstractIntegrationTest {

    @Autowired
    private PlanCommandHandler planCommandHandler;

    @Autowired
    private OrderStatusCommandHandler orderStatusCommandHandler;

    @Autowired
    private EventStore eventStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void permanentRejection_completesExecutionAndDeactivatesPlan() {
        String planId = planCommandHandler.createPlan(
                "Monthly ETF buy", 15, List.of(new Investment("DELISTED_CO", BigDecimal.ONE)));

        String executionId = UUID.randomUUID().toString();
        eventStore.append(executionId, List.of(
                new ExecutionInitiated(UUID.randomUUID(), executionId, Instant.now(), planId, LocalDate.of(2026, 9, 15)),
                new OrderRequested(UUID.randomUUID(), executionId, Instant.now(), "DELISTED_CO", BigDecimal.ONE)
        ));

        var permanentReason = new com.bux.investmentplans.domain.events.OrderStatusUpdated.RejectionReason("ASSET_DELISTED", true);
        orderStatusCommandHandler.updateOrderStatus(executionId, "DELISTED_CO", OrderStatus.REJECTED, Optional.of(permanentReason));

        Integer executionProcessed = jdbcTemplate.queryForObject(
                "select count(*) from event_store where aggregate_id = ? and event_type = 'ExecutionProcessed'",
                Integer.class, executionId);
        Integer planDeactivated = jdbcTemplate.queryForObject(
                "select count(*) from event_store where aggregate_id = ? and event_type = 'PlanDeactivated'",
                Integer.class, planId);

        assertThat(executionProcessed).isEqualTo(1);
        assertThat(planDeactivated).isEqualTo(1);
    }
}
